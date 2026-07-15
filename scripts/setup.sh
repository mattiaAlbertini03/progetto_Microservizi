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
    simulation-visualizer-service
)

die() {
    echo "ERROR: $*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 ||
        die "Required command not found: $1"
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

minikube_memory_mb() {
    local config_file="$HOME/.minikube/profiles/$PROFILE/config.json"

    if [[ ! -f "$config_file" ]]; then
        printf '0\n'
        return
    fi

    jq -r '.Memory // 0' "$config_file"
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
        awk '
            {
                split($2, ready, "/");

                if ($3 != "Running" || ready[1] != ready[2]) {
                    print $1;
                }
            }
        '
    )"

    local pod

    while IFS= read -r pod; do
        [[ -n "$pod" ]] || continue

        echo
        echo "--- Describe: $pod ---"
        kubectl describe pod "$pod" -n "$namespace" || true

        echo
        echo "--- Current Logs: $pod ---"
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
    local interval_seconds=5
    local elapsed=0

    echo "==> Waiting for all current pods in namespace '$namespace'"

    while (( elapsed < timeout_seconds )); do
        local pod_output
        local total_pods
        local unready_pods

        pod_output="$(
            kubectl get pods \
                -n "$namespace" \
                --no-headers 2>/dev/null || true
        )"

        total_pods="$(
            awk 'NF { count++ } END { print count + 0 }' \
                <<< "$pod_output"
        )"

        unready_pods="$(
            awk '
                NF {
                    split($2, ready, "/");

                    if ($3 != "Running" || ready[1] != ready[2]) {
                        count++;
                    }
                }

                END {
                    print count + 0;
                }
            ' <<< "$pod_output"
        )"

        if (( total_pods > 0 && unready_pods == 0 )); then
            echo "==> Namespace '$namespace' is ready"
            return 0
        fi

        printf \
            "    Waiting: %s pod(s), %s not ready\r" \
            "$total_pods" \
            "$unready_pods"

        sleep "$interval_seconds"
        elapsed=$((elapsed + interval_seconds))
    done

    echo
    echo "ERROR: Namespace '$namespace' did not become ready within ${timeout_seconds}s"

    collect_namespace_diagnostics "$namespace"
    return 1
}

wait_for_deployments() {
    local namespace="$1"
    local timeout_seconds="$2"

    echo "==> Waiting for Deployments in namespace '$namespace'"

    local deployments

    deployments="$(
        kubectl get deployments \
            -n "$namespace" \
            -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}'
    )"

    if [[ -z "$deployments" ]]; then
        echo "==> No Deployments found in namespace '$namespace'"
        return 0
    fi

    local deployment

    while IFS= read -r deployment; do
        [[ -n "$deployment" ]] || continue

        echo "    Waiting for deployment/$deployment"

        if ! kubectl rollout status \
            "deployment/$deployment" \
            -n "$namespace" \
            --timeout="${timeout_seconds}s"; then

            collect_namespace_diagnostics "$namespace"
            return 1
        fi
    done <<< "$deployments"

    echo "==> All Deployments in namespace '$namespace' are ready"
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

    local actual_memory_mb
    actual_memory_mb="$(minikube_memory_mb)"

    if [[ ! "$actual_memory_mb" =~ ^[0-9]+$ ]]; then
        die "Invalid Minikube memory value: $actual_memory_mb"
    fi

    if (( actual_memory_mb < MINIKUBE_MEMORY_MB )); then
        cat >&2 <<EOF

The Minikube profile '$PROFILE' does not have enough memory.

Required: ${MINIKUBE_MEMORY_MB} MiB
Configured: ${actual_memory_mb} MiB

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

    echo "==> Minikube memory: ${actual_memory_mb} MiB"
}

ensure_namespaces() {
    echo "==> Creating Kubernetes namespaces"

    kubectl create namespace "$ACME_NAMESPACE" \
        --dry-run=client \
        -o yaml |
        kubectl apply -f -

    kubectl create namespace "$ORCHESTRATION_NAMESPACE" \
        --dry-run=client \
        -o yaml |
        kubectl apply -f -

    kubectl create namespace "$KAFKA_NAMESPACE" \
        --dry-run=client \
        -o yaml |
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
        --wait=hookOnly
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
        echo " BUILDING AND DEPLOYING: $service"
        echo "========================================"

        if ! "$ROOT_DIR/scripts/service.sh" deploy "$service"; then
            die "Build or deployment failed for service: $service"
        fi

        echo "==> Successfully deployed: $service"
    done
}

main() {
    require_command docker
    require_command minikube
    require_command kubectl
    require_command helm
    require_command timeout
    require_command jq

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
    wait_for_deployments "$ACME_NAMESPACE" 300 || failed=1

    if (( failed != 0 )); then
        die "Setup incomplete: one or more components are not ready"
    fi

    echo
    echo "========================================"
    echo " SETUP COMPLETED SUCCESSFULLY"
    echo "========================================"

    kubectl get pods -A

    echo
    echo "Minikube IP: $(minikube -p "$PROFILE" ip)"
}

main "$@"