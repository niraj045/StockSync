#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_ROOT="${STOCKSYNC_DEPLOY_ROOT:-/opt/stocksync}"
DEPLOY_USER="${STOCKSYNC_DEPLOY_USER:-stocksync}"
MIN_RAM_KB=$((3 * 1024 * 1024))
MIN_DISK_KB=$((10 * 1024 * 1024))

fail() {
  printf '[ERROR] %s\n' "$*" >&2
  exit 1
}

command -v docker >/dev/null 2>&1 || fail "Docker Engine is not installed or not on PATH."
docker compose version >/dev/null 2>&1 || fail "Docker Compose v2 is not installed."
docker info >/dev/null 2>&1 || fail "Docker Engine is not running or the current user cannot access it."
id "$DEPLOY_USER" >/dev/null 2>&1 || fail "Deployment user '$DEPLOY_USER' does not exist."

ram_kb="$(awk '/MemTotal/ {print $2}' /proc/meminfo)"
(( ram_kb >= MIN_RAM_KB )) || fail "At least 3 GB RAM is required; detected $((ram_kb / 1024)) MB."

parent="$(dirname "$DEPLOY_ROOT")"
disk_kb="$(df -Pk "$parent" | awk 'NR==2 {print $4}')"
(( disk_kb >= MIN_DISK_KB )) || fail "At least 10 GB free disk is required below $parent."

if [[ "${EUID}" -eq 0 ]]; then
  install -d -o "$DEPLOY_USER" -g "$DEPLOY_USER" \
    "$DEPLOY_ROOT" \
    "$DEPLOY_ROOT/deployment/storage/uploads" \
    "$DEPLOY_ROOT/deployment/storage/documents" \
    "$DEPLOY_ROOT/deployment/storage/reports" \
    "$DEPLOY_ROOT/deployment/storage/backups"
  chown -R "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_ROOT"
elif [[ "$(id -un)" == "$DEPLOY_USER" ]]; then
  mkdir -p \
    "$DEPLOY_ROOT/deployment/storage/uploads" \
    "$DEPLOY_ROOT/deployment/storage/documents" \
    "$DEPLOY_ROOT/deployment/storage/reports" \
    "$DEPLOY_ROOT/deployment/storage/backups"
else
  fail "Run this script as root or as '$DEPLOY_USER'."
fi

chmod 750 "$DEPLOY_ROOT/deployment/storage" \
  "$DEPLOY_ROOT/deployment/storage/uploads" \
  "$DEPLOY_ROOT/deployment/storage/documents" \
  "$DEPLOY_ROOT/deployment/storage/reports" \
  "$DEPLOY_ROOT/deployment/storage/backups"

printf '[OK] Docker Engine and Compose are available.\n'
printf '[OK] RAM: %s MB; free disk: %s GB.\n' "$((ram_kb / 1024))" "$((disk_kb / 1024 / 1024))"
printf '[OK] StockSync directories are owned by %s below %s.\n' "$DEPLOY_USER" "$DEPLOY_ROOT"
printf '[INFO] Set APP_UID=%s and APP_GID=%s in deployment/.env.\n' "$(id -u "$DEPLOY_USER")" "$(id -g "$DEPLOY_USER")"
