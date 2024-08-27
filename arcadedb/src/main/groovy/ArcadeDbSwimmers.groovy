import com.arcadedb.database.DatabaseFactory
import com.arcadedb.gremlin.ArcadeGraph

var factory = new DatabaseFactory("/tmp/swimming")

def insertSwimmer(db, name, country) {
    var swimmer = db.newVertex('Swimmer')
    swimmer.set(name: name, country: country).save()
}

def insertSwim(db, at, event, time, result, swimmer) {
    var swim = db.newVertex('Swim')
    swim.set(at: at, result: result, event: event, time: time).save()
    swim.newEdge('swam', swimmer, false).save()
    swim
}

def supersedes(swimA, swimB) {
    swimB.newEdge('supersedes', swimA, false).save()
}

if (factory.exists()) {
    factory.open().drop()
}

try(var db = factory.create()) {
    db.transaction { ->
        db.schema.with {
            createVertexType('Swimmer')
            createVertexType('Swim')
            createEdgeType('swam')
            createEdgeType('supersedes')
        }

        var es = db.newVertex('Swimmer')
        es.set(name: 'Emily Seebohm', country: '🇦🇺').save()

        var swim1 = db.newVertex('Swim')
        swim1.set(at: 'London 2012', result: 'First', event: 'Heat 4', time: 58.23).save()
        swim1.newEdge('swam', es, false).save()

        var (name, country) = ['name', 'country'].collect { es.get(it) }
        var (at, event, time) = ['at', 'event', 'time'].collect { swim1.get(it) }
        println "$name from $country swam a time of $time in $event at the $at Olympics"

        var km = insertSwimmer(db, 'Kylie Masse', '🇨🇦')
        var swim2 = insertSwim(db, 'Tokyo 2021', 'Heat 4', 58.17, 'First', km)
        supersedes(swim2, swim1)
        var swim3 = insertSwim(db, 'Tokyo 2021', 'Final', 57.72, '🥈', km)

        var rs = insertSwimmer(db, 'Regan Smith', '🇺🇸')
        var swim4 = insertSwim(db, 'Tokyo 2021', 'Heat 5', 57.96, 'First', rs)
        supersedes(swim4, swim2)
        var swim5 = insertSwim(db, 'Tokyo 2021', 'Semifinal 1', 57.86, 'First', rs)
        var swim6 = insertSwim(db, 'Tokyo 2021', 'Final', 58.05, '🥉', rs)
        var swim7 = insertSwim(db, 'Paris 2024', 'Final', 57.66, '🥈', rs)
        var swim8 = insertSwim(db, 'Paris 2024', 'Relay leg1', 57.28, 'First', rs)

        var kmk = insertSwimmer(db, 'Kaylie McKeown', '🇦🇺')
        var swim9 = insertSwim(db, 'Tokyo 2021', 'Heat 6', 57.88, 'First', kmk)
        supersedes(swim9, swim4)
        supersedes(swim5, swim9)
        var swim10 = insertSwim(db, 'Tokyo 2021', 'Final', 57.47, '🥇', kmk)
        supersedes(swim10, swim5)
        var swim11 = insertSwim(db, 'Paris 2024', 'Final', 57.33, '🥇', kmk)
        supersedes(swim11, swim10)
        supersedes(swim8, swim11)

        var kb = insertSwimmer(db, 'Katharine Berkoff', '🇺🇸')
        var swim12 = insertSwim(db, 'Paris 2024', 'Final', 57.98, '🥉', kb)

        var results = db.query('SQL', '''
        SELECT expand(outV()) FROM (SELECT expand(outE('supersedes')) FROM Swim WHERE event = 'Final')
        ''')
        assert results*.toMap().time.toSet() == [57.47, 57.33] as Set

        results = db.query('gremlin', '''
        g.V().has('event', 'Final').as('ev').out('supersedes').select('ev').values('time')
        ''')
        assert results*.toMap().result.toSet() == [57.47, 57.33] as Set

        results = db.query('cypher', '''
        MATCH (s1:Swim {event: 'Final'})-[:supersedes]->(s2:Swim)
        RETURN s1.time AS time
        ''')
        assert results*.toMap().time.toSet() == [57.47, 57.33] as Set

        results = db.query('SQL', "SELECT expand(outV()) FROM (SELECT expand(outE('supersedes')) FROM Swim WHERE event.left(4) = 'Heat')")
        assert results*.toMap().at.toSet() == ['Tokyo 2021', 'London 2012'] as Set

        results = db.query('SQL', "SELECT country FROM ( SELECT expand(out('swam')) FROM Swim WHERE at = 'Paris 2024' )")
        assert results*.toMap().country.toSet() == ['🇺🇸', '🇦🇺'] as Set

        results = db.query('SQL', "TRAVERSE out('supersedes') FROM :swim", swim1)
        results.each {
            if (it.toElement() != swim1) {
                var props = it.toMap()
                println "$props.at $props.event"
            }
        }
    }

}

factory.close()

try (final ArcadeGraph graph = ArcadeGraph.open("/tmp/swimming")) {
    var recordTimesInFinals = graph.traversal().V().has('event', 'Final').as('ev').out('supersedes')
        .select('ev').values('time').toSet()
    assert recordTimesInFinals == [57.47, 57.33] as Set
}
