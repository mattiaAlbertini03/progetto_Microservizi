
type LoginRequest: void{
    .name: string
    .conto: int
    .ricarica: int
}

type OpMessage: void{
    .sid: string
    .value?: int
}

type Ris: void{
    .value: bool
}

interface Bank_service {
    RequestResponse: login(LoginRequest)(OpMessage), cauzione(OpMessage)(Ris), 
                        addebito(OpMessage)(Ris), rimozione(OpMessage)(Ris),
                        logout(OpMessage)(LoginRequest)
}

