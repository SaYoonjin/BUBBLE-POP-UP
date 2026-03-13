#!/usr/bin/env bash

set -euo pipefail

DEPLOY_USER="${DEPLOY_USER:-ubuntu}"
APP_DIR="${APP_DIR:-/home/${DEPLOY_USER}/S14P21A205-data}"

if ! command -v docker >/dev/null 2>&1; then
  apt-get update
  apt-get install -y docker.io docker-compose-plugin
fi

systemctl enable --now docker

mkdir -p "${APP_DIR}/data" "${APP_DIR}/spark/jobs"
chown -R "${DEPLOY_USER}:${DEPLOY_USER}" "${APP_DIR}"

usermod -aG docker "${DEPLOY_USER}" || true

echo "Data server bootstrap complete."
echo "Reconnect SSH once so docker group membership is applied."
