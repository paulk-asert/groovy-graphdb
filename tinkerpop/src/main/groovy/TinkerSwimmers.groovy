import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLWriter
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.in
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.select

static insertSwimmer(TraversalSource g, name, country) {
    g.addV('swimmer').property(name: name, country: country).next()
}

static insertSwim(TraversalSource g, where, event, time, result, swimmer) {
    var swim = g.addV('swim').property(where: where, event: event, time: time, result: result).next()
    swimmer.addEdge('swam', swim)
    swim
}

Vertex es, kmk, km, rs, kb
Vertex swim1, swim2, swim3, swim4, swim5, swim6, swim7, swim8, swim9, swim10, swim11, swim12

def run() {
    var graph = TinkerGraph.open()
    var g = traversal().withEmbedded(graph)
    es = g.addV('swimmer').property(name: 'Emily Seebohm', country: '🇦🇺').next()
    swim1 = g.addV('swim').property(where: 'London 2012', event: 'Heat 4', time: 58.23, result: 'First').next()
    es.addEdge('swam', swim1)

    def (name, country) = ['name', 'country'].collect{g.V(es).values(it)[0] }
    def (where, event, time) = ['where', 'event', 'time'].collect{ g.V(swim1).values(it)[0] }
    println "$name from $country swam a time of $time in $event at the $where Olympics"

    km = insertSwimmer(g, 'Kylie Masse', '🇨🇦')
    swim2 = insertSwim(g, 'Tokyo 2021', 'Heat 4', 58.17, 'First', km)
    swim2.addEdge('supercedes', swim1)
    swim3 = insertSwim(g, 'Tokyo 2021', 'Final', 58.17, '🥈', km)

    rs = insertSwimmer(g, 'Regan Smith', '🇺🇸')
    swim4 = insertSwim(g, 'Tokyo 2021', 'Heat 5', 57.96, 'First', rs)
    swim4.addEdge('supercedes', swim2)
    swim5 = insertSwim(g, 'Tokyo 2021', 'Semifinal 1', 57.86, '', rs)
    swim6 = insertSwim(g, 'Tokyo 2021', 'Final', 58.05, '🥉', rs)
    swim7 = insertSwim(g, 'Paris 2024', 'Final', 57.66, '🥈', rs)
    swim8 = insertSwim(g, 'Paris 2024', 'Relay leg1', 57.28, 'First', rs)

    kmk = insertSwimmer(g, 'Kaylie McKeown', '🇦🇺')
    swim9 = insertSwim(g, 'Tokyo 2021', 'Heat 6', 57.88, 'First', kmk)
    swim9.addEdge('supercedes', swim4)
    swim5.addEdge('supercedes', swim9)
    swim10 = insertSwim(g, 'Tokyo 2021', 'Final', 57.47, '🥇', kmk)
    swim10.addEdge('supercedes', swim5)
    swim11 = insertSwim(g, 'Paris 2024', 'Final', 57.33, '🥇', kmk)
    swim11.addEdge('supercedes', swim10)
    swim8.addEdge('supercedes', swim11)

    kb = insertSwimmer(g, 'Katharine Berkoff', '🇺🇸')
    swim12 = insertSwim(g, 'Paris 2024', 'Final', 57.98, '🥉', kb)

    def successInParis = g.V().out('swam').has('where', 'Paris 2024').in()
        .values('country').toSet()
    assert successInParis == ['🇺🇸', '🇦🇺'] as Set

    def recordSetInHeat = g.V().hasLabel('swim')
        .filter{ it.get().property('event').value().startsWith('Heat') }
        .values('where').toSet()
    assert recordSetInHeat == ['London 2012', 'Tokyo 2021'] as Set

    def recordTimesInFinals = g.V().has('event', 'Final').as('ev').out('supercedes')
        .select('ev').values('time').toSet()
    assert recordTimesInFinals == [57.47, 57.33] as Set

    println "Olympic records after ${g.V(swim1).values('where', 'event').toList().join(' ')}: "
    println g.V(swim1).repeat(in('supercedes')).as('sw').emit()
        .values('where').concat(' ')
        .concat(select('sw').values('event')).toList().join('\n')

//    def writer = GraphMLWriter.build().normalize(true).create()
//    new File("/tmp/swimmers.graphml").withOutputStream { os ->
//        writer.writeGraph(os, graph)
//    }

}
