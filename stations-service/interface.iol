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

type GetStationRequest: void {
    .id: string
}

type Station: void {
    .id: string
    .name: string
    .lat: double
    .lon: double
    .capacity: int
}

type StationsResponse: void {
    .stations*: void {
        .id: string
        .name: string
        .lat: double
        .lon: double
        .capacity: int
    }
}


interface Stazione_service {
    RequestResponse: login(EmptyMessage)(OpMessage), 
                        sblocco(OpMessage)(Ris), blocco(OpMessage)(Ris),
                        logout(OpMessage)(EmptyMessage)
}

interface RESTInterface {
    RequestResponse: listStations(EmptyMessage)(StationsResponse), 
                    getStation( GetStationRequest )( Station )
}