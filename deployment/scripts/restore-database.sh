#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env}"
BACKUP_DIR="$DEPLOY_DIR/storage/backups"
filename="${1:-}"
COMPOSE=(docker compose --project-directory "$DEPLOY_DIR" --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.prod.yml")

[[ -n "$filename" ]] || { printf 'Usage: %s BACKUP_FILENAME\n' "$0" >&2; exit 2; }
[[ "$filename" == "$(basename "$filename")" ]] || { printf '[ERROR] Supply a filename from deployment/storage/backups only.\n' >&2; exit 2; }
[[ "$filename" == stocksync-*.sql.gz ]] || { printf '[ERROR] Invalid StockSync backup filename.\n' >&2; exit 2; }
[[ -f "$ENV_FILE" ]] || { printf '[ERROR] Missing %s\n' "$ENV_FILE" >&2; exit 1; }

backup="$BACKUP_DIR/$filename"
[[ -f "$backup" ]] || { printf '[ERROR] Backup not found: %s\n' "$backup" >&2; exit 1; }
gzip -t "$backup"

printf 'This will replace only the StockSync database shuttering_inventory.\n'
read -r -p 'Type RESTORE to continue: ' confirmation
[[ "$confirmation" == "RESTORE" ]] || { printf '[INFO] Restore cancelled.\n'; exit 1; }

"${COMPOSE[@]}" stop frontend backend

"${COMPOSE[@]}" exec -T mysql sh -ec '
  test "$MYSQL_DATABASE" = "shuttering_inventory"
  config="$(mktemp)"
  trap '\''rm -f "$config"'\'' EXIT
  umask 077
  printf "[client]\nuser=root\npassword=%s\nhost=127.0.0.1\n" "$MYSQL_ROOT_PASSWORD" >"$config"
  mysql --defaults-extra-file="$config" -e "DROP DATABASE IF EXISTS \`shuttering_inventory\`; CREATE DATABASE \`shuttering_inventory\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON \`shuttering_inventory\`.* TO '\''stocksync_app'\''@'\''%'\''; FLUSH PRIVILEGES;"
'

if ! gzip -dc "$backup" | "${COMPOSE[@]}" exec -T mysql sh -ec '
  config="$(mktemp)"
  trap '\''rm -f "$config"'\'' EXIT
  umask 077
  printf "[client]\nuser=root\npassword=%s\nhost=127.0.0.1\n" "$MYSQL_ROOT_PASSWORD" >"$config"
  mysql --defaults-extra-file="$config" shuttering_inventory
'; then
  printf '[ERROR] Restore failed; backend and frontend remain stopped for inspection.\n' >&2
  exit 1
fi

"${COMPOSE[@]}" up -d backend frontend
printf '[OK] Restored %s into the independent StockSync database.\n' "$filename"
printf '[INFO] Run deployment/scripts/status.sh and confirm both services are healthy.\n'
