#!/usr/bin/env bash
set -euo pipefail

# Smoke-test for the investigation service API.
# Requires curl and jq.
# Environment variables:
#   INVESTIGATION_BASE_URL (default: http://localhost:8082)
#   INVESTIGATION_USER (default: investigator)
#   INVESTIGATION_PASSWORD (default: changeme)
#   INVESTIGATION_REFERENCE (default: REF-INV-$(timestamp))
#   INVESTIGATION_NOTE (default: "Initial inquiry")

for bin in curl jq; do
  if ! command -v "$bin" >/dev/null 2>&1; then
    echo "[ERROR] Required dependency '$bin' is not installed or not on PATH." >&2
    exit 1
  fi
done

BASE_URL=${INVESTIGATION_BASE_URL:-http://localhost:8082}
USER=${INVESTIGATION_USER:-investigator}
PASSWORD=${INVESTIGATION_PASSWORD:-changeme}
REFERENCE=${INVESTIGATION_REFERENCE:-REF-INV-$(date +%Y%m%d%H%M%S)}
INITIAL_NOTE=${INVESTIGATION_NOTE:-"Initial inquiry"}

CREATE_BODY=$(mktemp)
GET_BODY=$(mktemp)
STATUS_BODY=$(mktemp)
trap 'rm -f "$CREATE_BODY" "$GET_BODY" "$STATUS_BODY"' EXIT

REQUEST_PAYLOAD=$(jq -n \
  --arg ref "$REFERENCE" \
  --arg reporter "contact-center" \
  --arg note "$INITIAL_NOTE" \
  '{reference: $ref, reportedBy: $reporter, initialNote: $note}')

CREATE_STATUS=$(curl -sS -u "$USER:$PASSWORD" \
  -H 'Content-Type: application/json' \
  -d "$REQUEST_PAYLOAD" \
  -o "$CREATE_BODY" \
  -w '%{http_code}' \
  "$BASE_URL/api/v1/investigations")

if [[ "$CREATE_STATUS" != "201" ]]; then
  echo "[ERROR] Investigation creation failed (HTTP $CREATE_STATUS)." >&2
  cat "$CREATE_BODY" >&2
  exit 1
fi

CASE_ID=$(jq -r '.caseId' "$CREATE_BODY")

echo "[INFO] Created investigation caseId=$CASE_ID reference=$REFERENCE"
cat "$CREATE_BODY" | jq '.'

curl -sS -u "$USER:$PASSWORD" \
  -H 'Accept: application/json' \
  -o "$GET_BODY" \
  "$BASE_URL/api/v1/investigations/$CASE_ID"
cat "$GET_BODY" | jq '.'

STATUS_PAYLOAD=$(jq -n '{status:"IN_PROGRESS", note:"Assigned to tier2"}')
UPDATE_STATUS=$(curl -sS -u "$USER:$PASSWORD" \
  -H 'Content-Type: application/json' \
  -d "$STATUS_PAYLOAD" \
  -o "$STATUS_BODY" \
  -w '%{http_code}' \
  "$BASE_URL/api/v1/investigations/$CASE_ID/status")

if [[ "$UPDATE_STATUS" != "200" ]]; then
  echo "[ERROR] Status update failed (HTTP $UPDATE_STATUS)." >&2
  cat "$STATUS_BODY" >&2
  exit 1
fi

cat "$STATUS_BODY" | jq '.'

echo "[INFO] Retrieving status-count metrics"
curl -sS -u "$USER:$PASSWORD" \
  -H 'Accept: application/json' \
  "$BASE_URL/api/v1/investigations/metrics/status-count" | jq '.'

echo "[DONE] Investigation service smoke-test completed."
