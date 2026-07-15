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
        [ listStations(listRequest)(listResponse) {
            readFile@FileService({
                filename = "./stations.json",
                format = "json"
            })(data);

            for (i = 0, i < #data._, i++) {
                listResponse.stations[i].id = data._[i].id;
                listResponse.stations[i].name = data._[i].name;
                listResponse.stations[i].lat = data._[i].lat;
                listResponse.stations[i].lon = data._[i].lon;
                listResponse.stations[i].capacity = data._[i].capacity
            }
        }]

        [ getStation(getRequest)(getResponse) {
            readFile@FileService({
                filename = "./stations.json",
                format = "json"
            })(data);

            for (i = 0, i < #data._, i++) {
                if (data._[i].id == getRequest.id) {
                    getResponse.id = data._[i].id;
                    getResponse.name = data._[i].name;
                    getResponse.lat = data._[i].lat;
                    getResponse.lon = data._[i].lon;
                    getResponse.capacity = data._[i].capacity
                }
            }
        }]

        [ openSession(openRequest)(openResponse) {
            stationId = openRequest.stationId;
            sessionIsOpen = true;

            openResponse.sessionId = csets.sessionId = new
        }] {
            while (sessionIsOpen) {
                [ unlockVehicle(unlockRequest)(unlockResponse) {
                    lastVehicleId = unlockRequest.vehicleId;
                    unlockResponse.success = true
                }]

                [ lockVehicle(lockRequest)(lockResponse) {
                    lastVehicleId = lockRequest.vehicleId;
                    lockResponse.success = true
                }]

                [ closeSession(closeRequest)(closeResponse) {
                    sessionIsOpen = false;
                    closeResponse.success = true
                }]
            }
        }   
    }
}
