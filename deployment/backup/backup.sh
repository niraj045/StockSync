#!/usr/bin/env bash
set -Eeuo pipefail

: "${DB_HOST:?DB_HOST is required}"
: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"

interval="${BACKUP_INTERVAL_SECONDS:-86400}"
retention="${BACKUP_RETENTION_DAYS:-7}"

create_backup() {
  local timestamp destination temporary
  timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
  destination="/backups/$timestamp"
  temporary="/backups/.${timestamp}.tmp"
  mkdir -p "$temporary"

  MYSQL_PWD="$DB_PASSWORD" mysqldump \
    --host="$DB_HOST" \
    --user="$DB_USER" \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    --events \
    --no-tablespaces \
    --set-gtid-purged=OFF \
    "$DB_NAME" | gzip -9 >"$temporary/database.sql.gz"

  tar -C /source -czf "$temporary/uploads.tar.gz" uploads
  (
    cd "$temporary"
    sha256sum database.sql.gz uploads.tar.gz >SHA256SUMS
  )

  mv "$temporary" "$destination"
  cp -a "$destination" /offsite/
  find /backups -mindepth 1 -maxdepth 1 -type d -mtime "+$retention" -exec rm -rf -- {} +
  printf '[OK] Backup created: %s\n' "$destination"
}

while true; do
  create_backup
  [[ "${RUN_ONCE:-false}" == "true" ]] && exit 0
  sleep "$interval"
done
