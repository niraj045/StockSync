#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env}"
COMPOSE=(docker compose --project-directory "$DEPLOY_DIR" --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.prod.yml")
CERTBOT_IMAGE="${CERTBOT_IMAGE:-certbot/certbot:latest}"

[[ -f "$ENV_FILE" ]] || { echo "[ERROR] Missing $ENV_FILE" >&2; exit 1; }
domain="$(sed -n 's/^APP_DOMAIN=//p' "$ENV_FILE" | tail -n 1)"
[[ -n "$domain" ]] || { echo "[ERROR] APP_DOMAIN is required" >&2; exit 1; }
mkdir -p "$DEPLOY_DIR/certbot/conf" "$DEPLOY_DIR/certbot/www"

email_args=(--register-unsafely-without-email)
if [[ -n "${LETSENCRYPT_EMAIL:-}" ]]; then
  email_args=(--email "$LETSENCRYPT_EMAIL" --no-eff-email)
fi

echo "[INFO] Stopping the HTTP frontend briefly to issue the first certificate..."
"${COMPOSE[@]}" stop frontend
restore_frontend() { "${COMPOSE[@]}" up -d --no-deps frontend >/dev/null 2>&1 || true; }
trap restore_frontend EXIT

docker run --rm --network host \
  -v "$DEPLOY_DIR/certbot/conf:/etc/letsencrypt" \
  "$CERTBOT_IMAGE" certonly --standalone --non-interactive --agree-tos \
  "${email_args[@]}" -d "$domain"

trap - EXIT
"${COMPOSE[@]}" up -d --no-deps frontend
echo "[OK] Certificate issued for $domain."
