```text
C ::= (CATALOGUE)* ; (QR_RENTAL + SHORT_RESERVATION)*

CATALOGUE ::=
    catalogueReq: APP -> ACME ;
    catalogueRes: ACME -> APP

BANK_SESSION ::=
    bankLoginReq: ACME -> BANK ;
    bankLoginRes: BANK -> ACME

QR_RENTAL ::=
    qrRentalReq: APP -> ACME ;
    qrValidationReq: ACME -> LOG ;
    qrValidationRes: LOG -> ACME ;
    (
        [QR valid]
        reservationAck: ACME -> APP ;
        BANK_SESSION ;
        CREATE_AND_ACTIVATE ;
        START_RENTAL ;
        ACTIVE_RENTAL
      +
        [QR invalid]
        qrRejected: ACME -> APP
    )

SHORT_RESERVATION ::=
    reservationReq: APP -> ACME ;
    reservationAck: ACME -> APP ;
    BANK_SESSION ;
    createReq: ACME -> RES ;
    createRes: RES -> ACME ;
    holdReq: ACME -> RES ;
    vehicleHoldReq: RES -> LOG ;
    vehicleHoldRes: LOG -> RES ;
    holdRes: RES -> ACME ;
    reservationConfirmed: ACME -> APP ;
    RESERVATION_WAIT

CREATE_AND_ACTIVATE ::=
    createReq: ACME -> RES ;
    createRes: RES -> ACME ;
    activateReq: ACME -> RES ;
    vehicleHoldReq: RES -> LOG ;
    vehicleHoldRes: LOG -> RES ;
    vehiclePickupReq: RES -> LOG ;
    vehiclePickupRes: LOG -> RES ;
    activateRes: RES -> ACME

RESERVATION_WAIT ::=
    (
        [pickup before deadline]
        pickupReq: APP -> ACME ;
        pickupAck: ACME -> APP ;
        [wait until scheduledStart if pickup was requested early]
        activateReq: ACME -> RES ;
        vehiclePickupReq: RES -> LOG ;
        vehiclePickupRes: LOG -> RES ;
        activateRes: RES -> ACME ;
        START_RENTAL ;
        ACTIVE_RENTAL
      +
        [cancellation no later than start - 5 minutes]
        cancelReq: APP -> ACME ;
        cancelAck: ACME -> APP ;
        CANCEL_RESERVATION
      +
        [cancellation after start - 5 minutes]
        cancelReq: APP -> ACME ;
        cancelAck: ACME -> APP ;
        PENALTY ;
        CANCEL_RESERVATION
      +
        [no pickup at start + 5 minutes]
        PENALTY ;
        CANCEL_RESERVATION
    )

PENALTY ::=
    penaltyReq: ACME -> BANK ;
    penaltyRes: BANK -> ACME

CANCEL_RESERVATION ::=
    cancelReservationReq: ACME -> RES ;
    vehicleReleaseReq: RES -> LOG ;
    vehicleReleaseRes: LOG -> RES ;
    cancelReservationRes: RES -> ACME

START_RENTAL ::=
    openStationReq: ACME -> STA ;
    openStationRes: STA -> ACME ;
    unlockReq: ACME -> STA ;
    unlockRes: STA -> ACME ;
    depositReq: ACME -> BANK ;
    depositRes: BANK -> ACME ;
    depositHeldNotice: ACME -> APP ;
    monitorStartReq: ACME -> FLEET ;
    monitorStartRes: FLEET -> ACME

ACTIVE_RENTAL ::=
    (statusReq: ACME -> FLEET ; statusRes: FLEET -> ACME)* ;
    returnReq: APP -> ACME ;
    returnCommandAck: ACME -> APP ;
    destinationReq: ACME -> FLEET ;
    destinationRes: FLEET -> ACME ;
    (
        [vehicle outside destination]
        returnRejected: ACME -> APP ;
        ACTIVE_RENTAL
      +
        [vehicle at destination]
        returnAccepted: ACME -> APP ;
        FINISH_RENTAL
    )

FINISH_RENTAL ::=
    lockReq: ACME -> STA ;
    lockRes: STA -> ACME ;
    closeStationReq: ACME -> STA ;
    closeStationRes: STA -> ACME ;
    monitorStopReq: ACME -> FLEET ;
    tripSummaryRes: FLEET -> ACME ;
    completeReq: ACME -> RES ;
    vehicleReturnReq: RES -> LOG ;
    vehicleReturnRes: LOG -> RES ;
    completeRes: RES -> ACME ;
    calculateReq: ACME -> WORKER ;
    calculateRes: WORKER -> ACME ;
    OPTIONAL_LOW_BATTERY_NOTICE ;
    finalChargeReq: ACME -> BANK ;
    finalChargeRes: BANK -> ACME ;
    depositReleaseReq: ACME -> BANK ;
    depositReleaseRes: BANK -> ACME ;
    depositReleasedNotice: ACME -> APP ;
    returnConfirmed: ACME -> APP ;
    bankLogoutReq: ACME -> BANK ;
    bankLogoutRes: BANK -> ACME

OPTIONAL_LOW_BATTERY_NOTICE ::=
    lowBatteryPenaltyNotice: ACME -> APP
  + 1
```
