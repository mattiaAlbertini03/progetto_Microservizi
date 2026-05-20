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

    init {
        isLogin = false
    }

    execution { concurrent }

cset {  sid: OpMessage.sid }
    
main {
        [ stations( req )( res ) {
            readFile@FileService( {
                filename = "../acme-mobility-service/src/main/resources/stations.json"
            } )( fileContent );
            res.value = fileContent
        }]

        [ login( req )( response ) {
            isLogin = true;
            response.sid = csed.new = new // 'new' creates a new correlation ID
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
