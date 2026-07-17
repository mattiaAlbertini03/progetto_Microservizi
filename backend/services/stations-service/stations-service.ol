include "stations-interface.iol"
include "database.iol"
include "runtime.iol"

inputPort SoapInput {
    Location: "socket://0.0.0.0:8080/"
    Protocol: soap {
        .wsdl = "./StationsService.wsdl";
        .wsdl.port = "MyInputServicePort"
    }
    Interfaces: StationSoapService
}

inputPort WebInput {
    Location: "socket://0.0.0.0:8000/"
    Protocol: http {
        .format = "json"
    }
    Interfaces: StationRestService
}

execution { concurrent }

init {
    getenv@Runtime("STATIONS_DB_HOST")(dbHost);
    getenv@Runtime("STATIONS_DB_PORT")(dbPort);
    getenv@Runtime("STATIONS_DB_NAME")(dbName);
    getenv@Runtime("STATIONS_DB_USER")(dbUser);
    getenv@Runtime("STATIONS_DB_PASSWORD")(dbPassword);

    with (connectionInfo) {
        .username = dbUser;
        .password = dbPassword;
        .host = dbHost;
        .port = int(dbPort);
        .database = dbName;
        .driver = "postgresql";
        .toLowerCase = true
    };
    connect@Database(connectionInfo)();

    update@Database(
        "CREATE TABLE IF NOT EXISTS stations (" +
        "id VARCHAR(255) PRIMARY KEY, name VARCHAR(255) NOT NULL, " +
        "lat DOUBLE PRECISION NOT NULL, lon DOUBLE PRECISION NOT NULL)"
    )(schemaResult);
    update@Database(
        "CREATE TABLE IF NOT EXISTS station_sessions (" +
        "session_id VARCHAR(255) PRIMARY KEY, station_id VARCHAR(255) NOT NULL REFERENCES stations(id), " +
        "last_vehicle_id VARCHAR(255))"
    )(schemaResult);

    update@Database(
        "INSERT INTO stations(id,name,lat,lon) VALUES " +
        "('station-1','Stazione Centrale',44.505461,11.341496)," +
        "('station-2','Università',44.496518,11.354375)," +
        "('station-3','Piazza Maggiore',44.493673,11.341753)," +
        "('station-4','Giardini Margherita',44.482744,11.355395)," +
        "('station-5','Fiera',44.509036,11.363389) ON CONFLICT (id) DO NOTHING"
    )(seedResult)
}

main {
    [ listStations(request)(response) {
        query@Database("SELECT id,name,lat,lon FROM stations ORDER BY id")(rows);
        for (i = 0, i < #rows.row, i++) {
            response.stations[i].id = rows.row[i].id;
            response.stations[i].name = rows.row[i].name;
            response.stations[i].lat = rows.row[i].lat;
            response.stations[i].lon = rows.row[i].lon
        }
    }]

    [ getStation(request)(response) {
        query@Database(
            "SELECT id,name,lat,lon FROM stations WHERE id=:id" { .id = request.id }
        )(rows);
        if (#rows.row == 1) {
            response.id = rows.row[0].id;
            response.name = rows.row[0].name;
            response.lat = rows.row[0].lat;
            response.lon = rows.row[0].lon
        }
    }]

    [ openSession(request)(response) {
        response.sessionId = new;
        update@Database(
            "INSERT INTO station_sessions(session_id,station_id) SELECT :session_id,:station_id WHERE EXISTS (SELECT 1 FROM stations WHERE id=:station_id)" {
                .session_id = response.sessionId,
                .station_id = request.stationId
            }
        )(updatedRows)
    }]

    [ unlockVehicle(request)(response) {
        update@Database(
            "UPDATE station_sessions SET last_vehicle_id=:vehicle_id WHERE session_id=:session_id" {
                .vehicle_id = request.vehicleId,
                .session_id = request.sessionId
            }
        )(updatedRows);
        response.success = updatedRows == 1
    }]

    [ lockVehicle(request)(response) {
        update@Database(
            "UPDATE station_sessions SET last_vehicle_id=:vehicle_id WHERE session_id=:session_id" {
                .vehicle_id = request.vehicleId,
                .session_id = request.sessionId
            }
        )(updatedRows);
        response.success = updatedRows == 1
    }]

    [ closeSession(request)(response) {
        update@Database(
            "DELETE FROM station_sessions WHERE session_id=:session_id" {
                .session_id = request.sessionId
            }
        )(updatedRows);
        response.success = updatedRows == 1
    }]
}
