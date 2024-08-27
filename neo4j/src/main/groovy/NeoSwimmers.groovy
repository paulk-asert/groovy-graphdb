import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import org.neo4j.graphdb.*
import org.neo4j.graphdb.traversal.Evaluators
import org.neo4j.graphdb.traversal.Uniqueness

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME

enum SwimmingRelationships implements RelationshipType {
    swam, supersedes, runnerup
}

import static SwimmingRelationships.*
import static org.neo4j.graphdb.Label.label

var db = '/tmp/swimmersDB' as File
var managementService = new DatabaseManagementServiceBuilder(db.toPath()).build()
var graphDb = managementService.database(DEFAULT_DATABASE_NAME)

static insertSwimmer(Transaction tx, name, country) {
    var sr = tx.createNode(label('Swimmer'))
    sr.setProperty('name', name)
    sr.setProperty('country', country)
    sr
}

static insertSwim(Transaction tx, at, event, time, result, swimmer) {
    var sm = tx.createNode(label('Swim'))
    sm.setProperty('result', result)
    sm.setProperty('event', event)
    sm.setProperty('at', at)
    sm.setProperty('time', time)
    swimmer.createRelationshipTo(sm, swam)
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

        es = tx.createNode(label('Swimmer'))
        es.setProperty('name', 'Emily Seebohm')
        es.setProperty('country', '🇦🇺')

        swim1 = tx.createNode(label('Swim'))
        swim1.setProperty('event', 'Heat 4')
        swim1.setProperty('at', 'London 2012')
        swim1.setProperty('result', 'First')
        swim1.setProperty('time', 58.23d)
        es.createRelationshipTo(swim1, swam)

        var name = es.getProperty('name')
        var country = es.getProperty('country')
        var at = swim1.getProperty('at')
        var event = swim1.getProperty('event')
        var time = swim1.getProperty('time')
        println "$name from $country swam a time of $time in $event at the $at Olympics"

        km = tx.createNode(label('Swimmer'))
        km.name = 'Kylie Masse'
        km.country = '🇨🇦'

        swim2 = tx.createNode(label('Swim'))
        swim2.time = 58.17d
        swim2.result = 'First'
        swim2.event = 'Heat 4'
        swim2.at = 'Tokyo 2021'
        km.swam(swim2)
        swim2.supersedes(swim1)

        swim3 = tx.createNode(label('Swim'))
        swim3.time = 57.72d
        swim3.result = '🥈'
        swim3.event = 'Final'
        swim3.at = 'Tokyo 2021'
        km.swam(swim3)

        rs = insertSwimmer(tx, 'Regan Smith', '🇺🇸')
        swim4 = insertSwim(tx, 'Tokyo 2021', 'Heat 5', 57.96d, 'First', rs)
        swim4.supersedes(swim2)
        swim5 = insertSwim(tx, 'Tokyo 2021', 'Semifinal 1', 57.86d, 'First', rs)
        swim6 = insertSwim(tx, 'Tokyo 2021', 'Final', 58.05d, '🥉', rs)
        swim7 = insertSwim(tx, 'Paris 2024', 'Final', 57.66d, '🥈', rs)
        swim8 = insertSwim(tx, 'Paris 2024', 'Relay leg1', 57.28d, 'First', rs)

        kmk = insertSwimmer(tx, 'Kaylie McKeown', '🇦🇺')
        swim9 = insertSwim(tx, 'Tokyo 2021', 'Heat 6', 57.88d, 'First', kmk)
        swim9.supersedes(swim4)
        swim5.supersedes(swim9)
        swim10 = insertSwim(tx, 'Tokyo 2021', 'Final', 57.47d, '🥇', kmk)
        swim10.supersedes(swim5)
        swim11 = insertSwim(tx, 'Paris 2024', 'Final', 57.33d, '🥇', kmk)
        swim11.supersedes(swim10)
        swim8.supersedes(swim11)

        kb = insertSwimmer(tx, 'Katharine Berkoff', '🇺🇸')
        swim12 = insertSwim(tx, 'Paris 2024', 'Final', 57.98d, '🥉', kb)

        var swimmers = [es, km, rs, kmk, kb]
        var successInParis = swimmers.findAll { swimmer ->
            swimmer.getRelationships(swam).any { run ->
                run.getOtherNode(swimmer).at == 'Paris 2024'
            }
        }
        assert successInParis*.country.unique() == ['🇺🇸', '🇦🇺']

        var swims = [swim1, swim2, swim3, swim4, swim5, swim6, swim7, swim8, swim9, swim10, swim11, swim12]
        var recordSetInHeat = swims.findAll { swim ->
            swim.event.startsWith('Heat')
        }*.at
        assert recordSetInHeat.unique() == ['London 2012', 'Tokyo 2021']

        var recordTimesInFinals = swims.findAll { swim ->
            swim.event == 'Final' && swim.hasRelationship(supersedes)
        }*.time
        assert recordTimesInFinals == [57.47d, 57.33d]

        var info = { s -> "$s.at $s.event" }
        println "Olympic records following ${info(swim1)}:"

        for (Path p in tx.traversalDescription()
            .breadthFirst()
            .relationships(supersedes)
            .evaluator(Evaluators.fromDepth(1))
            .uniqueness(Uniqueness.NONE)
            .traverse(swim1)) {
            println p.endNode().with(info)
        }

        assert tx.execute('''
        MATCH (s:Swim WHERE s.event STARTS WITH 'Heat')
        WITH s.at as at
        WITH DISTINCT at
        RETURN at
        ''')*.at == ['London 2012', 'Tokyo 2021']

        assert tx.execute('''
        MATCH (s1:Swim {event: 'Final'})-[:supersedes]->(s2:Swim)
        RETURN s1.time AS time
        ''')*.time == [57.47d, 57.33d]

        tx.execute('''
        MATCH (s1:Swim)-[:supersedes]->{1,}(s2:Swim { at: $at })
        RETURN s1
        ''', [at: swim1.at])*.s1.each { s ->
            println "$s.at $s.event"
        }

        assert tx.execute('''
        MATCH (sr1:Swimmer)-[:swam]->(sm1:Swim {event: 'Final'}), (sm2:Swim {event: 'Final'})-[:supersedes]->(sm3:Swim)
        WHERE sm1.at = sm2.at AND sm1 <> sm2 AND sm1.time < sm3.time
        RETURN sr1.name as name
        ''')*.name == ['Kylie Masse']

        swim6.runnerup(swim3)
        swim3.runnerup(swim10)
        swim12.runnerup(swim7)
        swim7.runnerup(swim11)

        assert tx.execute('''
        MATCH (sr1:Swimmer)-[:swam]->(sm1:Swim {event: 'Final'})-[:runnerup]->{1,2}(sm2:Swim {event: 'Final'})-[:supersedes]->(sm3:Swim)
        WHERE sm1.time < sm3.time
        RETURN sr1.name as name
        ''')*.name == ['Kylie Masse']
    }
}
