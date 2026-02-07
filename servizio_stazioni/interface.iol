
type veicolo: void{
    .batteria: int 
    .status:  string( enum(["paul","homer","mark"]))
}

type OpMessage: void{
    .sid: string
    .value?: int
}

interface Bank_service {
    RequestResponse: login(LoginRequest)(OpMessage), cauzione(OpMessage)(bool), 
                        addebito(OpMessage)(bool), rimozione(OpMessage)(bool),
                        logout(OpMessage)(undefined)
}

