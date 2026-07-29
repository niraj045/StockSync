#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
result_dir="$ROOT_DIR/e2e-data/capacity/$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$result_dir"

set -a
# shellcheck disable=SC1091
source "$ROOT_DIR/.env"
set +a

docker stats --no-stream \
  --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.PIDs}}' \
  >"$result_dir/container-stats-before.csv"

docker run --rm --network stocksync_stocksync_internal \
  -e BASE_URL=http://frontend \
  -e VUS="${VUS:-8}" \
  -e DURATION="${DURATION:-2m}" \
  -v "$ROOT_DIR/deployment/loadtest:/scripts:ro" \
  grafana/k6:0.54.0 run \
  --summary-export=/tmp/summary.json \
  /scripts/k6-smoke.js | tee "$result_dir/k6-output.txt"

docker stats --no-stream \
  --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.PIDs}}' \
  >"$result_dir/container-stats-after.csv"
docker compose ps --format json >"$result_dir/compose-status.json"
docker compose logs --since 10m >"$result_dir/compose-logs.txt"

printf '[OK] Capacity-test evidence saved in %s\n' "$result_dir"
