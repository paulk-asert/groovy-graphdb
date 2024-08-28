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

import groovy.sql.Sql
import org.apache.age.jdbc.base.Agtype

String DB_URL = "jdbc:postgresql://localhost:5455/postgresDB"
String USER = "postgresUser"
String PASS = "postgresPW"

Sql.withInstance(DB_URL, USER, PASS, 'org.postgresql.jdbc.PgConnection') { sql ->
    sql.connection.addDataType("agtype", Agtype)
    sql.execute("CREATE EXTENSION IF NOT EXISTS age")
    sql.execute("LOAD 'age'")
    sql.execute('SET search_path = ag_catalog, "$user", public')

    sql.execute("SELECT drop_graph('swimming_graph', true)")
    sql.execute("SELECT create_graph('swimming_graph')")
    sql.execute("SELECT create_vlabel('swimming_graph', 'swimmer')")
    sql.execute("SELECT create_vlabel('swimming_graph', 'swim')")
    sql.execute'''
        SELECT * FROM cypher('swimming_graph', $$ CREATE
        (es:Swimmer {name: 'Emily Seebohm', country: '🇦🇺'}),
        (swim1:Swim {event: 'Heat 4', result: 'First', time: 58.23, at: 'London 2012'}),
        (es)-[:swam]->(swim1),

        (km:Swimmer {name: 'Kylie Masse', country: '🇨🇦'}),
        (swim2:Swim {event: 'Heat 4', result: 'First', time: 58.17, at: 'Tokyo 2021'}),
        (km)-[:swam]->(swim2),
        (swim2)-[:supersedes]->(swim1),
        (swim3:Swim {event: 'Final', result: '🥈', time: 57.72, at: 'Tokyo 2021'}),
        (km)-[:swam]->(swim3),

        (rs:Swimmer {name: 'Regan Smith', country: '🇺🇸'}),
        (swim4:Swim {event: 'Heat 5', result: 'First', time: 57.96, at: 'Tokyo 2021'}),
        (rs)-[:swam]->(swim4),
        (swim4)-[:supersedes]->(swim2),
        (swim5:Swim {event: 'Semifinal 1', result: 'First', time: 57.86, at: 'Tokyo 2021'}),
        (rs)-[:swam]->(swim5),
        (swim6:Swim {event: 'Final', result: '🥉', time: 58.05, at: 'Tokyo 2021'}),
        (rs)-[:swam]->(swim6),
        (swim7:Swim {event: 'Final', result: '🥈', time: 57.66, at: 'Paris 2024'}),
        (rs)-[:swam]->(swim7),
        (swim8:Swim {event: 'Relay leg1', result: 'First', time: 57.28, at: 'Paris 2024'}),
        (rs)-[:swam]->(swim8),

        (kmk:Swimmer {name: 'Kaylie McKeown', country: '🇦🇺'}),
        (swim9:Swim {event: 'Heat 6', result: 'First', time: 57.88, at: 'Tokyo 2021'}),
        (kmk)-[:swam]->(swim9),
        (swim9)-[:supersedes]->(swim4),
        (swim5)-[:supersedes]->(swim9),
        (swim10:Swim {event: 'Final', result: '🥇', time: 57.47, at: 'Tokyo 2021'}),
        (kmk)-[:swam]->(swim10),
        (swim10)-[:supersedes]->(swim5),
        (swim11:Swim {event: 'Final', result: '🥇', time: 57.33, at: 'Paris 2024'}),
        (kmk)-[:swam]->(swim11),
        (swim11)-[:supersedes]->(swim10),
        (swim8)-[:supersedes]->(swim11),

        (kb:Swimmer {name: 'Katharine Berkoff', country: '🇺🇸'}),
        (swim12:Swim {event: 'Final', result: '🥉', time: 57.98, at: 'Paris 2024'}),
        (kb)-[:swam]->(swim12)
        $$) AS (a agtype)
    '''

    assert sql.rows('''
        SELECT * from cypher('swimming_graph', $$
        MATCH (s:Swim)
        WHERE left(s.event, 4) = 'Heat'
        RETURN s
        $$) AS (a agtype)
    ''').a*.map*.get('properties')*.at.toUnique() == ['London 2012', 'Tokyo 2021']

    assert sql.rows('''
        SELECT * from cypher('swimming_graph', $$
        MATCH (s1:Swim {event: 'Final'})-[:supersedes]->(s2:Swim)
        RETURN s1
        $$) AS (a agtype)
    ''').a*.map*.get('properties')*.time == [57.47, 57.33]

    sql.eachRow('''
        SELECT * from cypher('swimming_graph', $$
        MATCH (s1:Swim)-[:supersedes]->(swim1)
        RETURN s1
        $$) AS (a agtype)
    ''') {
        println it.a*.map*.get('properties')[0].with{ "$it.at $it.event" }
    }
}
