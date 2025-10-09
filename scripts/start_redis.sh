#!/usr/bin/env bash
set -euo pipefail

# Start a local Redis instance via Docker for development/testing.
# Requires Docker Engine.

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] docker is required on PATH." >&2
  exit 1
fi

docker run --rm -d \
  --name corebank-redis \
  -p 6379:6379 \
  redis:7-alpine

echo "[INFO] Redis is running on localhost:6379 (container: corebank-redis)."
echo "[INFO] Stop with: docker stop corebank-redis"
