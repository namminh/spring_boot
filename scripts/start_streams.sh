#!/usr/bin/env bash
set -euo pipefail

# Start Kafka Streams processor using application-streams.yml
# Requirements: mvn, kafka broker accessible via KAFKA_BOOTSTRAP_SERVERS
# Optional env:
#   KAFKA_BOOTSTRAP_SERVERS (default: localhost:9092)
#   PAYMENT_STREAMS_PROFILE (default: streams)
#   SPRING_ADDITIONAL_OPTS for extra Spring arguments

require_bin() {
  for bin in "$@"; do
    if ! command -v "$bin" >/dev/null 2>&1; then
      echo "[ERROR] Required dependency '$bin' is not installed." >&2
      exit 1
    fi
  done
}

require_bin mvn

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
PROFILE=${PAYMENT_STREAMS_PROFILE:-streams}
SPRING_OPTS=${SPRING_ADDITIONAL_OPTS:-}

export KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}

cat <<EOT
[INFO] Launching Kafka Streams application
       Profile: ${PROFILE}
       Kafka:   ${KAFKA_BOOTSTRAP_SERVERS}
EOT

mvn -f "$ROOT_DIR/pom.xml" spring-boot:run \
  -Dspring-boot.run.main-class=com.corebank.payment.stream.PaymentEventStreamApplication \
  -Dspring-boot.run.profiles=${PROFILE} \
  -Dspring-boot.run.arguments="${SPRING_OPTS}"
