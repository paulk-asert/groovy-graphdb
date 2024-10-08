/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
//import org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLWriter
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.in
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.select

static insertAthlete(TraversalSource g, first, last, dob) {
    g.addV('athlete').property(first: first, last: last, dob: dob).next()
}

static insertRun(TraversalSource g, h, m, s, where, when, athlete) {
    var run = g.addV('marathon').property(distance: 42195, when: when, where: where, time: h * 60 * 60 + m * 60 + s).next()
    athlete.addEdge('won', run)
    run
}

Vertex athlete1, athlete2, athlete3, athlete4
Vertex marathon1, marathon2a, marathon2b, marathon3, marathon4a, marathon4b

def run() {
    var graph = TinkerGraph.open()
    var g = traversal().withEmbedded(graph)
    athlete1 = g.addV('athlete').property(first: 'Paul', last: 'Tergat', dob: '1969-06-17').next()
    marathon1 = g.addV('marathon').property(distance: 42195, when: '2003-09-28', where: 'Berlin', time: 2 * 60 * 60 + 4 * 60 + 55).next()
    athlete1.addEdge('won', marathon1)

    // show two different ways to extract properties:
    var (first, last) = ['first', 'last'].collect{athlete1.property(it).value() }
    var (where, when) = ['where', 'when'].collect{ g.V(marathon1).values(it)[0] }
    println "$first $last won the $where marathon on $when"

    athlete2 = insertAthlete(g, 'Khalid', 'Khannouchi', '1971-12-22')
    marathon2a = insertRun(g, 2, 5, 38, 'London', '2002-04-14', athlete2)
    marathon2b = insertRun(g, 2, 5, 42, 'Chicago', '1999-10-24', athlete2)

    athlete3 = insertAthlete(g, 'Ronaldo', 'da Costa', '1970-06-07')
    marathon3 = insertRun(g, 2, 6, 5, 'Berlin', '1998-09-20', athlete3)

    athlete4 = insertAthlete(g, 'Paula', 'Radcliffe', '1973-12-17')
    marathon4a = insertRun(g, 2, 17, 18, 'Chicago', '2002-10-13', athlete4)
    marathon4b = insertRun(g, 2, 15, 25, 'London', '2003-04-13', athlete4)

    var wonInLondon = g.V().out('won').has('where', 'London').in()
        .values('last').toSet()
    assert wonInLondon == ['Khannouchi', 'Radcliffe'] as Set

    marathon2b.addEdge('supersedes', marathon3)
    marathon2a.addEdge('supersedes', marathon2b)
    marathon1.addEdge('supersedes', marathon2a)
    marathon4b.addEdge('supersedes', marathon4a)

    var bornAfter1970 = g.V().hasLabel('athlete')
        .filter{ it.get().property('dob').value()[0..3] > '1970' }
        .values('last').toSet()
    assert bornAfter1970 == ['Radcliffe', 'Khannouchi'] as Set

    var londonRecordDates = g.V().has('where', 'London').as('m').out('supersedes')
        .select('m').values('when').toSet()
    assert londonRecordDates == ['2002-04-14', '2003-04-13'] as Set

    println "World records after ${g.V(marathon3).values('where', 'when').toList().join(' ')}: "
    println g.V(marathon3).repeat(in('supersedes')).as('m').emit()
        .values('where').concat(' ')
        .concat(select('m').values('when')).toList()

//    var writer = GraphMLWriter.build().normalize(true).create()
//    new File("/tmp/athletes.graphml").withOutputStream { os ->
//        writer.writeGraph(os, graph)
//    }
}
