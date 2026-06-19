include "console.iol"
include "interface.iol"	
from file import File

// Wrap everything inside a service block
service StazioneService {
    // Now 'embed' is inside the service
    embed File as FileService

    inputPort MyInput {
        Location: "socket://0.0.0.0:8080/" 
        Protocol: soap {
            .wsdl = "./MyInput.wsdl",
            .wsdl.port = "MyInputServicePort",
            .debug = true,
            .debug.showContent = true
        }
        Interfaces: Stazione_service 
    }

    inputPort Web {
        location: "socket://0.0.0.0:8000/"
        protocol: http {
            format = "json"
            osc << {
                listStations << {
                    template = "/stations"
                    method = "get"
                }
                getStation << {
                    template = "/stations/{id}"
                    method = "get"
                }
            }
            default = "default"
        }
        interfaces: RESTInterface
    }

    init {
        isLogin = false
    }

    execution { concurrent }

    cset {  sid: OpMessage.sid }
    
    main {
        [ listStations( req )( res ) {
            readFile@FileService( {
                filename = "./stations.json",
                format = "json"
            } )( data );

            for( i = 0, i < #data._, i++ ) {
                res.stations[i].id = data._[i].id
                res.stations[i].name = data._[i].name
                res.stations[i].lat = data._[i].lat
                res.stations[i].lon = data._[i].lon
                res.stations[i].capacity = data._[i].capacity
            }
        }]

        [ getStation( req )( res ) {
            readFile@FileService( {
                filename = "./stations.json",
                format = "json"
            } )( data );

            found = false;

            for( i = 0, i < #data._, i++ ) {
                if( data._[i].id == req.id ) {
                    res.id = data._[i].id;
                    res.name = data._[i].name;
                    res.lat = data._[i].lat;
                    res.lon = data._[i].lon;
                    res.capacity = data._[i].capacity;
                    found = true
                }
            }
        }]

        [ login( req )( response ) {
            isLogin = true;
            response.sid = cset.new = new // 'new' creates a new correlation ID
        }]

	    [ sblocco( request ) ( res ) {
		    if(isLogin){
			    stazione = request.sid
		    	auto = request.vehicleId
			    res.value = true
		    }
	    }]

	    [ blocco( request )( res ) {
		    if(isLogin){
			    stazione = request.sid
			    auto = request.vehicleId
			    res.value = true
		    }
	    }]

	    [ logout ( request )( res ) {
		    if(isLogin){
			    isLogin = false
		    }
	    }]
    }
}
