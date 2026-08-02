#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env}"
COMPOSE=(docker compose --project-directory "$DEPLOY_DIR" --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.prod.yml")
CERTBOT_IMAGE="${CERTBOT_IMAGE:-certbot/certbot:latest}"

mkdir -p "$DEPLOY_DIR/certbot/conf" "$DEPLOY_DIR/certbot/www"
docker run --rm \
  -v "$DEPLOY_DIR/certbot/conf:/etc/letsencrypt" \
  -v "$DEPLOY_DIR/certbot/www:/var/www/certbot" \
  "$CERTBOT_IMAGE" renew --webroot --webroot-path /var/www/certbot --quiet
"${COMPOSE[@]}" exec -T frontend nginx -s reload
echo "[OK] Certificate renewal check completed and Nginx reloaded."
