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
import gql.DSL
import graphql.schema.DataFetchingEnvironment

var swimmerType = DSL.type('Swimmer') {
    field 'name', GraphQLString
    field 'country', GraphQLString
}

var swimType = DSL.type('Swim') {
    field 'who', swimmerType
    field 'at', GraphQLString
    field 'result', GraphQLString
    field 'event', GraphQLString
    field 'time', GraphQLFloat
}

record Swimmer(String name, String country) {}

record Swim(Swimmer who, String at, String result, String event, double time) {}

var es = new Swimmer('Emily Seebohm', '🇦🇺')
var km = new Swimmer('Kylie Masse', '🇨🇦')
var rs = new Swimmer('Regan Smith', '🇺🇸')
var kmk = new Swimmer('Kaylee McKeown', '🇦🇺')
var kb = new Swimmer('Katharine Berkoff', '🇺🇸')

var swim1 = new Swim(es, 'London 2012', 'First', 'Heat 4', 58.23)
var swim2 = new Swim(km, 'Tokyo 2021', 'First', 'Heat 4', 58.17)
var swim3 = new Swim(km, 'Tokyo 2021', '🥈', 'Final', 57.72)
var swim4 = new Swim(rs, 'Tokyo 2021', 'First', 'Heat 5', 57.96)
var swim5 = new Swim(rs, 'Tokyo 2021', 'First', 'Semifinal 1', 57.86)
var swim6 = new Swim(rs, 'Tokyo 2021', '🥉', 'Final', 58.05)
var swim7 = new Swim(rs, 'Paris 2024', '🥈', 'Final', 57.66)
var swim8 = new Swim(rs, 'Paris 2024', 'First', 'Relay leg1', 57.28)
var swim9 = new Swim(kmk, 'Tokyo 2021', 'First', 'Heat 6', 57.88)
var swim10 = new Swim(kmk, 'Tokyo 2021', '🥇', 'Final', 57.47)
var swim11 = new Swim(kmk, 'Paris 2024', '🥇', 'Final', 57.33)
var swim12 = new Swim(kb, 'Paris 2024', '🥉', 'Final', 57.98)

var swims = [swim1, swim2, swim3, swim4, swim5, swim6,
             swim7, swim8, swim9, swim10, swim11, swim12]

var supersedes = [
    [swim2, swim1],
    [swim4, swim2],
    [swim9, swim4],
    [swim5, swim9],
    [swim10, swim5],
    [swim11, swim10],
    [swim8, swim11],
]

var schema = DSL.schema {
    queries {
        field('findSwim') {
            type swimType
            argument 'name', GraphQLString
            argument 'at', GraphQLString
            argument 'event', GraphQLString
            fetcher { DataFetchingEnvironment env ->
                var name = env.arguments.name
                var at = env.arguments.at
                var event = env.arguments.event
                swims.find{ s -> s.who.name == name && s.at == at && s.event == event }
            }
        }
        field('recordsInFinals') {
            type list(swimType)
            fetcher { DataFetchingEnvironment env ->
                swims.findAll{ s -> s.event == 'Final' && supersedes.any{ it[0] == s } }
            }
        }
        field('recordsInHeats') {
            type list(swimType)
            fetcher { DataFetchingEnvironment env ->
                swims.findAll{ s -> s.event.startsWith('Heat') &&
                    (supersedes[0][1] == s || supersedes.any{ it[0] == s }) }
            }
        }
        field('success') {
            type list(swimmerType)
            argument 'at', GraphQLString
            fetcher { DataFetchingEnvironment env ->
                swims.findAll{ s -> s.at == env.arguments.at }*.who
            }
        }
        field('allRecords') {
            type list(swimType)
            fetcher { DataFetchingEnvironment env ->
                supersedes.collect{it[0] }
            }
        }
    }
}

swim1.with {
    println "$who.name from $who.country swam a time of $time in $event at the $at Olympics"
}

DSL.execute(schema, '''
    query findSwim($name: String!, $at: String!, $event: String!) {
        findSwim(name: $name, at: $at, event: $event) {
            who {
                name
                country
            }
            event
            at
            time
        }
    }
''', [name: 'Emily Seebohm', at: 'London 2012', event: 'Heat 4']).data.findSwim.with {
    println "$who.name from $who.country swam a time of $time in $event at the $at Olympics"
}

/* Times for olympic records set in finals */
assert DSL.execute(schema, '''{
    recordsInFinals {
        time
    }
}''').data.recordsInFinals*.time == [57.47, 57.33]

/* At which olympics were records set in heats */
assert DSL.newExecutor(schema).execute {
    query('recordsInHeats') {
        returns(Swim) {
            at
        }
    }
}.data.recordsInHeats*.at.toUnique() == ['London 2012', 'Tokyo 2021']

/* Successful countries in Paris 2024 */
var query = DSL.buildQuery {
    query('success', [at: 'Paris 2024']) {
        returns(Swimmer) {
            country
        }
    }
}
assert DSL.execute(schema, query).data.success*.country.toUnique() == ['🇺🇸', '🇦🇺']

/* Print all records since London 2012 */
DSL.execute(schema, '''{
    allRecords {
        at
        event
    }
}''').data.allRecords.each {
    println "$it.at $it.event"
}
