include "console.iol"
include "bankInterface.iol"	
	
inputPort MyInput {
    Location: "socket://localhost:8000" 
    Protocol: http {
        .format = "json";
        
        .osc.login.method = "get";
        .osc.login.template = "/banca/login/{name}/{value}";   
        .osc.cauzione.method = "get";
        .osc.cauzione.template = "/banca/cauzione/{sid}/{value}";
        .osc.addebito.method = "get";
        .osc.addebito.template = "/banca/addebito/{sid}/{value}";
        .osc.rimozione.method = "get";
        .osc.rimozione.template = "/banca/rimozione/{sid}/{value}";
        .osc.logout.method = "get";
        .osc.logout.template = "/banca/logout/{sid}"
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
            ris = false
            if (value < conto) {
                ris = true
                conto -= value
                cauzione += value
            }
        }] { println@Console("cauzione")() }
        
        [ addebito( request )( ris ) {
            value = request.value
            ris = false
            if (value < conto) {
                ris = true
                conto -= value
            }
        }] { println@Console("addebito")() }
        
        [ rimozione( request )( ris ) {
            value = request.value
            ris = false
            if (value < cauzione){
                ris = true
                cauzione -= value
                conto += value
            }
        }] { println@Console("rimozione")() }
        
        [ logout ( request )(null) { 
			println@Console( "User " + username + " logged out." ) ( )
			isLogin = false 
        }]
	}
}


