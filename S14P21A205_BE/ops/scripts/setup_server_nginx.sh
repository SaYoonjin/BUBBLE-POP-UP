#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   DOMAIN=<YOUR_API_DOMAIN> APP_DIR=/home/ubuntu/S14P21A205 bash ops/scripts/setup_server_nginx.sh
# Preconditions:
#   1) Run on Ubuntu server
#   2) This repo exists at APP_DIR
#   3) APP_DIR/build/libs/app.jar exists
#   4) APP_DIR/.env.prod exists
#   5) docker + docker compose plugin + certbot are installed

DOMAIN="${DOMAIN:-}"
APP_DIR="${APP_DIR:-/home/ubuntu/S14P21A205}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"
CERTBOT_WEBROOT="${CERTBOT_WEBROOT:-/var/www/certbot}"
COMPOSE_BIN="${COMPOSE_BIN:-docker compose}"

if [[ -z "${DOMAIN}" ]]; then
  echo "[ERROR] DOMAIN is required."
  exit 1
fi

if [[ ! -f "$APP_DIR/build/libs/app.jar" ]]; then
  echo "[ERROR] Missing $APP_DIR/build/libs/app.jar"
  exit 1
fi

if [[ ! -f "$APP_DIR/.env.prod" ]]; then
  echo "[ERROR] Missing $APP_DIR/.env.prod"
  exit 1
fi

if [[ ! -f "$APP_DIR/docker-compose.yml" ]]; then
  echo "[ERROR] Missing $APP_DIR/docker-compose.yml"
  exit 1
fi

if ! grep -q '^NGINX_SERVER_NAME=' "$APP_DIR/.env.prod"; then
  echo "[ERROR] Missing NGINX_SERVER_NAME in $APP_DIR/.env.prod"
  exit 1
fi

echo "[1/5] Prepare webroot"
sudo install -d -m 755 "${CERTBOT_WEBROOT}"
sudo chown -R "$(id -un)":"$(id -gn)" "${CERTBOT_WEBROOT}"

echo "[2/5] Stop legacy host services if present"
sudo systemctl disable --now nginx >/dev/null 2>&1 || true
sudo systemctl disable --now S14P21A205 >/dev/null 2>&1 || true

echo "[3/5] Start containers (HTTP mode if certificate is absent)"
cd "$APP_DIR"
${COMPOSE_BIN} up -d --build --remove-orphans

echo "[4/5] Issue or renew certificate"
if [[ -n "${CERTBOT_EMAIL}" ]]; then
  sudo certbot certonly --webroot -w "${CERTBOT_WEBROOT}" -d "$DOMAIN" \
    --non-interactive --agree-tos --email "${CERTBOT_EMAIL}"
else
  sudo certbot certonly --webroot -w "${CERTBOT_WEBROOT}" -d "$DOMAIN"
fi

echo "[5/5] Restart nginx container to pick up HTTPS config"
${COMPOSE_BIN} up -d nginx

echo "[DONE] Done"
echo "Check: https://$DOMAIN"
