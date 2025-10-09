#!/usr/bin/env bash
set -euo pipefail

# Smoke-test for the CoreBank monitoring orchestrator API.
# Requires curl and jq. Override defaults via environment variables below.
#   MONITOR_BASE_URL (default: http://localhost:8081)
#   MONITOR_API_USER (default: orchestrator)
#   MONITOR_API_PASSWORD (default: changeme)
#   MONITOR_RECENT_HOURS (default: 4)
#   MONITOR_ALERT_SEVERITY (default: HIGH)
#   MONITOR_ALERT_MESSAGE (default: Test alert from monitor script)

for bin in curl jq; do
  if ! command -v "$bin" >/dev/null 2>&1; then
    echo "[ERROR] Required dependency '$bin' is not installed or not on PATH." >&2
    exit 1
  fi
done

BASE_URL=${MONITOR_BASE_URL:-http://localhost:8081}
API_USER=${MONITOR_API_USER:-orchestrator}
API_PASSWORD=${MONITOR_API_PASSWORD:-changeme}
RECENT_HOURS=${MONITOR_RECENT_HOURS:-4}
ALERT_SEVERITY=${MONITOR_ALERT_SEVERITY:-HIGH}
DEFAULT_MESSAGE="Test alert from monitor script"
ALERT_MESSAGE=${MONITOR_ALERT_MESSAGE:-$DEFAULT_MESSAGE}

STATUS_BODY=$(mktemp)
TRANSACTIONS_BODY=$(mktemp)
ALERTS_BODY=$(mktemp)
CREATE_HEADERS=$(mktemp)
CREATE_BODY=$(mktemp)
UPDATED_ALERTS_BODY=$(mktemp)
trap 'rm -f "$STATUS_BODY" "$TRANSACTIONS_BODY" "$ALERTS_BODY" "$CREATE_HEADERS" "$CREATE_BODY" "$UPDATED_ALERTS_BODY"' EXIT

STATUS_URL="$BASE_URL/api/v1/monitoring/status"
echo "[INFO] Fetching monitoring status via $STATUS_URL"
STATUS_CODE=$(curl -sS -u "$API_USER:$API_PASSWORD" \
  -H 'Accept: application/json' \
  -o "$STATUS_BODY" \
  -w '%{http_code}' \
  "$STATUS_URL")
if [[ "$STATUS_CODE" != "200" ]]; then
  echo "[ERROR] Status endpoint failed (HTTP $STATUS_CODE)." >&2
  cat "$STATUS_BODY" >&2
  exit 1
fi
cat "$STATUS_BODY" | jq '.'

TRANSACTIONS_URL="$BASE_URL/api/v1/monitoring/transactions/recent?hours=$RECENT_HOURS"
echo "[INFO] Fetching recent transactions via $TRANSACTIONS_URL"
TRANSACTIONS_CODE=$(curl -sS -u "$API_USER:$API_PASSWORD" \
  -H 'Accept: application/json' \
  -o "$TRANSACTIONS_BODY" \
  -w '%{http_code}' \
  "$TRANSACTIONS_URL")
if [[ "$TRANSACTIONS_CODE" != "200" ]]; then
  echo "[ERROR] Recent transactions endpoint failed (HTTP $TRANSACTIONS_CODE)." >&2
  cat "$TRANSACTIONS_BODY" >&2
  exit 1
fi
cat "$TRANSACTIONS_BODY" | jq '.'

ACTIVE_ALERTS_URL="$BASE_URL/api/v1/monitoring/alerts/active"
echo "[INFO] Fetching active alerts via $ACTIVE_ALERTS_URL"
ALERTS_CODE=$(curl -sS -u "$API_USER:$API_PASSWORD" \
  -H 'Accept: application/json' \
  -o "$ALERTS_BODY" \
  -w '%{http_code}' \
  "$ACTIVE_ALERTS_URL")
if [[ "$ALERTS_CODE" != "200" ]]; then
  echo "[ERROR] Active alerts endpoint failed (HTTP $ALERTS_CODE)." >&2
  cat "$ALERTS_BODY" >&2
  exit 1
fi
cat "$ALERTS_BODY" | jq '.'

CREATE_ALERT_URL="$BASE_URL/api/v1/monitoring/alerts"
ALERT_PAYLOAD=$(jq -n \
  --arg severity "$ALERT_SEVERITY" \
  --arg message "$ALERT_MESSAGE" \
  '{severity: $severity, message: $message}')

echo "[INFO] Creating alert via $CREATE_ALERT_URL"
CREATE_CODE=$(curl -sS -u "$API_USER:$API_PASSWORD" \
  -H 'Content-Type: application/json' \
  -d "$ALERT_PAYLOAD" \
  -D "$CREATE_HEADERS" \
  -o "$CREATE_BODY" \
  -w '%{http_code}' \
  "$CREATE_ALERT_URL")
if [[ "$CREATE_CODE" != "201" ]]; then
  echo "[ERROR] Create alert failed (HTTP $CREATE_CODE)." >&2
  cat "$CREATE_BODY" >&2
  exit 1
fi
cat "$CREATE_BODY" | jq '.'
LOCATION_HEADER=$(grep -i '^Location:' "$CREATE_HEADERS" | awk '{print $2}' | tr -d '\r')
if [[ -n "$LOCATION_HEADER" ]]; then
  echo "[INFO] Alert resource location: $LOCATION_HEADER"
fi

echo "[INFO] Fetching active alerts again to confirm creation"
UPDATED_ALERTS_CODE=$(curl -sS -u "$API_USER:$API_PASSWORD" \
  -H 'Accept: application/json' \
  -o "$UPDATED_ALERTS_BODY" \
  -w '%{http_code}' \
  "$ACTIVE_ALERTS_URL")
if [[ "$UPDATED_ALERTS_CODE" != "200" ]]; then
  echo "[ERROR] Active alerts refresh failed (HTTP $UPDATED_ALERTS_CODE)." >&2
  cat "$UPDATED_ALERTS_BODY" >&2
  exit 1
fi
cat "$UPDATED_ALERTS_BODY" | jq '.'

echo "[DONE] Monitoring API smoke-test completed."
