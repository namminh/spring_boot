#!/usr/bin/env bash
set -euo pipefail

# Start the Spring Boot applications (payment API, monitoring orchestrator, investigation service).
# Options (override via environment variables):
#   PAYMENT_PORT (default: 8080)
#   ORCHESTRATOR_PORT (default: 8081)
#   INVESTIGATION_PORT (default: 8082)
#   SKIP_BUILD=1 to skip the initial Maven compile step
require_bin() {
  for bin in "$@"; do
    if ! command -v "$bin" >/dev/null 2>&1; then
      echo "[ERROR] Required dependency '$bin' is not installed or not on PATH." >&2
      exit 1
    fi
  done
}

require_bin mvn

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
PAYMENT_PORT=${PAYMENT_PORT:-8080}
ORCHESTRATOR_PORT=${ORCHESTRATOR_PORT:-8081}
INVESTIGATION_PORT=${INVESTIGATION_PORT:-8082}
SKIP_BUILD=${SKIP_BUILD:-0}
ACTIVE_PROFILES=${SPRING_PROFILES_ACTIVE:-lab-java21}

if [[ "$SKIP_BUILD" != "1" ]]; then
  echo "[INFO] Preparing classes via mvn compile (set SKIP_BUILD=1 to skip)."
  mvn -B -DskipTests compile -f "$ROOT_DIR/pom.xml"
fi

PIDS=()

start_payment() {
  echo "[INFO] Starting payment application on port $PAYMENT_PORT"
  mvn -f "$ROOT_DIR/pom.xml" spring-boot:run \
    -Dspring-boot.run.main-class=com.corebank.payment.CorebankPaymentApplication \
    -Dspring-boot.run.profiles=$ACTIVE_PROFILES \
    -Dspring-boot.run.arguments=--server.port=$PAYMENT_PORT \
    -Dspring-boot.run.fork=false &
  PIDS+=($!)
}

start_orchestrator() {
  echo "[INFO] Starting monitoring orchestrator on port $ORCHESTRATOR_PORT"
  mvn -f "$ROOT_DIR/pom.xml" spring-boot:run \
    -Dspring-boot.run.main-class=com.corebank.orchestrator.CorebankOrchestratorApplication \
    -Dspring-boot.run.profiles=$ACTIVE_PROFILES \
    -Dspring-boot.run.arguments=--server.port=$ORCHESTRATOR_PORT \
    -Dspring-boot.run.fork=false &
  PIDS+=($!)
}

start_investigation() {
  echo "[INFO] Starting investigation service on port $INVESTIGATION_PORT"
  mvn -f "$ROOT_DIR/pom.xml" spring-boot:run \
    -Dspring-boot.run.main-class=com.corebank.investigation.CorebankInvestigationApplication \
    -Dspring-boot.run.profiles=$ACTIVE_PROFILES \
    -Dspring-boot.run.arguments=--server.port=$INVESTIGATION_PORT \
    -Dspring-boot.run.fork=false &
  PIDS+=($!)
}

trap 'echo "[INFO] Stopping applications"; for pid in "${PIDS[@]}"; do kill "$pid" 2>/dev/null || true; done' EXIT

start_payment
start_orchestrator
start_investigation

cat <<EOT
[READY] Applications are starting.
        Payment API:         http://localhost:${PAYMENT_PORT}/api/v1/payments
        Monitoring API:      http://localhost:${ORCHESTRATOR_PORT}/api/v1/monitoring
        Investigation API:   http://localhost:${INVESTIGATION_PORT}/api/v1/investigations
Press Ctrl+C to stop all applications.
EOT

wait -n "${PIDS[@]}"
ECHO_CODE=$?

if [[ $ECHO_CODE -ne 0 ]]; then
  echo "[WARN] One of the applications exited with code $ECHO_CODE."
fi

wait "${PIDS[@]}" || true
