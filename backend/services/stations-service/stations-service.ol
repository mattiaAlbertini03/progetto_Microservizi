include "stations-interface.iol"
from file import File

service StationsService {
    embed File as FileService

    inputPort MyInput {
        Location: "socket://0.0.0.0:8080/"
        Protocol: soap {
            .wsdl = "./StationsService.wsdl",
            .wsdl.port = "MyInputServicePort"
        }
        Interfaces: StationSoapService
    }

    inputPort Web {
        Location: "socket://0.0.0.0:8000/"
        Protocol: http {
            .format = "json"
            .osc << {
                listStations << {
                    .template = "/stations"
                    .method = "get"
                }
                getStation << {
                    .template = "/stations/{id}"
                    .method = "get"
                }
            }
            .default = "default"
        }
        Interfaces: StationRestService
    }

    execution { concurrent }

    cset {
        sessionId:
            UnlockVehicleRequest.sessionId
            LockVehicleRequest.sessionId
            CloseSessionRequest.sessionId
    }

    main {
        [ listStations(request)(response) {
            readFile@FileService({
                filename = "./stations.json",
                format = "json"
            })(data);

            for (i = 0, i < #data._, i++) {
                response.stations[i].id = data._[i].id;
                response.stations[i].name = data._[i].name;
                response.stations[i].lat = data._[i].lat;
                response.stations[i].lon = data._[i].lon;
                response.stations[i].capacity = data._[i].capacity
            }
        }]

        [ getStation(request)(response) {
            readFile@FileService({
                filename = "./stations.json",
                format = "json"
            })(data);

            for (i = 0, i < #data._, i++) {
                if (data._[i].id == request.id) {
                    response.id = data._[i].id;
                    response.name = data._[i].name;
                    response.lat = data._[i].lat;
                    response.lon = data._[i].lon;
                    response.capacity = data._[i].capacity
                }
            }
        }]

        [ openSession(request)(response) {
            stationId = request.stationId;
            sessionIsOpen = true;

            response.sessionId = csets.sessionId = new
        }] {
            while (sessionIsOpen) {
                [ unlockVehicle(request)(response) {
                    lastVehicleId = request.vehicleId;
                    response.success = true
                }]

                [ lockVehicle(request)(response) {
                    lastVehicleId = request.vehicleId;
                    response.success = true
                }]

                [ closeSession(request)(response) {
                    sessionIsOpen = false;
                    response.success = true
                }]
            }
        }
    }
}
