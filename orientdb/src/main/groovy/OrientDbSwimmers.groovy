import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.db.OrientDB
import com.orientechnologies.orient.core.db.OrientDBConfig

var context = new OrientDB("embedded:", OrientDBConfig.defaultConfig())
context.execute("create database swimming memory users(admin identified by 'adminpwd' role admin)").close()

def insertSwimmer(ODatabaseSession db, name, country) {
    var swimmer = db.newVertex('Swimmer')
    swimmer.setProperty('name', name)
    swimmer.setProperty('country', country)
    swimmer
}

def insertSwim(ODatabaseSession db, at, event, time, result, swimmer) {
    var swim = db.newVertex('Swim')
    swim.setProperty('at', at)
    swim.setProperty('result', result)
    swim.setProperty('event', event)
    swim.setProperty('time', time)
    swimmer.addEdge(swim, 'swam')
    swim
}

try (var db = context.open("swimming", "admin", "adminpwd")) {
    db.createVertexClass('Swimmer')
    db.createVertexClass('Swim')
    db.createEdgeClass('swam')
    db.createEdgeClass('supersedes')

    var es = db.newVertex('Swimmer')
    es.setProperty('name', 'Emily Seebohm')
    es.setProperty('country', '🇦🇺')
    var swim1 = db.newVertex('Swim')
    swim1.setProperty('at', 'London 2012')
    swim1.setProperty('result', 'First')
    swim1.setProperty('event', 'Heat 4')
    swim1.setProperty('time', 58.23)
    es.addEdge(swim1, 'swam')

    var (name, country) = ['name', 'country'].collect { es.getProperty(it) }
    var (at, event, time) = ['at', 'event', 'time'].collect { swim1.getProperty(it) }
    println "$name from $country swam a time of $time in $event at the $at Olympics"

    var km = insertSwimmer(db, 'Kylie Masse', '🇨🇦')
    var swim2 = insertSwim(db, 'Tokyo 2021', 'Heat 4', 58.17, 'First', km)
    swim2.addEdge(swim1, 'supersedes')
    var swim3 = insertSwim(db, 'Tokyo 2021', 'Final', 57.72, '🥈', km)

    var rs = insertSwimmer(db, 'Regan Smith', '🇺🇸')
    var swim4 = insertSwim(db, 'Tokyo 2021', 'Heat 5', 57.96, 'First', rs)
    swim4.addEdge(swim2, 'supersedes')
    var swim5 = insertSwim(db, 'Tokyo 2021', 'Semifinal 1', 57.86, '', rs)
    var swim6 = insertSwim(db, 'Tokyo 2021', 'Final', 58.05, '🥉', rs)
    var swim7 = insertSwim(db, 'Paris 2024', 'Final', 57.66, '🥈', rs)
    var swim8 = insertSwim(db, 'Paris 2024', 'Relay leg1', 57.28, 'First', rs)

    var kmk = insertSwimmer(db, 'Kaylie McKeown', '🇦🇺')
    var swim9 = insertSwim(db, 'Tokyo 2021', 'Heat 6', 57.88, 'First', kmk)
    swim9.addEdge(swim4, 'supersedes')
    swim5.addEdge(swim9, 'supersedes')
    var swim10 = insertSwim(db, 'Tokyo 2021', 'Final', 57.47, '🥇', kmk)
    swim10.addEdge(swim5, 'supersedes')
    var swim11 = insertSwim(db, 'Paris 2024', 'Final', 57.33, '🥇', kmk)
    swim11.addEdge(swim10, 'supersedes')
    swim8.addEdge(swim11, 'supersedes')

    var kb = insertSwimmer(db, 'Katharine Berkoff', '🇺🇸')
    var swim12 = insertSwim(db, 'Paris 2024', 'Final', 57.98, '🥉', kb)

}

context.close()
