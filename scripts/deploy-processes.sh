#!/usr/bin/env bash

set -Eeuo pipefail

PROFILE="${MINIKUBE_PROFILE:-asm}"
NAMESPACE="${ORCHESTRATION_NAMESPACE:-orchestration}"
CAMUNDA_SERVICE="${CAMUNDA_SERVICE:-camunda-zeebe-gateway}"
LOCAL_PORT="${CAMUNDA_LOCAL_PORT:-18080}"

CAMUNDA_USERNAME="${CAMUNDA_USERNAME:-demo}"
CAMUNDA_PASSWORD="${CAMUNDA_PASSWORD:-demo}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROCESS_DIR="${PROCESS_DIR:-$ROOT_DIR/docs/orchestration/processes}"

die() {
    echo "ERROR: $*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 ||
        die "Required command not found: $1"
}

cleanup() {
    if [[ -n "${PORT_FORWARD_PID:-}" ]]; then
        kill "$PORT_FORWARD_PID" >/dev/null 2>&1 || true
        wait "$PORT_FORWARD_PID" 2>/dev/null || true
    fi
}

trap cleanup EXIT

wait_for_api() {
    local attempts=60

    for ((i = 1; i <= attempts; i++)); do
        if ! kill -0 "$PORT_FORWARD_PID" 2>/dev/null; then
            echo "Port-forward terminated unexpectedly:" >&2
            cat "$PORT_FORWARD_LOG" >&2 || true
            die "Camunda port-forward failed"
        fi

        local status
        status="$(
            curl \
                --silent \
                --user "${CAMUNDA_USERNAME}:${CAMUNDA_PASSWORD}" \
                --output /tmp/camunda-topology-response.json \
                --write-out '%{http_code}' \
                "http://127.0.0.1:${LOCAL_PORT}/v2/topology" \
                2>/dev/null || true
        )"

        if [[ "$status" == "200" ]]; then
            return 0
        fi

        sleep 2
    done

    echo "Port-forward log:" >&2
    cat "$PORT_FORWARD_LOG" >&2 || true

    echo "Last API response:" >&2
    cat /tmp/camunda-topology-response.json >&2 || true
    echo >&2

    die "Camunda REST API did not become available"
}

deploy_file() {
    local file="$1"
    local filename

    filename="$(basename "$file")"

    echo "==> Deploying $filename"

    local response_file
    response_file="$(mktemp)"

    local http_status
    http_status="$(
        curl \
            --silent \
            --show-error \
            --user "${CAMUNDA_USERNAME}:${CAMUNDA_PASSWORD}" \
            --output "$response_file" \
            --write-out '%{http_code}' \
            --request POST \
            "http://127.0.0.1:${LOCAL_PORT}/v2/deployments" \
            --form "resources=@${file}"
    )"

    if [[ "$http_status" != "200" ]]; then
        echo "Camunda response:"
        cat "$response_file"
        echo
        rm -f "$response_file"

        die "Failed to deploy $filename. HTTP status: $http_status"
    fi

    echo "    Deployment successful"
    cat "$response_file"
    echo

    rm -f "$response_file"
}

main() {
    require_command kubectl
    require_command minikube
    require_command curl
    require_command find

    [[ -d "$PROCESS_DIR" ]] ||
        die "Process directory not found: $PROCESS_DIR"

    minikube -p "$PROFILE" update-context >/dev/null

    echo "==> Waiting for Camunda gateway"

    kubectl rollout status \
        statefulset/camunda-zeebe \
        -n "$NAMESPACE" \
        --timeout=600s

    echo "==> Starting temporary port-forward"

    kubectl port-forward \
        -n "$NAMESPACE" \
        "service/$CAMUNDA_SERVICE" \
        "${LOCAL_PORT}:8080" \
        >/tmp/camunda-port-forward.log 2>&1 &

    PORT_FORWARD_PID=$!

    wait_for_api

    mapfile -t resources < <(
        find "$PROCESS_DIR" \
            -maxdepth 1 \
            -type f \
            \( \
                -name "*.bpmn" -o \
                -name "*.dmn" -o \
                -name "*.form" \
            \) \
            | sort
    )

    if (( ${#resources[@]} == 0 )); then
        die "No BPMN, DMN or form resources found in $PROCESS_DIR"
    fi

    local resource
    for resource in "${resources[@]}"; do
        deploy_file "$resource"
    done

    echo
    echo "========================================"
    echo " PROCESS RESOURCES DEPLOYED SUCCESSFULLY"
    echo "========================================"
}

main "$@"