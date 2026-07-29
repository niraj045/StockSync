#!/usr/bin/env bash
# Restore a StockSync backup into a running Compose stack.
# This replaces the target database and uploaded-file volume contents.
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
backup_dir="${1:-}"
confirmation="${2:-}"
compose_file="${COMPOSE_FILE:-compose.production.yaml}"

[[ -n "$backup_dir" && -d "$backup_dir" ]] ||
  { printf 'Usage: %s BACKUP_DIRECTORY --confirm\n' "$0" >&2; exit 2; }
[[ "$confirmation" == "--confirm" ]] ||
  { printf '[ERROR] Restore requires --confirm because target data is replaced.\n' >&2; exit 2; }

backup_dir="$(cd "$backup_dir" && pwd)"
[[ -f "$backup_dir/database.sql.gz" && -f "$backup_dir/uploads.tar.gz" && -f "$backup_dir/SHA256SUMS" ]] ||
  { printf '[ERROR] Backup set is incomplete.\n' >&2; exit 1; }

(cd "$backup_dir" && sha256sum --check SHA256SUMS)

set -a
# shellcheck disable=SC1091
source "$ROOT_DIR/${ENV_FILE:-.env.production}"
set +a

compose=(docker compose --project-name stocksync-production --project-directory "$ROOT_DIR" -f "$ROOT_DIR/$compose_file")

printf '[INFO] Stopping backend and frontend during restore...\n'
"${compose[@]}" stop frontend backend

printf '[INFO] Recreating database %s...\n' "$DB_NAME"
"${compose[@]}" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
  mysql -uroot -e "DROP DATABASE IF EXISTS \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'%'; FLUSH PRIVILEGES;"
gzip -dc "$backup_dir/database.sql.gz" |
  "${compose[@]}" exec -T -e MYSQL_PWD="$DB_PASSWORD" mysql \
    mysql -u"$DB_USER" "$DB_NAME"

printf '[INFO] Restoring uploaded files...\n'
"${compose[@]}" run --rm --no-deps \
  -v "$backup_dir:/restore:ro" \
  --entrypoint /bin/sh backup \
  -c 'rm -rf /source/uploads/* && tar -xzf /restore/uploads.tar.gz -C /source'

"${compose[@]}" up -d --wait --wait-timeout 180 backend frontend
printf '[OK] Restore completed and application services are healthy.\n'
