import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import org.neo4j.graphdb.*
import org.neo4j.graphdb.traversal.Evaluators
import org.neo4j.graphdb.traversal.Uniqueness

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME

enum SwimmingRelationships implements RelationshipType { swam, supercedes }
import static SwimmingRelationships.*

var db = '/tmp/athletesDB' as File
var managementService = new DatabaseManagementServiceBuilder(db.toPath()).build()
var graphDb = managementService.database(DEFAULT_DATABASE_NAME)

static insertSwimmer(Transaction tx, name, country) {
    def sr = tx.createNode()
    sr.setProperty('name', name)
    sr.setProperty('country', country)
    sr
}

static insertSwim(Transaction tx, where, event, time, result, swimmer) {
    def sm = tx.createNode()
    sm.setProperty('result', result)
    sm.setProperty('event', event)
    sm.setProperty('where', where)
    sm.setProperty('time', time)
    sm.createRelationshipTo(swimmer, swam)
    sm
}

var es, kmk, km, rs, kb
var swim1, swim2, swim3, swim4, swim5, swim6, swim7, swim8, swim9, swim10, swim11, swim12

def run() {
    addShutdownHook {
        managementService.shutdown()
    }

    Node.metaClass {
        propertyMissing { String name, val -> delegate.setProperty(name, val) }
        propertyMissing { String name -> delegate.getProperty(name) }
        methodMissing { String name, args ->
            delegate.createRelationshipTo(args[0], SwimmingRelationships."$name")
        }
    }

    try (Transaction tx = graphDb.beginTx()) {

        es = tx.createNode()
        es.setProperty('name', 'Emily Seebohm')
        es.setProperty('country', '🇦🇺')

        swim1 = tx.createNode()
        swim1.setProperty('event', 'Heat 4')
        swim1.setProperty('where', 'London 2012')
        swim1.setProperty('result', 'First')
        swim1.setProperty('time', 58.23d)
        swim1.createRelationshipTo(es, swam)

        def name = es.getProperty('name')
        def country = es.getProperty('country')
        def where = swim1.getProperty('where')
        def event = swim1.getProperty('event')
        def time = swim1.getProperty('time')
        println "$name from $country swam a time of $time in $event at the $where Olympics"

        km = tx.createNode()
        km.name = 'Kylie Masse'
        km.country = '🇨🇦'

        swim2 = tx.createNode()
        swim2.time = 58.17d
        swim2.result = 'First'
        swim2.event = 'Heat 4'
        swim2.where = 'Tokyo 2021'
        km.swam(swim2)
        swim2.supercedes(swim1)

        swim3 = tx.createNode()
        swim3.time = 57.72d
        swim3.result = '🥈'
        swim3.event = 'Final'
        swim3.where = 'Tokyo 2021'
        km.swam(swim3)

        rs = insertSwimmer(tx, 'Regan Smith', '🇺🇸')
        swim4 = insertSwim(tx, 'Tokyo 2021', 'Heat 5', 57.96d, 'First', rs)
        swim4.supercedes(swim2)
        swim5 = insertSwim(tx, 'Tokyo 2021', 'Semifinal 1', 57.86d, '', rs)
        swim6 = insertSwim(tx, 'Tokyo 2021', 'Final', 58.05d, '🥉', rs)
        swim7 = insertSwim(tx, 'Paris 2024', 'Final', 57.66d, '🥈', rs)
        swim8 = insertSwim(tx, 'Paris 2024', 'Relay leg1', 57.28d, 'First', rs)

        kmk = insertSwimmer(tx, 'Kaylie McKeown', '🇦🇺')
        swim9 = insertSwim(tx, 'Tokyo 2021', 'Heat 6', 57.88d, 'First', kmk)
        swim9.supercedes(swim4)
        swim5.supercedes(swim9)
        swim10 = insertSwim(tx, 'Tokyo 2021', 'Final', 57.47d, '🥇', kmk)
        swim10.supercedes(swim5)
        swim11 = insertSwim(tx, 'Paris 2024', 'Final', 57.33d, '🥇', kmk)
        swim11.supercedes(swim10)
        swim8.supercedes(swim11)

        kb = insertSwimmer(tx, 'Katharine Berkoff', '🇺🇸')
        swim12 = insertSwim(tx, 'Paris 2024', 'Final', 57.98d, '🥉', kb)

        def swimmers = [es, km, rs, kmk, kb]
        def successInParis = swimmers.findAll { swimmer ->
            swimmer.getRelationships(swam).any { run ->
                run.getOtherNode(swimmer).where == 'Paris 2024'
            }
        }
        assert successInParis*.country.unique() == ['🇺🇸', '🇦🇺']

        def swims = [swim1, swim2, swim3, swim4, swim5, swim6, swim7, swim8, swim9, swim10, swim11, swim12]
        def recordSetInHeat = swims.findAll {  swim ->
            swim.event.startsWith('Heat')
        }*.where
        assert recordSetInHeat.unique() == ['London 2012', 'Tokyo 2021']

        def recordTimesInFinals = swims.findAll {  swim ->
            swim.event == 'Final' && swim.hasRelationship(supercedes)
        }*.time
        assert recordTimesInFinals == [57.47d, 57.33d]

        def info = { s -> "$s.where $s.event" }
        println "Olympic records following ${info(swim1)}:"

        for (Path p in tx.traversalDescription()
            .breadthFirst()
            .relationships(supercedes)
            .evaluator(Evaluators.fromDepth(1))
            .uniqueness(Uniqueness.NONE)
            .traverse(swim1)) {
            println p.endNode().with(info)
        }
    }
}
