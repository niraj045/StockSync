#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env}"
COMPOSE=(docker compose --project-directory "$DEPLOY_DIR" --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.prod.yml")

[[ -f "$ENV_FILE" ]] || { printf '[ERROR] Missing %s\n' "$ENV_FILE" >&2; exit 1; }

printf '\n== Compose services ==\n'
"${COMPOSE[@]}" ps

printf '\n== Container health ==\n'
for service in mysql backend frontend; do
  container_id="$("${COMPOSE[@]}" ps -q "$service")"
  if [[ -n "$container_id" ]]; then
    docker inspect --format "$service: status={{.State.Status}} health={{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}}" "$container_id"
  else
    printf '%s: not created\n' "$service"
  fi
done

printf '\n== RAM ==\n'
free -h
printf '\n== Disk ==\n'
df -h /opt/stocksync
printf '\n== Docker disk usage ==\n'
docker system df
