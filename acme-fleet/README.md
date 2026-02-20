# ACME Fleet -- Microservices Architecture (Helidon MP + Kafka + Kubernetes)

ACME Fleet è un sistema a microservizi per il monitoraggio di veicoli
elettrici, basato su:

-   **Helidon MP 4**
-   **Apache Kafka (Strimzi)**
-   **Kubernetes (Minikube)**
-   Architettura **event-driven / CQRS**

------------------------------------------------------------------------

## 📦 Servizi

  Servizio             Ruolo
  -------------------- ------------------------------------------------
  `tracking-service`   Pubblica eventi di posizione veicoli su Kafka
  `battery-service`    Pubblica eventi di stato batteria su Kafka
  `query-service`      Consuma eventi e mantiene uno stato read-model

------------------------------------------------------------------------

## 🏗 Architettura

    tracking-service  ---> Kafka topic: vehicle-tracking                                                ---> query-service (consumer)
    battery-service   ---> Kafka topic: vehicle-battery  /

-   I producer usano:
    -   `acks=all`
    -   `enable.idempotence=true`
-   Il `query-service` mantiene uno store in-memory aggiornato dagli
    eventi.

------------------------------------------------------------------------

## ⚙️ Prerequisiti

-   Java 21
-   Maven
-   Docker
-   kubectl
-   Minikube
-   Strimzi Kafka installato nel namespace `kafka`

Verifica cluster:

``` bash
kubectl get pods -A
```

------------------------------------------------------------------------

## 🚀 Deploy su Minikube

Lo script `startup.sh`:

1.  Compila i servizi
2.  Builda le immagini Docker
3.  Le inserisce nel Docker daemon di Minikube
4.  Applica i manifest Kubernetes
5.  Forza il rollout con tag univoco

### Esecuzione consigliata

``` bash
MODE=minikube CLEAN_DEPLOYS=true ./startup.sh
```

------------------------------------------------------------------------

## 🔧 Docker di Minikube

Lo script utilizza:

``` bash
eval $(minikube -p minikube docker-env)
```

Non utilizzare `minikube image load` in questo flusso.

------------------------------------------------------------------------

## 📡 Verifica stato servizi

``` bash
kubectl -n acme-fleet get pods -o wide
kubectl -n acme-fleet get svc
```

Log:

``` bash
kubectl -n acme-fleet logs -f deploy/query-service
kubectl -n acme-fleet logs -f deploy/tracking-service
```

------------------------------------------------------------------------

## 📬 Test rapido

``` bash
curl -X POST http://<tracking-service-ip>:8080/tracking   -H "Content-Type: application/json"   -d '{
    "vehicleId":"V1",
    "lat":44.4949,
    "lon":11.3426
  }'
```

Verifica nel query-service:

``` bash
curl http://<query-service-ip>:8080/vehicles/V1
```

------------------------------------------------------------------------

## 🧠 Configurazione Kafka Producer

-   `acks=all`
-   `enable.idempotence=true`
-   `linger.ms=5`

------------------------------------------------------------------------

## 🔄 Problemi comuni

### Rollout bloccato

Verifica:

``` bash
kubectl -n acme-fleet get pods
kubectl -n acme-fleet describe pod <pod>
```

### ImagePullBackOff su Minikube

Controlla Docker daemon attivo:

``` bash
docker images
```

------------------------------------------------------------------------

## 📎 Struttura progetto

    acme-fleet/
     ├── tracking-service/
     ├── battery-service/
     ├── query-service/
     ├── k8s/
     ├── k8s-kafka-topics/
     └── startup.sh

------------------------------------------------------------------------

## 📌 Namespace

-   Servizi: `acme-fleet`
-   Kafka / Strimzi: `kafka`