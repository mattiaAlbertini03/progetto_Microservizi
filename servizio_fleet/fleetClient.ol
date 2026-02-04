include "bankInterface.iol"
include "console.iol"

outputPort BankServer { 
    Location: "socket://localhost:8000"
    Protocol: sodep 
	Interfaces: Bank_service 
} 



main {
    println@Console("Login Name: ")()
    request.name = readLine@Console()
    request.value = 4000
    login@BankServer(request)(response)
    login = true
    sid = response.sid
    
    while(login){
        println@Console("C per cauzione, A per addebito, R per rimozione, altro per uscire")()
        value = readLine@Console()
        if (value == "C") {
            println@Console("cauzione : ")()
            dep.value = int ( readLine@Console() )
            dep.sid = sid
            cauzione@BankServer( dep )( token )
            if (!token) {
                println@Console("Errore nella cauzione")()
            }
            else {
                println@Console("Cauzione Avvenuta con sucesso")()
            }
        }
        else if (value == "A") {
            println@Console("addebito : ")()
            dep.value = int ( readLine@Console() )
            dep.sid = sid
            cauzione@BankServer( dep )( token )
            if (!token) {
                println@Console("Errore nell'addebito")()
            }
            else {
                println@Console("Addebito Avvenuto con sucesso")()
            }
        }
        else if (value == "R") {
            println@Console("rimuovi cauzione : ")()
            dep.value = int ( readLine@Console() )
            dep.sid = sid
            cauzione@BankServer( dep )( token )
            if (!token) {
                println@Console("Errore nella rimozione")()
            }
            else {
                println@Console("Rimozione avvenuta con sucesso")()
            }
        }
        else{
            println@Console(" logout ")()
            dep.value = int ( readLine@Console() )
            dep.sid = sid
            cauzione@BankServer( dep )( token )
            if (!token) {
                println@Console("Errore nella cauzione")()
            }
            else {
                println@Console("Cauzione Avvenuta con sucesso")()
            }
        }


    }
}

