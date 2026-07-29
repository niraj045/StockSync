#!/usr/bin/env bash
# Production-like local deployment using Docker Compose.
# Usage: ./deploy-local.sh [start|status|logs|restart|stop]
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$ROOT_DIR/.env"
COMPOSE=(docker compose --project-directory "$ROOT_DIR")

fail() {
  printf '[ERROR] %s\n' "$*" >&2
  exit 1
}

load_environment() {
  [[ -f "$ENV_FILE" ]] ||
    fail ".env is missing. Copy .env.example to .env and set local passwords."

  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a

  : "${APP_PORT:?APP_PORT is required in .env}"
  : "${DB_HOST_PORT:?DB_HOST_PORT is required in .env}"
  : "${DB_NAME:?DB_NAME is required in .env}"
  : "${DB_USER:?DB_USER is required in .env}"
  : "${DB_PASSWORD:?DB_PASSWORD is required in .env}"
  : "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required in .env}"

  [[ "$DB_PASSWORD" != replace_with_* ]] ||
    fail "Replace the example DB_PASSWORD in .env."
  [[ "$MYSQL_ROOT_PASSWORD" != replace_with_* ]] ||
    fail "Replace the example MYSQL_ROOT_PASSWORD in .env."
}

seed_local_admin() {
  local admin_hash='$2b$10$BuxOBAjd5fFnARIM0afB1uYK6h6vTNcfR/fKbXkrluOepgFcmLyT2'

  "${COMPOSE[@]}" exec -T \
    -e MYSQL_PWD="$DB_PASSWORD" mysql \
    mysql -u "$DB_USER" "$DB_NAME" <<SQL
INSERT INTO users
  (full_name, username, email, password_hash, active, created_by, updated_by)
SELECT 'Admin User', 'admin', 'admin@stocksync.local', '$admin_hash',
       TRUE, 'local-deployment', 'local-deployment'
WHERE NOT EXISTS (
  SELECT 1
  FROM users
  WHERE LOWER(username) = 'admin'
     OR LOWER(email) = 'admin@stocksync.local'
);
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT id, 'ROLE_ADMIN'
FROM users
WHERE LOWER(username) = 'admin';
SQL
}

verify_deployment() {
  curl --silent --show-error --fail \
    "http://127.0.0.1:${APP_PORT}/health" >/dev/null
  curl --silent --show-error --fail \
    "http://127.0.0.1:${APP_PORT}/api/v1/health" >/dev/null

  "${COMPOSE[@]}" exec -T \
    -e MYSQL_PWD="$DB_PASSWORD" mysql \
    mysql -u "$DB_USER" "$DB_NAME" \
    --batch --skip-column-names \
    -e "SELECT COUNT(*) FROM flyway_schema_history;" >/dev/null
}

show_status() {
  "${COMPOSE[@]}" ps
  printf '\nApp:      http://127.0.0.1:%s/login\n' "$APP_PORT"
  printf 'Database: 127.0.0.1:%s (%s)\n' "$DB_HOST_PORT" "$DB_NAME"
}

start_deployment() {
  printf '[INFO] Building and starting the local deployment...\n'
  "${COMPOSE[@]}" up --build --detach --wait --wait-timeout 180
  seed_local_admin
  verify_deployment
  printf '[OK] Local deployment is healthy.\n'
  show_status
  printf '\nLocal login: admin / Password123\n'
}

load_environment

case "${1:-start}" in
  start)
    start_deployment
    ;;
  status)
    show_status
    ;;
  logs)
    "${COMPOSE[@]}" logs --follow --tail=200
    ;;
  restart)
    "${COMPOSE[@]}" down
    start_deployment
    ;;
  stop)
    "${COMPOSE[@]}" down
    printf '[OK] Local deployment stopped; database volumes were preserved.\n'
    ;;
  *)
    fail "Usage: ./deploy-local.sh [start|status|logs|restart|stop]"
    ;;
esac
