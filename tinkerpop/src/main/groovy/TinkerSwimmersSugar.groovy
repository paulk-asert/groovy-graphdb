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

import org.apache.tinkerpop.gremlin.groovy.loaders.SugarLoader
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.in
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.select

SugarLoader.load()

def insertSwimmer(TraversalSource g, name, country) {
    g.addV('swimmer').property(name: name, country: country).next()
}

def insertSwim(TraversalSource g, at, event, time, result, swimmer) {
    g.addV('swim').property(at: at, event: event, time: time, result: result).next().tap { swim ->
        swimmer.addEdge('swam', swim)
    }
}

var graph = TinkerGraph.open()
var g = traversal().withEmbedded(graph)

var es = g.addV('swimmer').property(name: 'Emily Seebohm', country: '🇦🇺').next()
swim1 = g.addV('swim').property(at: 'London 2012', event: 'Heat 4', time: 58.23, result: 'First').next()
es.addEdge('swam', swim1)

println "$es.name from $es.country swam a time of $swim1.time in $swim1.event at the $swim1.at Olympics"

var km = insertSwimmer(g, 'Kylie Masse', '🇨🇦')
var swim2 = insertSwim(g, 'Tokyo 2021', 'Heat 4', 58.17, 'First', km)
swim2.addEdge('supersedes', swim1)
var swim3 = insertSwim(g, 'Tokyo 2021', 'Final', 57.72, '🥈', km)

var rs = insertSwimmer(g, 'Regan Smith', '🇺🇸')
var swim4 = insertSwim(g, 'Tokyo 2021', 'Heat 5', 57.96, 'First', rs)
swim4.addEdge('supersedes', swim2)
var swim5 = insertSwim(g, 'Tokyo 2021', 'Semifinal 1', 57.86, '', rs)
var swim6 = insertSwim(g, 'Tokyo 2021', 'Final', 58.05, '🥉', rs)
var swim7 = insertSwim(g, 'Paris 2024', 'Final', 57.66, '🥈', rs)
var swim8 = insertSwim(g, 'Paris 2024', 'Relay leg1', 57.28, 'First', rs)

var kmk = insertSwimmer(g, 'Kaylie McKeown', '🇦🇺')
var swim9 = insertSwim(g, 'Tokyo 2021', 'Heat 6', 57.88, 'First', kmk)
swim9.addEdge('supersedes', swim4)
swim5.addEdge('supersedes', swim9)
var swim10 = insertSwim(g, 'Tokyo 2021', 'Final', 57.47, '🥇', kmk)
swim10.addEdge('supersedes', swim5)
var swim11 = insertSwim(g, 'Paris 2024', 'Final', 57.33, '🥇', kmk)
swim11.addEdge('supersedes', swim10)
swim8.addEdge('supersedes', swim11)

var kb = insertSwimmer(g, 'Katharine Berkoff', '🇺🇸')
var swim12 = insertSwim(g, 'Paris 2024', 'Final', 57.98, '🥉', kb)

var successInParis = g.V.out('swam').has('at', 'Paris 2024').in.country.toSet
assert successInParis == ['🇺🇸', '🇦🇺'] as Set

var recordSetInHeat = g.V.hasLabel('swim').filter { it.event.startsWith('Heat') }.at.toSet
assert recordSetInHeat == ['London 2012', 'Tokyo 2021'] as Set

var recordTimesInFinals = g.V.has('event', 'Final').as('ev').out('supersedes').select('ev').time.toSet
assert recordTimesInFinals == [57.47, 57.33] as Set

println "Olympic records after ${g.V(swim1).values('at', 'event').toList().join(' ')}: "
println g.V(swim1).repeat(in('supersedes')).as('sw').emit
    .at.concat(' ').concat(select('sw').event).toList.join('\n')
