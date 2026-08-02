#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env}"
BACKUP_DIR="$DEPLOY_DIR/storage/backups"
COMPOSE=(docker compose --project-directory "$DEPLOY_DIR" --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.prod.yml")

[[ -f "$ENV_FILE" ]] || { printf '[ERROR] Missing %s\n' "$ENV_FILE" >&2; exit 1; }
mkdir -p "$BACKUP_DIR"

timestamp="$(date +%Y%m%d-%H%M%S)"
destination="$BACKUP_DIR/stocksync-$timestamp.sql.gz"
temporary="$destination.tmp"

cleanup() {
  rm -f -- "$temporary"
}
trap cleanup EXIT

if ! "${COMPOSE[@]}" exec -T mysql sh -ec '
  test "$MYSQL_DATABASE" = "shuttering_inventory"
  config="$(mktemp)"
  trap '\''rm -f "$config"'\'' EXIT
  umask 077
  printf "[client]\nuser=%s\npassword=%s\nhost=127.0.0.1\n" "$MYSQL_USER" "$MYSQL_PASSWORD" >"$config"
  mysqldump --defaults-extra-file="$config" --single-transaction --quick --routines --triggers --events --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE"
' | gzip -9 >"$temporary"; then
  printf '[ERROR] Database backup failed.\n' >&2
  exit 1
fi

[[ -s "$temporary" ]] || { printf '[ERROR] Backup output is empty.\n' >&2; exit 1; }
gzip -t "$temporary"
mv "$temporary" "$destination"
trap - EXIT

retention="$(sed -n 's/^BACKUP_RETENTION_DAYS=//p' "$ENV_FILE" | tail -n 1)"
retention="${retention:-14}"
[[ "$retention" =~ ^[0-9]+$ ]] || { printf '[ERROR] BACKUP_RETENTION_DAYS must be numeric.\n' >&2; exit 1; }
find "$BACKUP_DIR" -maxdepth 1 -type f -name 'stocksync-*.sql.gz' -mtime "+$retention" -delete

printf '[OK] Database backup created: %s\n' "$destination"
