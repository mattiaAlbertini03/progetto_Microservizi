# Proiezione

Si usa `!` per indicare un output:

```text
!op@ROLE    output verso ROLE
op@ROLE     input proveniente da ROLE
```

La proiezione sostituisce inoltre con `1` le interazioni alle quali il ruolo
non partecipa ed è omomorfa rispetto agli operatori della coreografia.

## APP

```text
APP :== ((
    !online_req@ACME;
    online_res@ACME;

    (
        !pickup_req@ACME;
        pickup_res@ACME;

        !return_req@ACME;
        return_res@ACME
    )

    +

    (
        !cancel_req@ACME;
        cancel_res@ACME
    )
)

+
(
    !qr_req@ACME;
    qr_res@ACME;

    !return_req@ACME;
    return_res@ACME
))*
```

## ACME

```text
ACME :== ((
    online_req@APP;
    !online_res@APP;

    (
        pickup_req@APP;
        !pickup_res@APP;

        !unlock_req@STATIONS;
        unlock_res@STATIONS;

        !bail_req@BANK;
        bail_res@BANK;

        !start_req@FLEET;
        start_res@FLEET;

        (
            !track_req@FLEET;
            track_res@FLEET
        )*;

        return_req@APP;
        !return_res@APP;

        !lock_req@STATIONS;
        lock_res@STATIONS;

        !stop_req@FLEET;
        stop_res@FLEET;

        !charge_req@BANK;
        charge_res@BANK;

        !unbail_req@BANK;
        unbail_res@BANK
    )

    +

    (
        cancel_req@APP;
        !cancel_res@APP;

        (
            !bail_req@BANK;
            bail_res@BANK
        )

        +

        1
    )
)

+
(
    qr_req@APP;
    !qr_res@APP;

    !unlock_req@STATIONS;
    unlock_res@STATIONS;

    !bail_req@BANK;
    bail_res@BANK;

    !start_req@FLEET;
    start_res@FLEET;

    (
        !track_req@FLEET;
        track_res@FLEET
    )*;

    return_req@APP;
    !return_res@APP;

    !lock_req@STATIONS;
    lock_res@STATIONS;

    !stop_req@FLEET;
    stop_res@FLEET;

    !charge_req@BANK;
    charge_res@BANK;

    !unbail_req@BANK;
    unbail_res@BANK
))*
```

## STATIONS

```text
STATIONS :== ((
    (
        unlock_req@ACME;
        !unlock_res@ACME;

        lock_req@ACME;
        !lock_res@ACME
    )

    +

    1
)

+
(
    unlock_req@ACME;
    !unlock_res@ACME;

    lock_req@ACME;
    !lock_res@ACME
))*
```

## BANK

```text
BANK :== ((
    (
        bail_req@ACME;
        !bail_res@ACME;

        charge_req@ACME;
        !charge_res@ACME;

        unbail_req@ACME;
        !unbail_res@ACME
    )

    +

    (
        (
            bail_req@ACME;
            !bail_res@ACME
        )

        +

        1
    )
)

+
(
    bail_req@ACME;
    !bail_res@ACME;

    charge_req@ACME;
    !charge_res@ACME;

    unbail_req@ACME;
    !unbail_res@ACME
))*
```

## FLEET

```text
FLEET :== ((
    (
        start_req@ACME;
        !start_res@ACME;

        (
            track_req@ACME;
            !track_res@ACME
        )*;

        stop_req@ACME;
        !stop_res@ACME
    )

    +

    1
)

+
(
    start_req@ACME;
    !start_res@ACME;

    (
        track_req@ACME;
        !track_res@ACME
    )*;

    stop_req@ACME;
    !stop_res@ACME
))*
```

## Sistema proiettato

```text
SYSTEM :==
    (APP)_APP
    |
    (ACME)_ACME
    |
    (STATIONS)_STATIONS
    |
    (BANK)_BANK
    |
    (FLEET)_FLEET
```
