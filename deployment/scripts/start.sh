#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env}"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.prod.yml"
COMPOSE=(docker compose --project-directory "$DEPLOY_DIR" --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

fail() {
  printf '[ERROR] %s\n' "$*" >&2
  exit 1
}

env_value() {
  sed -n "s/^$1=//p" "$ENV_FILE" | tail -n 1
}

[[ -f "$ENV_FILE" ]] || fail "Missing $ENV_FILE. Copy .env.example to .env and set strong passwords."
command -v docker >/dev/null 2>&1 || fail "Docker is not installed or not on PATH."
docker compose version >/dev/null 2>&1 || fail "Docker Compose v2 is required."
docker info >/dev/null 2>&1 || fail "Docker Engine is not running or is not accessible."

for variable in MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD MYSQL_ROOT_PASSWORD APP_DOMAIN APP_ORIGIN; do
  [[ -n "$(env_value "$variable")" ]] || fail "$variable must be set in $ENV_FILE."
done
[[ "$(env_value MYSQL_DATABASE)" == "shuttering_inventory" ]] || fail "MYSQL_DATABASE must be shuttering_inventory."
[[ "$(env_value MYSQL_USER)" == "stocksync_app" ]] || fail "MYSQL_USER must be stocksync_app."
mysql_password="$(env_value MYSQL_PASSWORD)"
mysql_root_password="$(env_value MYSQL_ROOT_PASSWORD)"
(( ${#mysql_password} >= 20 )) || fail "MYSQL_PASSWORD must contain at least 20 characters."
(( ${#mysql_root_password} >= 20 )) || fail "MYSQL_ROOT_PASSWORD must contain at least 20 characters."

"${COMPOSE[@]}" config --quiet
printf '[INFO] Building production images...\n'
"${COMPOSE[@]}" build
printf '[INFO] Starting StockSync...\n'
"${COMPOSE[@]}" up -d

deadline=$((SECONDS + 300))
for service in mysql backend frontend; do
  while true; do
    container_id="$("${COMPOSE[@]}" ps -q "$service")"
    [[ -n "$container_id" ]] || fail "$service container was not created."
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id")"
    [[ "$health" == "healthy" ]] && break
    [[ "$health" == "unhealthy" ]] && {
      "${COMPOSE[@]}" logs --tail=100 "$service" >&2
      fail "$service became unhealthy."
    }
    (( SECONDS < deadline )) || {
      "${COMPOSE[@]}" logs --tail=100 "$service" >&2
      fail "Timed out waiting for $service health."
    }
    sleep 5
  done
  printf '[OK] %s is healthy.\n' "$service"
done

"${COMPOSE[@]}" ps
printf '[OK] StockSync is available at %s\n' "$(env_value APP_ORIGIN)"
