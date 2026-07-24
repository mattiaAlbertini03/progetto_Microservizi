# Connectedness and projection analysis

## criteria

This analysis follows the calculus and sufficient projection conditions, 
for two consecutive interactions

```text
o: a -> b ; o': c -> d
```

the sequence conditions are:

| Semantics | Sufficient condition |
| synchronous | `a=c` or `a=d` or `b=c` or `b=d` |
| sender | `a=c` or `b=c` |
| receiver | `b=c` or `b=d` |
| disjoint | `b=c` |

For a choice, synchronous projection requires a common role in the initial
interactions and the same set of participating roles in every branch.
Asynchronous projection additionally requires the same initial sender. These
are sufficient conditions, not necessary ones: failure means that correctness
is no longer guaranteed by the simple projection theorem, not that every
implementation must deadlock.

Causality safety must also be checked when the same operation between the same
participants can be enabled more than once. Distinct operation names and
correlation identifiers prevent one input from consuming a message belonging
to another interaction.

## Sequential composition

The implementation-aligned choreography is synchronously connected. Its
request/response chains normally satisfy even the strongest disjoint condition:

```text
bankLoginReq: ACME -> BANK ; bankLoginRes: BANK -> ACME ;
createReq: ACME -> RES
```

The receiver of each request sends the response, and `ACME`, which receives the
response, sends the next request. Reservation-to-Logistics chains have the same
shape with `RES` as their pivot.

The monitoring iteration is also connected:

```text
(statusReq: ACME -> FLEET ; statusRes: FLEET -> ACME)*
```

Both the body and successive iterations share `ACME` and `FLEET`. The return
rejection branch ends with `ACME -> APP` and the next return attempt begins with
`APP -> ACME`, satisfying the disjoint sequence condition at that boundary.

One deliberate exception to stronger asynchronous conditions is:

```text
returnConfirmed: ACME -> APP ; bankLogoutReq: ACME -> BANK
```

It is synchronously and sender-connected because `ACME` participates in and
sends both interactions, but it is not receiver- or disjoint-connected. Moving
bank logout before the final App notification would satisfy the disjoint rule,
but would no longer describe the current executable BPMN order.

## Choices
### Where the strict sufficient condition does not hold

The exact implementation choreography is not fully connected:

1. QR success involves Bank, Reservation, Station, Fleet and Worker, while QR
   rejection involves only App, ACME and Logistics. The branch role sets differ.
2. Pickup and cancellation start with an App-to-ACME command, while no-show is
   selected by ACME's timer. Their initial senders differ.
3. Return rejection loops back to App/ACME, while acceptance activates Station,
   Fleet, Reservation, Worker and Bank. The branch role sets differ.
4. `lowBatteryPenaltyNotice: ACME -> APP + 1` has a participant in only one
   branch. `1` is not neutral for choice.

These cases are safe in the implemented centralized orchestration because the
branch-only services are passive request/response servers rather than projected
sessions already waiting for a branch-specific message. Nevertheless, the
simple projection correctness theorem cannot be claimed for the unrefined term.

## Causality safety and correlation

Request and response operations have distinct names. Repeated interactions are
correlated by `requestId` during creation and by `reservationId` afterward.
This prevents concurrent process instances from consuming each other's
messages, assuming one active process instance per correlation key.

## Conclusion

The aligned choreography is synchronously sequence-connected and is executable
because `ACME` centrally owns every runtime decision. The documented decision-message 
refinement is the version to use when arguing correctness of the simplest formal projection.
