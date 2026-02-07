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

init {
    isLogin = false
    conto = 0
    cauzione = 0
}

execution{ concurrent }

cset {  sid: OpMessage.sid }

main{
    login( request )( response ){
        username = request.name
        conto = request.value
        isLogin = true
        response.sid = csets.sid = new
    };
    while( isLogin ){
        [ cauzione( request ) ( ris ) {
            value = request.value
            ris.value = false
            if (value < conto) {
                ris.value = true
                conto -= value
                cauzione += value
            }
        }] { println@Console("cauzione")() }
        
        [ addebito( request )( ris ) {
            value = request.value
            ris.value = false
            if (value < conto) {
                ris.value = true
                conto -= value
            }
        }] { println@Console("addebito")() }
        
        [ rimozione( request )( ris ) {
            value = request.value
            ris.value = false
            if (value < cauzione){
                ris.value = true
                cauzione -= value
                conto += value
            }
        }] { println@Console("rimozione")() }
        
        [ logout ( request )( ris ) {
            ris.name = username
            ris.value = int(conto)
			println@Console( "User " + username + " logged out." ) ( )
			isLogin = false 
        }]
	}
}


