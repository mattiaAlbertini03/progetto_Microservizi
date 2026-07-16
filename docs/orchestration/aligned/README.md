# Aligned orchestration artifacts

This directory contains a revised formal description aligned with the current ACMEMobility implementation and the V0.9 project specification.

The original artifacts remain unchanged:

- `../choreography.md`
- `../projection.md`
- `../modello_documentativo.bpmn`
- `../processes/process.bpmn`

The files in this directory are additive candidates for the final report:

| File | Purpose |
| --- | --- |
| `choreography.md` | Revised global choreography covering immediate rental, short reservation, cancellations, no-show, monitoring, return, and payment |
| `projections.md` | Behavioral projections for the App, ACMEMobility, Reservation, Logistics, Station, Bank, Fleet, and Charge Worker roles |
| `connectedness.md` | Connectedness and well-branchedness discussion, including the refinements used to make choices observable |
| `traceability.md` | Mapping from choreography interactions to BPMN elements, protocols, endpoints, and source components |

## Abstraction boundary

The formal domain choreography treats Fleet Management as one external role, consistently with the assignment. Its internal simulator, tracking, battery, query, Kafka, and GraphHopper components are an implementation refinement documented in `traceability.md`.

Camunda is considered part of the ACMEMobility role because it realizes ACMEMobility capabilities rather than representing a separate business participant.

## Notation

- `m: P -> Q` is an interaction named `m` sent by `P` and received by `Q`.
- `;` is sequential composition.
- `+` is an exclusive choice.
- `*` is repetition.
- `1` is successful termination.
- Conditions in square brackets identify the guard of a branch.
- `corr` denotes the request or reservation identifier used to correlate REST, BPMN-message, and SSE interactions.

The notation follows the course choreography calculus from `9-choreo.pdf`.
Square-bracketed branch descriptions are explanatory comments rather than
calculus syntax. `connectedness.md` distinguishes synchronous, sender,
receiver, and disjoint sequence connectedness and does not claim the strict
choice theorem where branch role sets differ.

These files describe the successful business behavior and the domain-relevant rejection branches. Connector retries and infrastructure failures remain implementation concerns of the executable BPMN.
