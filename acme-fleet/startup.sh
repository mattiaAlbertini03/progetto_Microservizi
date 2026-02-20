#!/usr/bin/env bash
set -euo pipefail

# ----------------------------
# Config (override via env)
# ----------------------------
NS="${NS:-acme-fleet}"
TAG="${TAG:-$(date +%Y%m%d%H%M%S)}"
MODE="${MODE:-auto}"                 # auto | kind | minikube | none
ROOT="${ROOT:-$PWD}"

MANIFEST_DIR="${MANIFEST_DIR:-$ROOT/k8s}"

TOPIC_MANIFEST_DIR="${TOPIC_MANIFEST_DIR:-$ROOT/k8s-kafka-topics}"
APPLY_TOPICS="${APPLY_TOPICS:-true}"          # true|false
KAFKA_NS="${KAFKA_NS:-kafka}"
KAFKA_BOOTSTRAP_SVC="${KAFKA_BOOTSTRAP_SVC:-my-cluster-kafka-bootstrap}"
KAFKA_BOOTSTRAP_PORT="${KAFKA_BOOTSTRAP_PORT:-9092}"

SERVICES=(tracking-service battery-service query-service)

CLEAN_DEPLOYS="${CLEAN_DEPLOYS:-false}"        # true|false

# ----------------------------
# Helpers
# ----------------------------
log() { echo -e "==> $*"; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || { echo "Missing command: $1" >&2; exit 1; }
}

detect_mode() {
  if [[ "$MODE" != "auto" ]]; then
    echo "$MODE"; return
  fi

  local ctx
  ctx="$(kubectl config current-context 2>/dev/null || true)"
  if echo "$ctx" | grep -qi "kind"; then
    echo "kind"; return
  fi

  if command -v minikube >/dev/null 2>&1 && minikube status >/dev/null 2>&1; then
    echo "minikube"; return
  fi

  echo "none"
}

# ----------------------------
# Preconditions
# ----------------------------
need_cmd kubectl
need_cmd mvn
need_cmd docker

if [[ ! -d "$MANIFEST_DIR" ]]; then
  echo "Manifest directory not found: $MANIFEST_DIR" >&2
  exit 1
fi

MODE="$(detect_mode)"
log "Detected mode: $MODE"
log "Tag: $TAG"

# ----------------------------
# IMPORTANT PATCH for minikube:
# build directly into minikube's Docker daemon
# ----------------------------
if [[ "$MODE" == "minikube" ]]; then
  need_cmd minikube
  log "Using minikube Docker daemon (docker-env)"
  # shellcheck disable=SC2046
  eval "$(minikube -p minikube docker-env)"
fi

# ----------------------------
# Namespace
# ----------------------------
log "Namespace: $NS"
kubectl get ns "$NS" >/dev/null 2>&1 || kubectl create ns "$NS" >/dev/null

# ----------------------------
# Optional cleanup (ONLY our services)
# ----------------------------
if [[ "$CLEAN_DEPLOYS" == "true" ]]; then
  log "Deleting only service deployments (and services) in namespace $NS"
  for s in "${SERVICES[@]}"; do
    kubectl delete deployment -n "$NS" "$s" --ignore-not-found
    kubectl delete service -n "$NS" "$s" --ignore-not-found
  done
fi

# ----------------------------
# Build (Maven)
# ----------------------------
log "Maven build (skip tests)"
for s in "${SERVICES[@]}"; do
  log "Build $s"
  (cd "$ROOT/$s" && mvn -DskipTests clean package)
done

# ----------------------------
# Build (Docker images)
# ----------------------------
log "Docker build (tag=$TAG)"
for s in "${SERVICES[@]}"; do
  log "Image $s:$TAG"
  docker build -t "$s:$TAG" "$ROOT/$s"
done

# Sanity: ensure images exist in the CURRENT docker daemon
log "Built images (sanity check)"
docker images --format '{{.Repository}}:{{.Tag}}' | grep -E "^(tracking-service|battery-service|query-service):$TAG$" || {
  echo "ERROR: Images not found in current docker daemon. Check docker-env/minikube profile." >&2
  exit 1
}

# ----------------------------
# Load images into cluster runtime
# ----------------------------
# PATCH: if building inside minikube docker daemon, DO NOT load images
case "$MODE" in
  kind)
    need_cmd kind
    log "Loading images into kind"
    for s in "${SERVICES[@]}"; do
      kind load docker-image "$s:$TAG" >/dev/null
    done
    ;;
  minikube)
    log "Minikube mode: images already available in minikube Docker daemon. Skipping 'minikube image load'."
    ;;
  none)
    log "MODE=none. Assumo che il cluster possa pullare le immagini (registry)."
    ;;
  *)
    echo "Invalid MODE: $MODE (use auto|kind|minikube|none)" >&2
    exit 1
    ;;
esac

# ----------------------------
# (Optional) Apply Kafka topics via Strimzi CRs
# ----------------------------
if [[ "$APPLY_TOPICS" == "true" ]]; then
  if [[ ! -d "$TOPIC_MANIFEST_DIR" ]]; then
    echo "Topic manifest directory not found: $TOPIC_MANIFEST_DIR" >&2
    exit 1
  fi
  log "Applying KafkaTopic manifests to namespace $KAFKA_NS: $TOPIC_MANIFEST_DIR"
  kubectl apply -n "$KAFKA_NS" -f "$TOPIC_MANIFEST_DIR"
fi

# ----------------------------
# Verify Strimzi bootstrap service exists
# ----------------------------
log "Checking Kafka bootstrap service: $KAFKA_BOOTSTRAP_SVC.$KAFKA_NS:$KAFKA_BOOTSTRAP_PORT"
kubectl get svc -n "$KAFKA_NS" "$KAFKA_BOOTSTRAP_SVC" >/dev/null

# ----------------------------
# Apply service manifests
# ----------------------------
log "kubectl apply: $MANIFEST_DIR (namespace $NS)"
kubectl apply -n "$NS" -f "$MANIFEST_DIR"

# Force deployments to use the just-built tag
log "Setting deployment images to :$TAG"
for s in "${SERVICES[@]}"; do
  kubectl set image -n "$NS" "deploy/$s" "$s=$s:$TAG" >/dev/null 2>&1 || true
done

# ----------------------------
# Restart & wait deployments
# ----------------------------
log "Rollout restart services"
for s in "${SERVICES[@]}"; do
  kubectl rollout restart -n "$NS" "deploy/$s" >/dev/null 2>&1 || true
done

log "Wait for rollouts"
for s in "${SERVICES[@]}"; do
  kubectl rollout status -n "$NS" "deploy/$s" --timeout=180s
done

log "Done (tag=$TAG)"
echo "Tip: kubectl get pods -n $NS -o wide"
echo "Tip: kubectl get svc  -n $NS"

log "ImageIDs:"
for s in "${SERVICES[@]}"; do
  kubectl -n "$NS" get pod -l app="$s" -o jsonpath='{.items[0].status.containerStatuses[0].imageID}{"\n"}' 2>/dev/null || true
done
