# Connectedness and projection analysis

## Course criteria

This analysis follows the calculus and sufficient projection conditions from
the course choreography material. For two consecutive interactions

```text
o: a -> b ; o': c -> d
```

the sequence conditions are:

| Semantics | Sufficient condition |
| --- | --- |
| synchronous | `a=c` or `a=d` or `b=c` or `b=d` |
| asynchronous sender | `a=c` or `b=c` |
| asynchronous receiver | `b=c` or `b=d` |
| asynchronous disjoint | `b=c` |

The condition must be checked across the boundary of every sequential
composition, including loop boundaries. There is no corresponding condition
for parallel composition.

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

### What is well-branched in the implementation

Camunda gives `ACME` concrete ownership of the important runtime decisions:

- it receives QR validation before selecting valid or invalid handling;
- one event-based gateway observes pickup, cancellation, or the deadline;
- it receives Fleet's destination result before accepting or rejecting return;
- it receives `batteryPenaltyApplied` from the charge worker before selecting
  the optional low-battery notification.

Distinct SSE statuses inform the App of observable outcomes. Other services are
called only after `ACME` has selected the branch, so they never decide
independently.

### Where the strict sufficient condition does not hold

The exact implementation choreography is not fully connected under the
course's strict point-of-choice condition:

1. QR success involves Bank, Reservation, Station, Fleet and Worker, while QR
   rejection involves only App, ACME and Logistics. The branch role sets differ.
2. Pickup and cancellation start with an App-to-ACME command, while no-show is
   selected by ACME's timer. Their initial senders differ.
3. Return rejection loops back to App/ACME, while acceptance activates Station,
   Fleet, Reservation, Worker and Bank. The branch role sets differ.
4. `lowBatteryPenaltyNotice: ACME -> APP + 1` has a participant in only one
   branch. As emphasized in the course notes, `1` is not neutral for choice.

These cases are safe in the implemented centralized orchestration because the
branch-only services are passive request/response servers rather than projected
sessions already waiting for a branch-specific message. Nevertheless, the
simple projection correctness theorem cannot be claimed for the unrefined term.

## Strictly connected refinement

For a formal projection exercise, branch selection can be made explicit with
decision interactions sent by the same role to the same roles in every branch.
For example, the battery choice can be refined to:

```text
batteryPenalty: ACME -> APP ; finalChargeReq: ACME -> BANK
+
batteryAccepted: ACME -> APP ; finalChargeReq: ACME -> BANK
```

Both branches have the same initial sender and role set. Similar
`pickupSelected`, `cancelSelected`, `noShowSelected`, `returnAccepted` and
`returnRejected` decision interactions can be broadcast to the roles that must
continue in the projected session. Those extra messages are a formal
coordination refinement; they are not all present in the current REST/SOAP
implementation and are therefore kept separate from the exact traceability
mapping.

## Causality safety and correlation

Request and response operations have distinct names. Repeated interactions are
correlated by `requestId` during creation and by `reservationId` afterward.
BPMN message names distinguish pickup, free cancellation, late cancellation and
return. This prevents concurrent process instances from consuming each other's
messages, assuming one active process instance per correlation key.

## Fleet abstraction

The assignment asks the formal model to treat status requests synchronously.
Accordingly, the choreography exposes:

```text
statusReq: ACME -> FLEET ; statusRes: FLEET -> ACME
```

Kafka, Tracking, Battery and Query are an internal refinement of `FLEET`; they
do not alter the external choreography term.

## Conclusion

The aligned choreography is synchronously sequence-connected and is executable
because `ACME` centrally owns every runtime decision. It is not correct to claim
that the exact term satisfies every strict point-of-choice condition from the
course. The documented decision-message refinement is the version to use when
arguing correctness of the simplest formal projection.
