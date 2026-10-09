#!/bin/sh
# Smoke test for the local Docker Compose stack: verifies every service is up AND that the
# services can talk to each other. Usage: scripts/smoke-test.sh   (after `docker compose up -d --wait`)
set -eu

FRONTEND="http://127.0.0.1:${FRONTEND_PORT:-4200}"
BACKEND="http://127.0.0.1:${BACKEND_PORT:-8080}"
ML="http://127.0.0.1:${ML_PORT:-8000}"

fail() { echo "FAIL: $1" >&2; exit 1; }
pass() { echo "PASS: $1"; }

# check <description> <url> <expected-http-status> [expected-body-substring]
check() {
  desc=$1; url=$2; want=$3; needle=${4:-}
  i=0
  while :; do
    code=$(curl -s -o /tmp/smoke-body -w '%{http_code}' --max-time 5 "$url" || true)
    if [ "$code" = "$want" ] && { [ -z "$needle" ] || grep -q "$needle" /tmp/smoke-body; }; then
      pass "$desc"; return 0
    fi
    i=$((i + 1))
    [ "$i" -ge 20 ] && fail "$desc (got HTTP $code from $url, wanted $want${needle:+ containing $needle})"
    sleep 3
  done
}

check "ml-service health"                              "$ML/health"                          200 '"status":"UP"'
check "backend readiness (PostgreSQL + Redis reachable)" "$BACKEND/actuator/health/readiness" 200 '"status":"UP"'
check "backend liveness"                               "$BACKEND/actuator/health/liveness"   200 '"status":"UP"'
check "frontend serves the app"                        "$FRONTEND/"                          200 '<app-root'
check "frontend -> backend proxy (readiness)"          "$FRONTEND/actuator/health/readiness" 200 '"status":"UP"'
check "API is deny-by-default through the frontend"    "$FRONTEND/api/v1/orders"             401 'UNAUTHENTICATED'
check "unexposed actuator endpoints are not reachable" "$BACKEND/actuator/env"               401

echo "All smoke checks passed."
