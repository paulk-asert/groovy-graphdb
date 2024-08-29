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

import groovy.transform.Field
import org.apache.hugegraph.driver.HugeClient
import org.apache.hugegraph.structure.constant.T

// setup (or run outside docker if you prefer):
// docker run -itd --name=graph -p 8080:8080 hugegraph/hugegraph

@Field int NUM = 1

var client = HugeClient.builder("http://localhost:8080", "hugegraph").build()
var schema = client.schema()

schema.propertyKey("num").asInt().ifNotExist().create()
schema.propertyKey("name").asText().ifNotExist().create()
schema.propertyKey("country").asText().ifNotExist().create()
schema.propertyKey("at").asText().ifNotExist().create()
schema.propertyKey("event").asText().ifNotExist().create()
schema.propertyKey("result").asText().ifNotExist().create()
schema.propertyKey("time").asDouble().ifNotExist().create()

schema.vertexLabel('Swimmer')
    .properties('name', 'country')
    .primaryKeys('name')
    .ifNotExist()
    .create()

schema.vertexLabel('Swim')
    .properties('num', 'at', 'event', 'result', 'time')
    .primaryKeys('num')
    .ifNotExist()
    .create()

schema.edgeLabel("swam")
    .sourceLabel("Swimmer")
    .targetLabel("Swim")
    .ifNotExist()
    .create()

schema.edgeLabel("supersedes")
    .sourceLabel("Swim")
    .targetLabel("Swim")
    .ifNotExist()
    .create()

schema.indexLabel("SwimByEvent")
    .onV("Swim")
    .by("event")
    .secondary()
    .ifNotExist()
    .create()

schema.indexLabel("SwimByAt")
    .onV("Swim")
    .by("at")
    .secondary()
    .ifNotExist()
    .create()

def insertSwimmer(g, name, country) {
    g.addVertex(T.LABEL, 'Swimmer', 'name', name, 'country', country)
}

def insertSwim(g, at, event, time, result, swimmer) {
    var swim = g.addVertex(T.LABEL, 'Swim', 'at', at, 'event', event, 'time', time, 'result', result, 'num', NUM++)
    swimmer.addEdge('swam', swim)
    swim
}

def supersedes(swimA, swimB) {
    swimA.addEdge('supersedes', swimB)
}

var g = client.graph()
var es = g.addVertex(T.LABEL, 'Swimmer', 'name', 'Emily Seebohm', 'country', '🇦🇺')
var swim1 = g.addVertex(T.LABEL, 'Swim', 'at', 'London 2012', 'event', 'Heat 4', 'time', 58.23, 'result', 'First', 'num', NUM++)
es.addEdge('swam', swim1)

var (name, country) = ['name', 'country'].collect { es.property(it) }
var (at, event, time) = ['at', 'event', 'time'].collect { swim1.property(it) }
println "$name from $country swam a time of $time in $event at the $at Olympics"

var km = insertSwimmer(g, 'Kylie Masse', '🇨🇦')
var swim2 = insertSwim(g, 'Tokyo 2021', 'Heat 4', 58.17, 'First', km)
supersedes(swim2, swim1)
var swim3 = insertSwim(g, 'Tokyo 2021', 'Final', 57.72, '🥈', km)

var rs = insertSwimmer(g, 'Regan Smith', '🇺🇸')
var swim4 = insertSwim(g, 'Tokyo 2021', 'Heat 5', 57.96, 'First', rs)
supersedes(swim4, swim2)
var swim5 = insertSwim(g, 'Tokyo 2021', 'Semifinal 1', 57.86, '', rs)
var swim6 = insertSwim(g, 'Tokyo 2021', 'Final', 58.05, '🥉', rs)
var swim7 = insertSwim(g, 'Paris 2024', 'Final', 57.66, '🥈', rs)
var swim8 = insertSwim(g, 'Paris 2024', 'Relay leg1', 57.28, 'First', rs)

var kmk = insertSwimmer(g, 'Kaylie McKeown', '🇦🇺')
var swim9 = insertSwim(g, 'Tokyo 2021', 'Heat 6', 57.88, 'First', kmk)
supersedes(swim9, swim4)
supersedes(swim5, swim9)
var swim10 = insertSwim(g, 'Tokyo 2021', 'Final', 57.47, '🥇', kmk)
supersedes(swim10, swim5)
var swim11 = insertSwim(g, 'Paris 2024', 'Final', 57.33, '🥇', kmk)
supersedes(swim11, swim10)
supersedes(swim8, swim11)

var kb = insertSwimmer(g, 'Katharine Berkoff', '🇺🇸')
var swim12 = insertSwim(g, 'Paris 2024', 'Final', 57.98, '🥉', kb)

var gremlin = client.gremlin()

var successInParis = gremlin.gremlin('''
    g.V().out('swam').has('Swim', 'at', 'Paris 2024').in().values('country').dedup().order()
''').execute()
assert successInParis.data() == ['🇦🇺', '🇺🇸']

var recordSetInHeat = gremlin.gremlin('''
    g.V().hasLabel('Swim')
        .filter { it.get().property('event').value().startsWith('Heat') }
        .values('at').dedup().order()
''').execute()
assert recordSetInHeat.data() == ['London 2012', 'Tokyo 2021']

var recordTimesInFinals = gremlin.gremlin('''
    g.V().has('Swim', 'event', 'Final').as('ev').out('supersedes').select('ev').values('time').order()
''').execute()
assert recordTimesInFinals.data() == [57.33, 57.47]

println "Olympic records after ${swim1.properties().subMap(['at', 'event']).values().join(' ')}: "
gremlin.gremlin('''
    g.V().has('at', 'London 2012').repeat(__.in('supersedes')).emit().values('at', 'event')
''').execute().data().collate(2).each { a, e ->
    println "$a $e"
}
