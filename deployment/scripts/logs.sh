#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env}"
TAIL_LINES="${1:-200}"

[[ "$TAIL_LINES" =~ ^[0-9]+$ ]] || { printf '[ERROR] Tail line count must be numeric.\n' >&2; exit 2; }
[[ -f "$ENV_FILE" ]] || { printf '[ERROR] Missing %s\n' "$ENV_FILE" >&2; exit 1; }

docker compose --project-directory "$DEPLOY_DIR" --env-file "$ENV_FILE" \
  -f "$DEPLOY_DIR/docker-compose.prod.yml" logs --tail="$TAIL_LINES" mysql backend frontend
