#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   sudo DEPLOY_USER=ubuntu APP_DIR=/home/ubuntu/S14P21A205 \
#   bash ops/scripts/bootstrap_ec2.sh
#
# What this script does (idempotent):
# 1) Install Docker runtime packages + Certbot
# 2) Create app directory
# 3) Prepare ACME webroot directory
# 4) Allow deploy user to run Docker commands

if [[ "${EUID}" -ne 0 ]]; then
  echo "[ERROR] Please run as root (use sudo)." >&2
  exit 1
fi

DEPLOY_USER="${DEPLOY_USER:-ubuntu}"
APP_DIR="${APP_DIR:-/home/${DEPLOY_USER}/S14P21A205}"

echo "[1/4] Install packages"
apt update
apt install -y certbot rsync

if command -v docker >/dev/null 2>&1; then
  echo "[INFO] Docker already installed. Skipping Docker package install."
else
  if apt-cache show docker-ce >/dev/null 2>&1; then
    apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  else
    apt install -y docker.io docker-compose-plugin
  fi
fi

echo "[2/4] Enable Docker service"
systemctl enable --now docker

echo "[3/4] Prepare app directory: ${APP_DIR}"
install -d -m 755 "${APP_DIR}"
install -d -m 755 /var/www/certbot
chown -R "${DEPLOY_USER}:${DEPLOY_USER}" "${APP_DIR}"
chown -R "${DEPLOY_USER}:${DEPLOY_USER}" /var/www/certbot

echo "[4/4] Add deploy user to docker group"
usermod -aG docker "${DEPLOY_USER}"

echo "[DONE] Bootstrap complete"
echo "Next:"
echo "  1) Upload deployment files to ${APP_DIR}"
echo "  2) Upload .env.prod to ${APP_DIR}/.env.prod"
echo "  3) Run: bash ${APP_DIR}/ops/scripts/setup_server_nginx.sh"
