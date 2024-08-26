import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import org.neo4j.graphdb.*
import org.neo4j.graphdb.traversal.Evaluators
import org.neo4j.graphdb.traversal.Uniqueness

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME

enum AthleteRelationships implements RelationshipType { ran, supersedes }
import static AthleteRelationships.*

var db = '/tmp/athletesDB' as File
var managementService = new DatabaseManagementServiceBuilder(db.toPath()).build()
var graphDb = managementService.database(DEFAULT_DATABASE_NAME)

static insertAthlete(Transaction tx, first, last, dob) {
    def a = tx.createNode()
    a.setProperty('first', first)
    a.setProperty('last', last)
    a.setProperty('dob', dob)
    a
}

static insertRun(Transaction tx, h, m, s, where, when, athlete) {
    def r = tx.createNode()
    r.setProperty('distance', 42195)
    r.setProperty('when', when)
    r.setProperty('where', where)
    r.setProperty('time', h * 60 * 60 + m * 60 + s)
    r.createRelationshipTo(athlete, ran)
    r
}

var athlete1, athlete2, athlete3, athlete4
var marathon1, marathon2a, marathon2b, marathon3, marathon4a, marathon4b

def run() {
    addShutdownHook {
        managementService.shutdown()
    }

    Node.metaClass {
        propertyMissing { String name, val -> delegate.setProperty(name, val) }
        propertyMissing { String name -> delegate.getProperty(name) }
        methodMissing { String name, args ->
            delegate.createRelationshipTo(args[0], AthleteRelationships."$name")
        }
    }

    try (Transaction tx = graphDb.beginTx()) {

        athlete1 = tx.createNode()
        athlete1.setProperty('first', 'Paul')
        athlete1.setProperty('last', 'Tergat')
        athlete1.setProperty('dob', '1969-06-17')

        marathon1 = tx.createNode()
        marathon1.setProperty('distance', 42195)
        marathon1.setProperty('when', '2003-09-28')
        marathon1.setProperty('where', 'Berlin')
        marathon1.setProperty('time', 2 * 60 * 60 + 4 * 60 + 55)
        marathon1.createRelationshipTo(athlete1, ran)

        def first = athlete1.getProperty('first')
        def last = athlete1.getProperty('last')
        def where = marathon1.getProperty('where')
        def when = marathon1.getProperty('when')
        println "$first $last won the $where marathon on $when"


/*
        Relationship.metaClass {
            propertyMissing { String name, val -> delegate.setProperty(name, val) }
            propertyMissing { String name -> delegate.getProperty(name) }
        }
*/

        athlete2 = tx.createNode()
        athlete2.first = 'Khalid'
        athlete2.last = 'Khannouchi'
        athlete2.dob = '1971-12-22'

        marathon2a = tx.createNode()
        marathon2a.distance = 42195
        marathon2a.when = '2002-04-14'
        marathon2a.where = 'London'
        marathon2a.time = 2 * 60 * 60 + 5 * 60 + 38
        athlete2.ran(marathon2a)

        marathon2b = insertRun(tx, 2, 5, 42, 'Chicago', '1999-10-24', athlete2)

        athlete3 = insertAthlete(tx, 'Ronaldo', 'da Costa', '1970-06-07')
        marathon3 = insertRun(tx, 2, 6, 5, 'Berlin', '1998-09-20', athlete3)

        athlete4 = insertAthlete(tx, 'Paula', 'Radcliffe', '1973-12-17')
        marathon4a = insertRun(tx, 2, 17, 18, 'Chicago', '2002-10-13', athlete4)
        marathon4b = insertRun(tx, 2, 15, 25, 'London', '2003-04-13', athlete4)

        def allAthletes = [athlete1, athlete2, athlete3, athlete4]
        def wonInLondon = allAthletes.findAll { athlete ->
            athlete.getRelationships(ran).any { run ->
                run.getOtherNode(athlete).where == 'London'
            }
        }
        assert wonInLondon*.last == ['Khannouchi', 'Radcliffe']

        marathon2b.supersedes(marathon3)
        marathon2a.supersedes(marathon2b)
        marathon1.supersedes(marathon2a)
        marathon4b.supersedes(marathon4a)

        def info = { m -> "$m.where $m.when" }
        println "World records following ${info(marathon3)}:"

        for (Path p in tx.traversalDescription()
            .breadthFirst()
            .relationships(supersedes)
            .evaluator(Evaluators.fromDepth(1))
            .uniqueness(Uniqueness.NONE)
            .traverse(marathon3)) {
            println p.endNode().with(info)
        }

    }
}
