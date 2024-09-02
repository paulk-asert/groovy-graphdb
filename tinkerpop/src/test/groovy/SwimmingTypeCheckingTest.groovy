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

import groovy.transform.TypeChecked
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal

def init() {
    Vertex.metaClass.coaches = { Vertex other -> delegate.addEdge('coaches', other) }
}

@TypeChecked(extensions = 'SwimmingChecker.groovy')
def method() {
    init()
    var graph = TinkerGraph.open()
    var g = traversal().withEmbedded(graph)
    var swim1 = g.addV('Swim').property(at: 'London 2012', event: 'Heat 4', time: 58.23, result: 'First').next()
    var kmk = g.addV('Swimmer').property(name: 'Kaylee McKeown', country: '🇦🇺').next()
    var mb = g.addV('Coach').property(name: 'Michael Bohl').next()
    mb.coaches(kmk)
//    swim1.coaches(mb)
}
