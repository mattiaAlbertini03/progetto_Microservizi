include "console.iol"
include "interface.iol"	
	
inputPort MyInput {
    Location: "socket://localhost:8000" 
    Protocol: soap {
        .wsdl = "./myWsdl.wsdl";
        .wsdl.port = "MyInputServicePort"
    }
    Interfaces: Bank_service 
}

//definita la struttura di ricarica così da non dover essere ripetuta
define ricarica_logic {
    while (value >= conto){
        conto += ricarica
    }
}

//definita la struttura dell'addebito così da non dover essere ripetuta
define addebito_logic {
    value = request.value
    ricarica_logic
    conto -= value
    ris.value = true
}

init {
    isLogin = false
    conto = 0
    ricarica = 0
    cauzione = 0
}

execution{ concurrent }

cset {  sid: OpMessage.sid }

main{
    login( request )( response ){
        username = request.name
        conto = request.value
        ricarica = request.ricarica
        isLogin = true
        response.sid = csets.sid = new
    };
    while( isLogin ){
        [ addebito( request )( ris ) {
            addebito_logic 
        }] { println@Console("addebito")() }
                
        [ rimozione( request )( ris ) {
            ris.value = false
        }] { println@Console("rimozionei non possibile")() }
        
        [ cauzione( request ) ( ris ) {
            value = request.value
            ricarica_logic
            conto -= value
            cauzione += value
            ris.value = true
        }] { 
            // codice eseguito dopo aver eseguito cauzione
            println@Console("cauzione")() 
            rimozione_completata = false;
            while( !rimozione_completata ) {
                [ addebito( request )( ris ) {
                    addebito_logic 
                }] { println@Console("addebito")() }

                [ rimozione( request )( ris ) {
                    value = request.value
                    cauzione -= value
                    conto += value
                    rimozione_completata = true
                    ris.value = true
                }] { println@Console("rimozione")() }
            }
        }
        
        [ logout ( request )( ris ) {
            ris.name = username
            ris.value = int(conto)
			println@Console( "User " + username + " logged out." ) ( )
			isLogin = false 
        }]
	}
}




