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

//import org.neo4j.driver.AuthTokens
//import org.neo4j.driver.GraphDatabase
//import org.neo4j.driver.Result
//import org.neo4j.driver.SessionConfig
import com.antgroup.tugraph.TuGraphDbRpcClient
import groovy.transform.Field


@Field client = new TuGraphDbRpcClient('localhost:9090', "admin", "73@TuGraph")
//var authToken = AuthTokens.basic("admin", "73@TuGraph")
//var driver = GraphDatabase.driver("bolt://localhost:7687", authToken)

//var session = driver.session(SessionConfig.forDatabase("default"))
def callCypher(String s) {
    client.callCypher(s, "default", 100)
}
'''
CALL db.dropDB()
CALL db.createVertexLabel('Swimmer', 'name', 'name', STRING, false, 'country', STRING, false)
CALL db.createVertexLabel('Swim', 'id', 'id', INT32, false, 'event', STRING, false, 'result', STRING, false, 'at', STRING, false, 'time', FLOAT, false)
CALL db.createEdgeLabel('swam','[["Swimmer","Swim"]]')
CALL db.createEdgeLabel('supersedes','[["Swim","Swim"]]')
'''.readLines().grep().each(this::callCypher)

callCypher '''create
    (es:Swimmer {name: 'Emily Seebohm', country: 'AU'}),
    (swim1:Swim {event: 'Heat 4', result: 'First', time: 58.23, at: 'London 2012', id:1}),
    (es)-[:swam]->(swim1),
    (km:Swimmer {name: 'Kylie Masse', country: 'CA'}),
    (swim2:Swim {event: 'Heat 4', result: 'First', time: 58.17, at: 'Tokyo 2021', id:2}),
    (km)-[:swam]->(swim2),
    (swim3:Swim {event: 'Final', result: 'Silver', time: 57.72, at: 'Tokyo 2021', id:3}),
    (km)-[:swam]->(swim3),
    (swim2)-[:supersedes]->(swim1),
    (rs:Swimmer {name: 'Regan Smith', country: 'US'}),
    (swim4:Swim {event: 'Heat 5', result: 'First', time: 57.96, at: 'Tokyo 2021', id:4}),
    (rs)-[:swam]->(swim4),
    (swim5:Swim {event: 'Semifinal 1', result: 'First', time: 57.86, at: 'Tokyo 2021', id:5}),
    (rs)-[:swam]->(swim5),
    (swim6:Swim {event: 'Final', result: 'Bronze', time: 58.05, at: 'Tokyo 2021', id:6}),
    (rs)-[:swam]->(swim6),
    (swim7:Swim {event: 'Final', result: 'Silver', time: 57.66, at: 'Paris 2024', id:7}),
    (rs)-[:swam]->(swim7),
    (swim8:Swim {event: 'Relay leg1', result: 'First', time: 57.28, at: 'Paris 2024', id:8}),
    (rs)-[:swam]->(swim8),
    (swim4)-[:supersedes]->(swim2),
    (kmk:Swimmer {name: 'Kaylie McKeown', country: 'AU'}),
    (swim9:Swim {event: 'Heat 6', result: 'First', time: 57.88, at: 'Tokyo 2021', id:9}),
    (kmk)-[:swam]->(swim9),
    (swim9)-[:supersedes]->(swim4),
    (swim5)-[:supersedes]->(swim9),
    (swim10:Swim {event: 'Final', result: 'Gold', time: 57.47, at: 'Tokyo 2021', id:10}),
    (kmk)-[:swam]->(swim10),
    (swim10)-[:supersedes]->(swim5),
    (swim11:Swim {event: 'Final', result: 'Gold', time: 57.33, at: 'Paris 2024', id:11}),
    (kmk)-[:swam]->(swim11),
    (swim11)-[:supersedes]->(swim10),
    (swim8)-[:supersedes]->(swim11)
'''

callCypher '''create
    (kb:Swimmer {name: 'Katharine Berkoff', country: 'UA'}),
    (swim12:Swim {event: 'Final', result: 'Bronze', time: 57.98, at: 'Paris 2024', id:12}),
    (kb)-[:swam]->(swim12)
'''

assert callCypher('''
    MATCH (s:Swim)
    WHERE s.event STARTS WITH 'Heat'
    RETURN DISTINCT s.at AS at
''') == '[{"at":"London 2012"},{"at":"Tokyo 2021"}]'

assert callCypher('''
    MATCH (s1:Swim {event: 'Final'})-[:supersedes]->(s2:Swim)
    RETURN s1.time as time
''') == '[{"time":57.47},{"time":57.33}]'

assert callCypher('''
    MATCH (s1:Swim)-[:supersedes*1..10]->(s2:Swim)
    WHERE s2.at = 'London 2012'
    RETURN s1.at as at, s1.event as event
''') == '''
    [{"at":"Tokyo 2021","event":"Heat 4"},
     {"at":"Tokyo 2021","event":"Heat 5"},
     {"at":"Tokyo 2021","event":"Semifinal 1"},
     {"at":"Paris 2024","event":"Relay leg1"},
     {"at":"Tokyo 2021","event":"Heat 6"},
     {"at":"Tokyo 2021","event":"Final"},
     {"at":"Paris 2024","event":"Final"}]
'''.readLines()*.trim().join()
