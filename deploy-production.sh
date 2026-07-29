#!/usr/bin/env bash
# StockSync single-node production deployment manager.
# Usage: ./deploy-production.sh [validate|start|status|logs|backup|stop]
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.production}"
COMPOSE=(docker compose --project-name stocksync-production --project-directory "$ROOT_DIR" --env-file "$ENV_FILE" -f "$ROOT_DIR/compose.production.yaml")

fail() {
  printf '[ERROR] %s\n' "$*" >&2
  exit 1
}

load_environment() {
  [[ -f "$ENV_FILE" ]] ||
    fail "$ENV_FILE is missing. Copy .env.production.example and set production values."
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a

  for variable in SERVER_NAME DB_NAME DB_USER DB_PASSWORD MYSQL_ROOT_PASSWORD TLS_CERT_PATH TLS_KEY_PATH; do
    [[ -n "${!variable:-}" ]] || fail "$variable is required in $ENV_FILE."
  done
  [[ "$DB_PASSWORD" != replace_with_* && "$MYSQL_ROOT_PASSWORD" != replace_with_* ]] ||
    fail "Example database passwords are not allowed."
  [[ ${#DB_PASSWORD} -ge 20 && ${#MYSQL_ROOT_PASSWORD} -ge 20 ]] ||
    fail "Production database passwords must be at least 20 characters."
  [[ -f "$ROOT_DIR/$TLS_CERT_PATH" || -f "$TLS_CERT_PATH" ]] ||
    fail "TLS certificate not found at $TLS_CERT_PATH."
  [[ -f "$ROOT_DIR/$TLS_KEY_PATH" || -f "$TLS_KEY_PATH" ]] ||
    fail "TLS private key not found at $TLS_KEY_PATH."
  [[ "${SESSION_COOKIE_SECURE:-true}" == "true" ]] ||
    fail "SESSION_COOKIE_SECURE must be true in production."
}

validate() {
  load_environment
  "${COMPOSE[@]}" config --quiet
  printf '[OK] Production environment and Compose configuration are valid.\n'
}

start() {
  validate
  "${COMPOSE[@]}" up --build --detach --wait --wait-timeout 240
  printf '[OK] Production deployment is healthy at https://%s\n' "$SERVER_NAME"
}

load_environment
case "${1:-validate}" in
  validate) validate ;;
  start) start ;;
  status) "${COMPOSE[@]}" ps ;;
  logs) "${COMPOSE[@]}" logs --follow --tail=200 ;;
  backup)
    "${COMPOSE[@]}" run --rm -e RUN_ONCE=true backup
    ;;
  stop)
    "${COMPOSE[@]}" down
    printf '[OK] Services stopped; persistent volumes were preserved.\n'
    ;;
  *) fail "Usage: ./deploy-production.sh [validate|start|status|logs|backup|stop]" ;;
esac
