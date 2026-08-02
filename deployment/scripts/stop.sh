#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env}"
[[ -f "$ENV_FILE" ]] || { printf '[ERROR] Missing %s\n' "$ENV_FILE" >&2; exit 1; }

docker compose --project-directory "$DEPLOY_DIR" --env-file "$ENV_FILE" \
  -f "$DEPLOY_DIR/docker-compose.prod.yml" down --remove-orphans
printf '[OK] StockSync stopped. MySQL and file-storage data were preserved.\n'
