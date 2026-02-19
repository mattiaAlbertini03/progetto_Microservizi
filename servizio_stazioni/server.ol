include "console.iol"
include "file.iol"
include "interface.iol"	

//dato che deve simulare la gestione manuale non posso generare un errore dat dai dati
//il controllo se il veicolo si trova in stazione è fatto in ACME


inputPort MyInput {
    Location: "socket://localhost:8001" 
    Protocol: soap {
        .wsdl = "./myWsdl.wsdl";
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
        [ sblocco( request ) ( ris ) {
            stazione = request.sid
            auto = request.auto
            ris.value = true
        }] { println@Console("sblocco")() }
        
        [ blocco( request )( ris ) {
            stazione = request.sid
            auto = request.auto
            ris.value = true
        }] { println@Console("blocco")() }
        
        [ logout ( request )( res ) {
			isLogin = false 
        }]
	}
}


