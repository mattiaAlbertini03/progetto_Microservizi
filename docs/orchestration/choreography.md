# Coreografia

La coreografia descrive due modalità di avvio della sessione di mobilità:
la prenotazione online (`ONLINE`) e il ritiro immediato tramite codice QR
(`QR`). In entrambi i casi, `ACME` coordina le interazioni con l'applicazione
utente e con i servizi `STATIONS`, `BANK` e `FLEET`.

```text
ONLINE :== (
    online_req: APP -> ACME;
    online_res: ACME -> APP;

    (
        pickup_req: APP -> ACME;
        pickup_res: ACME -> APP;

        unlock_req: ACME -> STATIONS;
        unlock_res: STATIONS -> ACME;

        bail_req: ACME -> BANK;
        bail_res: BANK -> ACME;

        start_req: ACME -> FLEET;
        start_res: FLEET -> ACME;

        (
            track_req: ACME -> FLEET;
            track_res: FLEET -> ACME
        )*;

        return_req: APP -> ACME;
        return_res: ACME -> APP;

        lock_req: ACME -> STATIONS;
        lock_res: STATIONS -> ACME;

        stop_req: ACME -> FLEET;
        stop_res: FLEET -> ACME;

        charge_req: ACME -> BANK;
        charge_res: BANK -> ACME;

        unbail_req: ACME -> BANK;
        unbail_res: BANK -> ACME
    )

    +

    (
        cancel_req: APP -> ACME;
        cancel_res: ACME -> APP;

        (
            bail_req: ACME -> BANK;
            bail_res: BANK -> ACME
        )

        +

        1
    )
)

QR :== (
    qr_req: APP -> ACME;
    qr_res: ACME -> APP;

    unlock_req: ACME -> STATIONS;
    unlock_res: STATIONS -> ACME;

    bail_req: ACME -> BANK;
    bail_res: BANK -> ACME;

    start_req: ACME -> FLEET;
    start_res: FLEET -> ACME;

    (
        track_req: ACME -> FLEET;
        track_res: FLEET -> ACME
    )*;

    return_req: APP -> ACME;
    return_res: ACME -> APP;

    lock_req: ACME -> STATIONS;
    lock_res: STATIONS -> ACME;

    stop_req: ACME -> FLEET;
    stop_res: FLEET -> ACME;

    charge_req: ACME -> BANK;
    charge_res: BANK -> ACME;

    unbail_req: ACME -> BANK;
    unbail_res: BANK -> ACME
)

MOBILITY :== (ONLINE + QR)*
```
