#!/usr/bin/env bash
# One-command StockSync demo setup with the populated E2E database.
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$ROOT_DIR/friend-handoff/compose.yaml"
APP_PORT="${FRIEND_APP_PORT:-8088}"
DB_PORT="${FRIEND_DB_PORT:-3309}"

info() { printf '[StockSync] %s\n' "$*"; }
fail() { printf '[StockSync] ERROR: %s\n' "$*" >&2; exit 1; }

command -v docker >/dev/null 2>&1 || fail "Docker is required: https://docs.docker.com/get-docker/"
docker compose version >/dev/null 2>&1 || fail "Docker Compose v2 is required."

case "${1:-start}" in
  start)
    info "Building and starting the populated friend-testing environment..."
    FRIEND_APP_PORT="$APP_PORT" FRIEND_DB_PORT="$DB_PORT" \
      docker compose -f "$COMPOSE_FILE" up --build -d --wait
    printf '\nStockSync is ready.\n'
    printf '  App:      http://127.0.0.1:%s/login\n' "$APP_PORT"
    printf '  Username: admin\n'
    printf '  Password: Password123\n'
    printf '  MySQL:    127.0.0.1:%s\n' "$DB_PORT"
    printf '  Database: shuttering_inventory_e2e\n'
    printf '  DB user:  inventory_user\n'
    printf '  DB pass:  inventory_password\n'
    printf '\nStop with: ./setup-friend.sh --stop\n'
    ;;
  --status|status)
    docker compose -f "$COMPOSE_FILE" ps
    ;;
  --stop|stop)
    docker compose -f "$COMPOSE_FILE" down
    info "Stopped. The populated database volume was preserved."
    ;;
  --reset|reset)
    info "Resetting only the stocksync-friend Docker database and containers..."
    docker compose -f "$COMPOSE_FILE" down --volumes --remove-orphans
    FRIEND_APP_PORT="$APP_PORT" FRIEND_DB_PORT="$DB_PORT" \
      docker compose -f "$COMPOSE_FILE" up --build -d --wait
    info "Reset complete: http://127.0.0.1:$APP_PORT/login"
    ;;
  *)
    printf 'Usage: ./setup-friend.sh [start|--status|--stop|--reset]\n' >&2
    exit 2
    ;;
esac
