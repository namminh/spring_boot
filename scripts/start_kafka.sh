#!/usr/bin/env bash
set -euo pipefail

# Start a local Kafka + Zookeeper stack via Docker Compose and create default topics.
# Requires Docker Engine with compose plugin (`docker compose`).

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] docker is required on PATH." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "[ERROR] docker compose plugin is not available." >&2
  exit 1
fi

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
COMPOSE_FILE="$SCRIPT_DIR/kafka-compose.yml"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "[ERROR] Compose file not found at $COMPOSE_FILE" >&2
  exit 1
fi

echo "[INFO] Starting Kafka stack with docker compose"
docker compose -f "$COMPOSE_FILE" up -d

echo "[INFO] Waiting for Kafka to accept connections"
ATTEMPTS=0
until docker compose -f "$COMPOSE_FILE" exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list >/dev/null 2>&1; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if (( ATTEMPTS > 20 )); then
    echo "[ERROR] Kafka did not become ready in time." >&2
    exit 1
  fi
  sleep 2
done

echo "[INFO] Kafka is ready"

TOPICS=(payments.txn.completed payments.txn.failed payments.analytics.completed)
for topic in "${TOPICS[@]}"; do
  echo "[INFO] Ensuring topic '$topic' exists"
  docker compose -f "$COMPOSE_FILE" exec -T kafka kafka-topics \
    --bootstrap-server localhost:9092 \
    --create --if-not-exists \
    --topic "$topic" \
    --partitions 3 \
    --replication-factor 1 >/dev/null 2>&1 || true
 done

echo "[INFO] Kafka stack started. Export PAYMENT_EVENTS_KAFKA_ENABLED=true and run the application."
