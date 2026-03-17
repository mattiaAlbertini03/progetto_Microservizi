type EmptyMessage: void{
    x?: string
}

type OpMessage: void{
    .sid: string
    .vehicleId?: string
}

type Ris: void{
    .value: bool
}

interface Stazione_service {
    RequestResponse: login(EmptyMessage)(OpMessage), 
                        sblocco(OpMessage)(Ris), blocco(OpMessage)(Ris),
                        logout(OpMessage)(EmptyMessage)
}

