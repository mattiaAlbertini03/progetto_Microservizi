# ACMEMobility aligned global choreography

## Formal basis

This document uses the choreography calculus: 

```text
C ::= o: a -> b      interaction
    | C ; C          sequential composition
    | C | C          parallel composition
    | C + C          nondeterministic choice
    | C*             iteration, zero or more times
    | 1              successful termination
```
Labels in square brackets below are comments that relate a branch to the domain; 
they are not additional calculus operators.

The calculus specifies communication structure, not payload values or the local
conditions used to choose a branch. 

## Participants

| Symbol | Participant |
| --- | --- |
| `APP` | Customer application |
| `ACME` | ACMEMobility public API and Camunda orchestration |
| `RES` | Reservation service |
| `LOG` | Logistics service |
| `STA` | Jolie station service |
| `BANK` | Jolie banking service |
| `FLEET` | Fleet Management external role |
| `WORKER` | ACMEMobility charge-calculation worker |

Fleet's internal microservices are deliberately hidden at this level. 

## Interaction catalogue

| Interaction | Sender -> receiver | Meaning |
| --- | --- | --- |
| `catalogueReq/catalogueRes` | `APP <-> ACME` | Stations and nearby vehicles |
| `qrRentalReq/qrValidation` | `APP -> ACME`, `ACME <-> LOG` | Immediate-rental request and QR validation |
| `reservationReq/reservationAck` | `APP <-> ACME` | Scheduled reservation request and asynchronous acknowledgement |
| `bankLoginReq/bankLoginRes` | `ACME <-> BANK` | Open the banking session used by the process |
| `createReq/createRes` | `ACME <-> RES` | Create a pending reservation |
| `holdReq/holdRes` | `ACME <-> RES` | Hold the selected vehicle |
| `vehicleHoldReq/vehicleHoldRes` | `RES <-> LOG` | Change the vehicle to `ON_HOLD` |
| `reservationConfirmed` | `ACME -> APP` | SSE reservation confirmation |
| `pickupReq/pickupAck` | `APP <-> ACME` | Customer pickup command |
| `activateReq/activateRes` | `ACME <-> RES` | Activate the reservation |
| `vehiclePickupReq/vehiclePickupRes` | `RES <-> LOG` | Change the vehicle to in-rental state |
| `openStationReq/openStationRes` | `ACME <-> STA` | Open a correlated station session |
| `unlockReq/unlockRes` | `ACME <-> STA` | Physically unlock the vehicle |
| `depositReq/depositRes` | `ACME <-> BANK` | Hold the €10 deposit |
| `depositHeldNotice` | `ACME -> APP` | Confirm that the €10 deposit was held |
| `monitorStartReq/monitorStartRes` | `ACME <-> FLEET` | Start trip simulation and monitoring |
| `statusReq/statusRes` | `ACME <-> FLEET` | Periodic vehicle position and battery query |
| `cancelReq/cancelAck` | `APP <-> ACME` | Customer cancellation command |
| `penaltyReq/penaltyRes` | `ACME <-> BANK` | Charge €10 for late cancellation or no-show |
| `cancelReservationReq/cancelReservationRes` | `ACME <-> RES` | Cancel the reservation |
| `vehicleReleaseReq/vehicleReleaseRes` | `RES <-> LOG` | Release a held vehicle |
| `returnReq/returnCommandAck` | `APP <-> ACME` | Customer return command and immediate HTTP acknowledgement |
| `destinationReq/destinationRes` | `ACME <-> FLEET` | Verify proximity to the destination station |
| `returnRejected/returnAccepted` | `ACME -> APP` | SSE notification carrying the destination decision |
| `lockReq/lockRes` | `ACME <-> STA` | Physically lock the vehicle |
| `closeStationReq/closeStationRes` | `ACME <-> STA` | Close the station session |
| `monitorStopReq/tripSummaryRes` | `ACME <-> FLEET` | Stop monitoring and return minutes, kilometres, and battery |
| `completeReq/completeRes` | `ACME <-> RES` | Complete the reservation |
| `vehicleReturnReq/vehicleReturnRes` | `RES <-> LOG` | Return the vehicle to Logistics |
| `calculateReq/calculateRes` | `ACME <-> WORKER` | Calculate base and final amounts |
| `lowBatteryPenaltyNotice` | `ACME -> APP` | Report the optional 10% battery penalty |
| `finalChargeReq/finalChargeRes` | `ACME <-> BANK` | Charge the final rental amount |
| `depositReleaseReq/depositReleaseRes` | `ACME <-> BANK` | Release the €10 deposit |
| `depositReleasedNotice` | `ACME -> APP` | Confirm that the €10 deposit was released |
| `returnConfirmed` | `ACME -> APP` | SSE completion notification |
| `bankLogoutReq/bankLogoutRes` | `ACME <-> BANK` | Close the bank session after successful completion |

## Global choreography

```text
SYSTEM ::= (CATALOGUE)* ; (QR_RENTAL + SHORT_RESERVATION)*

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

## Fleet implementation refinement

The formal interaction pair:

```text
monitorStartReq: ACME -> FLEET ; monitorStartRes: FLEET -> ACME
```

is implemented by Fleet starting the simulator, which sends tracking and battery updates through dedicated services and Kafka. Similarly, `tripSummaryRes` aggregates the simulator's authoritative stop result. This refinement is intentionally hidden from the domain choreography, as permitted by the assignment's asynchronous-monitoring alternative.
