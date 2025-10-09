#!/usr/bin/env bash
set -euo pipefail

# Simple smoke-test script for the CoreBank payment orchestrator HTTP API.
# Requires curl and jq. Override defaults via environment variables shown below.
#   BASE_URL (default: http://localhost:8080)
#   PAYMENT_API_USER (default: orchestrator)
#   PAYMENT_API_PASSWORD (default: changeme)
#   PAYMENT_REFERENCE (optional preset reference)
#   PAYMENT_AMOUNT (default: 150000.00)
#   PAYMENT_CURRENCY (default: VND)
#   PAYMENT_CHANNEL (default: MOBILE)
#   PAYMENT_DEBTOR (default: 1234567890)
#   PAYMENT_CREDITOR (default: 9988776655)

for bin in curl jq; do
  if ! command -v "$bin" >/dev/null 2>&1; then
    echo "[ERROR] Required dependency '$bin' is not installed or not on PATH." >&2
    exit 1
  fi
done

BASE_URL=${BASE_URL:-http://localhost:8080}
API_USER=${PAYMENT_API_USER:-orchestrator}
API_PASSWORD=${PAYMENT_API_PASSWORD:-changeme}
REFERENCE=${PAYMENT_REFERENCE:-PAY-$(date +%Y%m%d%H%M%S)-$RANDOM}
AMOUNT=${PAYMENT_AMOUNT:-150000.00}
CURRENCY=${PAYMENT_CURRENCY:-VND}
CHANNEL=${PAYMENT_CHANNEL:-MOBILE}
DEBTOR=${PAYMENT_DEBTOR:-1234567890}
CREDITOR=${PAYMENT_CREDITOR:-9988776655}

REQUEST_BODY=$(jq -n \
  --arg ref "$REFERENCE" \
  --argjson amount "${AMOUNT}" \
  --arg cur "$CURRENCY" \
  --arg channel "$CHANNEL" \
  --arg debtor "$DEBTOR" \
  --arg creditor "$CREDITOR" \
  '{
    reference: $ref,
    amount: $amount,
    currency: $cur,
    channel: $channel,
    debtorAccount: $debtor,
    creditorAccount: $creditor
  }')

POST_HEADERS=$(mktemp)
POST_BODY=$(mktemp)
GET_BODY=$(mktemp)
METRICS_BODY=$(mktemp)
trap 'rm -f "$POST_HEADERS" "$POST_BODY" "$GET_BODY" "$METRICS_BODY"' EXIT

echo "[INFO] Submitting payment reference '$REFERENCE' to $BASE_URL/api/v1/payments"
POST_STATUS=$(curl -sS -u "$API_USER:$API_PASSWORD" \
  -H 'Content-Type: application/json' \
  -d "$REQUEST_BODY" \
  -D "$POST_HEADERS" \
  -o "$POST_BODY" \
  -w '%{http_code}' \
  "$BASE_URL/api/v1/payments")

if [[ "$POST_STATUS" != "201" ]]; then
  echo "[ERROR] Payment initiation failed (HTTP $POST_STATUS). Response follows:" >&2
  cat "$POST_BODY" >&2
  exit 1
fi

echo "[INFO] Payment accepted (HTTP 201)."
LOCATION_HEADER=$(grep -i '^Location:' "$POST_HEADERS" | awk '{print $2}' | tr -d '\r')
if [[ -n "$LOCATION_HEADER" ]]; then
  echo "[INFO] Resource location: $LOCATION_HEADER"
fi

echo "[INFO] Payment response payload:"
cat "$POST_BODY" | jq '.'

FETCH_URL="$BASE_URL/api/v1/payments/$REFERENCE"
echo "[INFO] Fetching payment by reference via $FETCH_URL"
GET_STATUS=$(curl -sS -u "$API_USER:$API_PASSWORD" \
  -H 'Accept: application/json' \
  -o "$GET_BODY" \
  -w '%{http_code}' \
  "$FETCH_URL")

if [[ "$GET_STATUS" != "200" ]]; then
  echo "[ERROR] Lookup failed (HTTP $GET_STATUS). Response follows:" >&2
  cat "$GET_BODY" >&2
  exit 1
fi

cat "$GET_BODY" | jq '.'

echo "[INFO] Retrieving status histogram"
METRICS_STATUS=$(curl -sS -u "$API_USER:$API_PASSWORD" \
  -H 'Accept: application/json' \
  -o "$METRICS_BODY" \
  -w '%{http_code}' \
  "$BASE_URL/api/v1/payments/metrics/status-count")

if [[ "$METRICS_STATUS" == "200" ]]; then
  cat "$METRICS_BODY" | jq '.'
else
  echo "[WARN] Unable to load metrics (HTTP $METRICS_STATUS)"
  cat "$METRICS_BODY"
fi

echo "[NEXT] If Kafka integration is disabled (default), check application logs for lines containing '[LAB] Event' to confirm outbox publication."
