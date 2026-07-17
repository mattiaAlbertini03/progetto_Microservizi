type OpenSessionRequest: void {
    .stationId: string
}

type OpenSessionResponse: void {
    .sessionId: string
}

type UnlockVehicleRequest: void {
    .sessionId: string
    .vehicleId: string
}

type UnlockVehicleResponse: void {
    .success: bool
}

type LockVehicleRequest: void {
    .sessionId: string
    .vehicleId: string
}

type LockVehicleResponse: void {
    .success: bool
}

type CloseSessionRequest: void {
    .sessionId: string
}

type CloseSessionResponse: void {
    .success: bool
}

type EmptyRequest: void

type GetStationRequest: void {
    .id: string
}

type Station: void {
    .id: string
    .name: string
    .lat: double
    .lon: double
}

type StationsResponse: void {
    .stations*: Station
}

interface StationSoapService {
    RequestResponse:
        openSession(OpenSessionRequest)(OpenSessionResponse),
        unlockVehicle(UnlockVehicleRequest)(UnlockVehicleResponse),
        lockVehicle(LockVehicleRequest)(LockVehicleResponse),
        closeSession(CloseSessionRequest)(CloseSessionResponse)
}

interface StationRestService {
    RequestResponse:
        stations(EmptyRequest)(StationsResponse),
        listStations(EmptyRequest)(StationsResponse),
        getStation(GetStationRequest)(Station)
}
