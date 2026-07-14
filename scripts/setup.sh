#!/usr/bin/env bash

set -Eeuo pipefail

PROFILE="${MINIKUBE_PROFILE:-asm}"
MINIKUBE_MEMORY_MB="${MINIKUBE_MEMORY_MB:-16384}"
MINIKUBE_CPUS="${MINIKUBE_CPUS:-8}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

ACME_NAMESPACE="acme"
ORCHESTRATION_NAMESPACE="orchestration"
KAFKA_NAMESPACE="kafka"

CAMUNDA_RELEASE="camunda"
CAMUNDA_VALUES="$ROOT_DIR/infrastructure/helm/camunda-values.yaml"

STRIMZI_INSTALL_URL="${STRIMZI_INSTALL_URL:-https://strimzi.io/install/latest?namespace=kafka}"
STRIMZI_KAFKA_URL="${STRIMZI_KAFKA_URL:-https://strimzi.io/examples/latest/kafka/kafka-single-node.yaml}"

TOPICS_DIR="$ROOT_DIR/infrastructure/kubernetes/topics"
SECRETS_DIR="$ROOT_DIR/infrastructure/kubernetes/secrets"

SERVICES=(
    auth-service
    reservation-service
    fleet-service
    battery-service
    tracking-service
    query-service
    stations-service
    logistics-service
    banking-service
    acme-worker-service
    acme-mobility-service
)

die() {
    echo "ERROR: $*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 ||
        die "Required command not found:: $1"
}

profile_exists() {
    minikube profile list -o json 2>/dev/null |
        grep -q "\"Name\":\"$PROFILE\""
}

profile_running() {
    timeout 20 minikube -p "$PROFILE" status \
        --format='{{.Host}}' 2>/dev/null |
        grep -q '^Running$'
}

docker_memory_bytes() {
    docker inspect "$PROFILE" \
        --format '{{.HostConfig.Memory}}' 2>/dev/null ||
        printf '0\n'
}

collect_namespace_diagnostics() {
    local namespace="$1"

    echo
    echo "========== DIAGNOSTICS: $namespace =========="

    kubectl get pods -n "$namespace" -o wide || true

    echo
    echo "--- Recent Events ---"
    kubectl get events \
        -n "$namespace" \
        --sort-by=.lastTimestamp |
        tail -50 || true

    local failed_pods
    failed_pods="$(
        kubectl get pods \
            -n "$namespace" \
            --no-headers 2>/dev/null |
        awk '$2 !~ /^[0-9]+\/[0-9]+$/ || $3 != "Running" {
            print $1
        }'
    )"

    local pod
    while IFS= read -r pod; do
        [[ -n "$pod" ]] || continue

        echo
        echo "--- Describe: $pod ---"
        kubectl describe pod "$pod" -n "$namespace" || true

        echo
        echo "--- Current logs: $pod ---"
        kubectl logs "$pod" \
            -n "$namespace" \
            --all-containers=true \
            --tail=200 || true

        echo
        echo "--- Previous Logs: $pod ---"
        kubectl logs "$pod" \
            -n "$namespace" \
            --all-containers=true \
            --previous \
            --tail=200 || true
    done <<< "$failed_pods"
}

wait_for_namespace() {
    local namespace="$1"
    local timeout_seconds="$2"

    echo "==> Waiting for all pods in namespace '$namespace'"

    if ! kubectl wait \
        --for=condition=Ready \
        pod \
        --all \
        -n "$namespace" \
        --timeout="${timeout_seconds}s"; then

        collect_namespace_diagnostics "$namespace"
        return 1
    fi
}

ensure_minikube() {
    if ! profile_exists; then
        echo "==> Creating Minikube profile '$PROFILE'"

        minikube start \
            -p "$PROFILE" \
            --driver=docker \
            --memory="$MINIKUBE_MEMORY_MB" \
            --cpus="$MINIKUBE_CPUS"
    elif ! profile_running; then
        echo "==> Starting existing Minikube profile '$PROFILE'"
        minikube -p "$PROFILE" start
    else
        echo "==> Minikube profile '$PROFILE' is already running"
    fi

    minikube -p "$PROFILE" update-context

    local required_bytes
    local actual_bytes

    required_bytes=$((MINIKUBE_MEMORY_MB * 1024 * 1024))
    actual_bytes="$(docker_memory_bytes)"

    if (( actual_bytes < required_bytes )); then
        cat >&2 <<EOF

The Minikube profile '$PROFILE' does not have enough memory.

Required: ${MINIKUBE_MEMORY_MB} MiB
Configured: $((actual_bytes / 1024 / 1024)) MiB

This script will not automatically recreate an existing cluster.

To recreate it, run:

minikube delete -p "$PROFILE"

minikube start -p "$PROFILE" \
  --driver=docker \
  --memory="$MINIKUBE_MEMORY_MB" \
  --cpus="$MINIKUBE_CPUS"

EOF
        exit 1
    fi
}

ensure_namespaces() {
    echo "==> Creating Kubernetes namespaces"

    kubectl create namespace "$ACME_NAMESPACE" \
        --dry-run=client -o yaml |
        kubectl apply -f -

    kubectl create namespace "$ORCHESTRATION_NAMESPACE" \
        --dry-run=client -o yaml |
        kubectl apply -f -

    kubectl create namespace "$KAFKA_NAMESPACE" \
        --dry-run=client -o yaml |
        kubectl apply -f -
}

install_camunda() {
    [[ -f "$CAMUNDA_VALUES" ]] ||
        die "Camunda values file not found: $CAMUNDA_VALUES"

    echo "==> Configuring Camunda Helm repository"

    helm repo add camunda https://helm.camunda.io \
        --force-update

    helm repo update camunda

    echo "==> Installing/Updating Camunda Platform"

    helm upgrade --install "$CAMUNDA_RELEASE" \
        camunda/camunda-platform \
        -n "$ORCHESTRATION_NAMESPACE" \
        -f "$CAMUNDA_VALUES" \
        --wait=false
}

install_kafka() {
    echo "==> Installing/Updating the Strimzi Cluster Operator"

    kubectl apply \
        -f "$STRIMZI_INSTALL_URL" \
        -n "$KAFKA_NAMESPACE"

    echo "==> Waiting for Strimzi CRDs to be registered"

    kubectl wait \
        --for=condition=Established \
        crd/kafkas.kafka.strimzi.io \
        --timeout=180s

    kubectl wait \
        --for=condition=Established \
        crd/kafkatopics.kafka.strimzi.io \
        --timeout=180s

    echo "==> Waiting for the Strimzi Cluster Operator"

    kubectl rollout status \
        deployment/strimzi-cluster-operator \
        -n "$KAFKA_NAMESPACE" \
        --timeout=300s

    echo "==> Creating/Updating Kafka cluster"

    kubectl apply \
        -f "$STRIMZI_KAFKA_URL" \
        -n "$KAFKA_NAMESPACE"

    echo "==> Waiting for Kafka cluster to become Ready"

    kubectl wait \
        kafka/my-cluster \
        -n "$KAFKA_NAMESPACE" \
        --for=condition=Ready \
        --timeout=600s

    apply_kafka_topics
}

apply_shared_resources() {
    if [[ -d "$SECRETS_DIR" ]]; then
        echo "==> Applying shared Kubernetes Secrets"

        kubectl apply \
            --recursive \
            -f "$SECRETS_DIR"
    else
        echo "==> No Secret directory found: $SECRETS_DIR"
    fi
}

apply_kafka_topics() {
    if [[ ! -d "$TOPICS_DIR" ]]; then
        echo "==> No Kafka topics directory found: $TOPICS_DIR"
        return
    fi

    if ! kubectl get crd kafkatopics.kafka.strimzi.io \
        >/dev/null 2>&1; then
        die "KafkaTopic CRD is not installed. Strimzi is not ready."
    fi

    echo "==> Applying Kafka topics"

    kubectl apply \
        --recursive \
        -f "$TOPICS_DIR"
}

deploy_services() {
    local service

    for service in "${SERVICES[@]}"; do
        echo
        echo "========================================"
        echo "  BUILDING AND DEPLOYING: $service      "
        echo "========================================"

        "$ROOT_DIR/scripts/service.sh" \
            deploy \
            "$service"
    done
}

main() {
    require_command docker
    require_command minikube
    require_command kubectl
    require_command helm
    require_command timeout

    ensure_minikube
    ensure_namespaces
    apply_shared_resources

    install_camunda

    wait_for_namespace "$ORCHESTRATION_NAMESPACE" 900

    "$ROOT_DIR/scripts/deploy-processes.sh"

    install_kafka
    deploy_services

    local failed=0

    wait_for_namespace "$KAFKA_NAMESPACE" 600 || failed=1
    wait_for_namespace "$ORCHESTRATION_NAMESPACE" 900 || failed=1
    wait_for_namespace "$ACME_NAMESPACE" 600 || failed=1

    if (( failed != 0 )); then
        die "Setup incompleto: uno o più componenti non sono pronti"
    fi

    echo
    echo "========================================"
    echo "  SETUP COMPLETED SUCCESSFULLY "
    echo "========================================"

    kubectl get pods -A
    echo
    echo "Minikube IP: $(minikube -p "$PROFILE" ip)"
}

main "$@"