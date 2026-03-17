include "console.iol"
include "interface.iol"	

inputPort MyInput {
    Location: "socket://localhost:8001/" 
    Protocol: soap {
        .wsdl = "./MyInput.wsdl";
        .wsdl.port = "MyInputServicePort"
    }
    Interfaces: Stazione_service 
}

init {
    isLogin = false
}

execution{ concurrent }

cset {  sid: OpMessage.sid }

main{
    login( req )( response ){
        isLogin = true
        response.sid = csets.sid = new
    };
    while( isLogin ){
        [ sblocco( request ) ( res ) {
            stazione = request.sid
            auto = request.vehicleId
            res.value = true
        }] { println@Console("sblocco")() }
        
        [ blocco( request )( res ) {
            stazione = request.sid
            auto = request.vehicleId
            res.value = true
        }] { println@Console("blocco")() }
        
        [ logout ( request )( res ) {
			isLogin = false 
        }]
	}
}


