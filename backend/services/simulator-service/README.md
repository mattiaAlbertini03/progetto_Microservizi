# Vehicle Simulator Service

## Overview
The Vehicle Simulator Service simulates the movement of vehicles between two geographic points.

Given a start and destination, the service:
- retrieves a route from GraphHopper
- simulates movement along the route over time
- periodically emits:
  - tracking events (position)
  - battery events

Events are published to Kafka and consumed by the Query Service.

---

## Architecture

Client → Vehicle Simulator → Kafka → Query Service → Frontend

### Responsibilities
- Route calculation via GraphHopper
- Simulation lifecycle management
- Event production to Kafka

---

## API

### Start Simulation

POST /simulations/start

#### Request Body

```json
{
  "vehicleId": "V1",
  "fromStationId": "S1",
  "toStationId": "S2",
  "fromLat": 44.4949,
  "fromLon": 11.3426,
  "toLat": 44.5050,
  "toLon": 11.3500,
  "speedMetersPerSecond": 8.0,
  "initialBatteryPct": 90
}
```

---

## Running Locally

Build and run:

```bash
mvn clean package
java -jar target/vehicle-simulator-service.jar
```

Start a simulation:

```bash
curl -X POST http://localhost:8080/simulations/start \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleId": "V1",
    "fromStationId": "S1",
    "toStationId": "S2",
    "fromLat": 44.4949,
    "fromLon": 11.3426,
    "toLat": 44.5050,
    "toLon": 11.3500,
    "speedMetersPerSecond": 8.0,
    "initialBatteryPct": 90
  }'
```

---

## Running on Kubernetes

Port-forward:

```bash
kubectl port-forward -n acme-fleet svc/vehicle-simulator-service 8080:8080
```

Start simulation:

```bash
curl -X POST http://localhost:8080/simulations/start -H "Content-Type: application/json" -d '{
    "vehicleId": "V1",
    "fromStationId": "S1",
    "toStationId": "S2",
    "fromLat": 44.4949,
    "fromLon": 11.3426,
    "toLat": 44.5050,
    "toLon": 11.3500,
    "speedMetersPerSecond": 8.0,
    "initialBatteryPct": 90
  }'
```

---

## Debugging

Logs:

```bash
kubectl logs -n acme-fleet deploy/vehicle-simulator-service -f
```

Expected output:

```
Start called for V1
Task scheduled for V1
Tick started for V1
Events sent for V1
```

Debug inside cluster:

```bash
kubectl run debug --rm -it -n acme-fleet --image=curlimages/curl -- sh
curl http://vehicle-simulator-service:8080/health
```

---

## GraphHopper Integration

Example:

```bash
curl "http://graphhopper-service:8989/route?point=LAT1,LON1&point=LAT2,LON2&profile=car&points_encoded=false"
```

---

## Kafka Integration

Topics:
- vehicle-tracking
- vehicle-battery

Consume events:

```bash
kubectl exec -it -n kafka <kafka-pod> -- \
  bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic vehicle-tracking \
  --from-beginning
```

---

## Configuration

- TICK_SECONDS
- battery consumption is randomized for each trip; at the 5% safety threshold the simulator pauses and the BPMN boundary flow invokes `stop-and-charge`, which recharges it to 25% and resumes it. The minimum observed level is retained for penalty calculation.
- GraphHopper URL
- Kafka bootstrap servers

---

## Notes

- One simulation per vehicle
- State stored in memory
- Starting a simulation with same ID replaces previous one
