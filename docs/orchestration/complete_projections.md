```text
S ::= APP | ACME | RES | LOG | STA | BANK | FLEET | WORKER

APP ::= (
    !catalogueReq@ACME ; ?catalogueRes@ACME
)* ; (
    QR_APP + RESERVATION_APP
)*

QR_APP ::=
    !qrRentalReq@ACME ;
    (
        ?reservationAck@ACME ; RENTAL_APP
      +
        ?qrRejected@ACME
    )

RESERVATION_APP ::=
    !reservationReq@ACME ;
    ?reservationAck@ACME ;
    ?reservationConfirmed@ACME ;
    (
        !pickupReq@ACME ; ?pickupAck@ACME ; RENTAL_APP
      +
        !cancelReq@ACME ; ?cancelAck@ACME
      +
        1
    )

RENTAL_APP ::=
    ?depositHeldNotice@ACME ; ACTIVE_APP

ACTIVE_APP ::=
    !returnReq@ACME ;
    ?returnCommandAck@ACME ;
    (
        ?returnRejected@ACME ; ACTIVE_APP
      +
        ?returnAccepted@ACME ;
        (?lowBatteryPenaltyNotice@ACME + 1) ;
        ?depositReleasedNotice@ACME ;
        ?returnConfirmed@ACME
    )

ACME ::= (
    ?catalogueReq@APP ; !catalogueRes@APP
)* ; (
    QR_ACME + RESERVATION_ACME
)*

QR_ACME ::=
    ?qrRentalReq@APP ;
    !qrValidationReq@LOG ; ?qrValidationRes@LOG ;
    (
        [valid]
        !reservationAck@APP ;
        BANK_SESSION_ACME ;
        CREATE_ACTIVATE_ACME ;
        START_ACME ; ACTIVE_ACME
      +
        [invalid]
        !qrRejected@APP
    )

RESERVATION_ACME ::=
    ?reservationReq@APP ; !reservationAck@APP ;
    BANK_SESSION_ACME ;
    !createReq@RES ; ?createRes@RES ;
    !holdReq@RES ; ?holdRes@RES ;
    !reservationConfirmed@APP ;
    (
        [pickup message before deadline]
        ?pickupReq@APP ; !pickupAck@APP ;
        !activateReq@RES ; ?activateRes@RES ;
        START_ACME ; ACTIVE_ACME
      +
        [free cancellation message]
        ?cancelReq@APP ; !cancelAck@APP ; CANCEL_ACME
      +
        [late cancellation message]
        ?cancelReq@APP ; !cancelAck@APP ; PENALTY_ACME ; CANCEL_ACME
      +
        [pickup deadline timer]
        PENALTY_ACME ; CANCEL_ACME
    )

BANK_SESSION_ACME ::=
    !bankLoginReq@BANK ; ?bankLoginRes@BANK

CREATE_ACTIVATE_ACME ::=
    !createReq@RES ; ?createRes@RES ;
    !activateReq@RES ; ?activateRes@RES

PENALTY_ACME ::=
    !penaltyReq@BANK ; ?penaltyRes@BANK

CANCEL_ACME ::=
    !cancelReservationReq@RES ; ?cancelReservationRes@RES

START_ACME ::=
    !openStationReq@STA ; ?openStationRes@STA ;
    !unlockReq@STA ; ?unlockRes@STA ;
    !depositReq@BANK ; ?depositRes@BANK ;
    !depositHeldNotice@APP ;
    !monitorStartReq@FLEET ; ?monitorStartRes@FLEET

ACTIVE_ACME ::=
    (!statusReq@FLEET ; ?statusRes@FLEET)* ;
    ?returnReq@APP ;
    !returnCommandAck@APP ;
    !destinationReq@FLEET ; ?destinationRes@FLEET ;
    (
        [outside destination]
        !returnRejected@APP ; ACTIVE_ACME
      +
        [at destination]
        !returnAccepted@APP ; FINISH_ACME
    )

FINISH_ACME ::=
    !lockReq@STA ; ?lockRes@STA ;
    !closeStationReq@STA ; ?closeStationRes@STA ;
    !monitorStopReq@FLEET ; ?tripSummaryRes@FLEET ;
    !completeReq@RES ; ?completeRes@RES ;
    !calculateReq@WORKER ; ?calculateRes@WORKER ;
    (!lowBatteryPenaltyNotice@APP + 1) ;
    !finalChargeReq@BANK ; ?finalChargeRes@BANK ;
    !depositReleaseReq@BANK ; ?depositReleaseRes@BANK ;
    !depositReleasedNotice@APP ;
    !returnConfirmed@APP ;
    !bankLogoutReq@BANK ; ?bankLogoutRes@BANK

RES ::= (
    ?createReq@ACME ; !createRes@ACME ;
    (
        ?holdReq@ACME ;
        !vehicleHoldReq@LOG ; ?vehicleHoldRes@LOG ;
        !holdRes@ACME ;
        (
            ?activateReq@ACME ;
            !vehiclePickupReq@LOG ; ?vehiclePickupRes@LOG ;
            !activateRes@ACME ; COMPLETE_OR_ACTIVE
          +
            ?cancelReservationReq@ACME ;
            !vehicleReleaseReq@LOG ; ?vehicleReleaseRes@LOG ;
            !cancelReservationRes@ACME
        )
      +
        ?activateReq@ACME ;
        !vehicleHoldReq@LOG ; ?vehicleHoldRes@LOG ;
        !vehiclePickupReq@LOG ; ?vehiclePickupRes@LOG ;
        !activateRes@ACME ; COMPLETE_OR_ACTIVE
    )
)*

COMPLETE_OR_ACTIVE ::=
    ?completeReq@ACME ;
    !vehicleReturnReq@LOG ; ?vehicleReturnRes@LOG ;
    !completeRes@ACME

LOG ::= (
    ?qrValidationReq@ACME ; !qrValidationRes@ACME
  +
    ?vehicleHoldReq@RES ; !vehicleHoldRes@RES
  +
    ?vehiclePickupReq@RES ; !vehiclePickupRes@RES
  +
    ?vehicleReleaseReq@RES ; !vehicleReleaseRes@RES
  +
    ?vehicleReturnReq@RES ; !vehicleReturnRes@RES
)*


STA ::= (
    ?openStationReq@ACME ; !openStationRes@ACME ;
    ?unlockReq@ACME ; !unlockRes@ACME ;
    ?lockReq@ACME ; !lockRes@ACME ;
    ?closeStationReq@ACME ; !closeStationRes@ACME
)*


BANK ::= (
    ?bankLoginReq@ACME ; !bankLoginRes@ACME ;
    (
        ?penaltyReq@ACME ; !penaltyRes@ACME
      +
        ?depositReq@ACME ; !depositRes@ACME ;
        ?finalChargeReq@ACME ; !finalChargeRes@ACME ;
        ?depositReleaseReq@ACME ; !depositReleaseRes@ACME ;
        ?bankLogoutReq@ACME ; !bankLogoutRes@ACME
    )
)*


FLEET ::= (
    ?monitorStartReq@ACME ; !monitorStartRes@ACME ;
    (?statusReq@ACME ; !statusRes@ACME)* ;
    (?destinationReq@ACME ; !destinationRes@ACME)* ;
    ?monitorStopReq@ACME ; !tripSummaryRes@ACME
)*


WORKER ::= (
    ?calculateReq@ACME ;
    !calculateRes@ACME
)*
```
