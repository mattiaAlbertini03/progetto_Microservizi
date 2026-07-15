#!/usr/bin/env bash

set -Eeuo pipefail

PROFILE="${MINIKUBE_PROFILE:-asm}"
NAMESPACE="${ACME_NAMESPACE:-acme}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
    cat <<'EOF'
Usage:
  ./scripts/service.sh <command> <service> [deployment]

Commands:
  build      Build the Docker image inside Minikube
  apply      Apply the Kubernetes manifest
  deploy     Build the image, apply the manifest and update the Deployment
  rebuild    Build a new image and update the Deployment
  restart    Restart an existing Deployment
  logs       Follow the Deployment logs

Examples:
  ./scripts/service.sh deploy auth-service
  ./scripts/service.sh rebuild acme-mobility-service
  ./scripts/service.sh restart reservation-service
  ./scripts/service.sh logs auth-service
EOF
}

die() {
    echo "ERROR: $*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 ||
        die "Required command not found: $1"
}

manifest_path() {
    local service="$1"
    printf '%s\n' \
        "$ROOT_DIR/infrastructure/kubernetes/services/${service}.yaml"
}

wait_for_deployment() {
    local deployment="$1"

    if ! kubectl rollout status \
        "deployment/$deployment" \
        -n "$NAMESPACE" \
        --timeout=180s; then

        echo
        echo "Deployment not ready: $deployment"
        kubectl get pods -n "$NAMESPACE" \
            -l "app=$deployment" \
            -o wide || true

        kubectl describe deployment "$deployment" \
            -n "$NAMESPACE" || true

        kubectl logs \
            -n "$NAMESPACE" \
            "deployment/$deployment" \
            --all-containers=true \
            --tail=200 || true

        exit 1
    fi
}

build_image() {
    local service="$1"
    local service_dir="$2"
    local tag="$3"

    echo "==> Building ${service}:${tag}"
    echo "    Build context: $service_dir"

    (
        cd "$service_dir"

        if ! minikube -p "$PROFILE" image build \
            -t "${service}:${tag}" \
            .; then
            die "Failed to build image ${service}:${tag}"
        fi
    )
}

image_exists() {
    local image="$1"

    minikube -p "$PROFILE" ssh -- \
        docker image inspect "$image" >/dev/null 2>&1
}

deploy_manifest() {
    local service="$1"
    local manifest

    manifest="$(manifest_path "$service")"

    [[ -f "$manifest" ]] ||
        die "Manifest not found: $manifest"

    echo "==> Applying $manifest"
    kubectl apply -f "$manifest"
}

set_deployment_image() {
    local service="$1"
    local deployment="$2"
    local tag="$3"

    echo "==> Updating Deployment $deployment to image ${service}:${tag}"

    kubectl set image \
        "deployment/$deployment" \
        "${deployment}=${service}:${tag}" \
        -n "$NAMESPACE"

    wait_for_deployment "$deployment"
}

resolve_service_dir() {
    local service="$1"

    local candidates=(
        "$ROOT_DIR/backend/services/$service"
        "$ROOT_DIR/backend/services/fleet-management/$service"
        "$ROOT_DIR/frontend/$service"
    )

    local candidate

    for candidate in "${candidates[@]}"; do
        if [[ -f "$candidate/Dockerfile" ]]; then
            realpath "$candidate"
            return 0
        fi
    done

    die "Service directory not found: $service"
}

main() {
    [[ $# -ge 2 ]] || {
        usage
        exit 1
    }

    local command="$1"
    local service="$2"
    local deployment="${3:-$service}"
    local tag="dev-$(date +%Y%m%d%H%M%S)"
    local service_dir

    require_command kubectl
    require_command minikube

    service_dir="$(resolve_service_dir "$service")"

    minikube -p "$PROFILE" update-context >/dev/null

    case "$command" in
        build)
            build_image "$service" "$service_dir" "$tag"
            ;;

        apply)
            deploy_manifest "$service"
            wait_for_deployment "$deployment"
            ;;

        deploy)
            build_image "$service" "$service_dir" "$tag"

            if ! image_exists "${service}:${tag}"; then
                die "Image ${service}:${tag} was not found in Minikube after the build"
            fi

            deploy_manifest "$service"
            set_deployment_image "$service" "$deployment" "$tag"
            ;;

        restart)
            kubectl rollout restart \
                "deployment/$deployment" \
                -n "$NAMESPACE"

            wait_for_deployment "$deployment"
            ;;

        rebuild)
            build_image "$service" "$service_dir" "$tag"

            if ! image_exists "${service}:${tag}"; then
                die "Image ${service}:${tag} was not found in Minikube after the build"
            fi

            set_deployment_image "$service" "$deployment" "$tag"
            ;;

        logs)
            kubectl logs \
                -f \
                -n "$NAMESPACE" \
                "deployment/$deployment" \
                --all-containers=true
            ;;

        *)
            usage
            die "Unknown command: $command"
            ;;
    esac
}

main "$@"
