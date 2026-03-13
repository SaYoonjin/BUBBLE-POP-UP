#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "[ERROR] Please run as root (use sudo)." >&2
  exit 1
fi

DEPLOY_USER="${DEPLOY_USER:-ubuntu}"
APP_DIR="${APP_DIR:-/home/${DEPLOY_USER}/S14P21A205-data}"

echo "[1/4] Install packages"
apt update
apt install -y rsync ca-certificates curl

if command -v docker >/dev/null 2>&1; then
  echo "[INFO] Docker already installed. Skipping Docker package install."
else
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo ${UBUNTU_CODENAME:-$VERSION_CODENAME}) stable" > /etc/apt/sources.list.d/docker.list
  apt update
  apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

echo "[2/4] Enable Docker service"
systemctl enable --now docker

echo "[3/4] Prepare app directory: ${APP_DIR}"
install -d -m 755 "${APP_DIR}/data"
install -d -m 755 "${APP_DIR}/spark/jobs"
chown -R "${DEPLOY_USER}:${DEPLOY_USER}" "${APP_DIR}"

echo "[4/4] Add deploy user to docker group"
usermod -aG docker "${DEPLOY_USER}"

echo "[DONE] Data server bootstrap complete"
echo "Next:"
echo "  1) Upload docker-compose.data.yml, hadoop.env, data/, spark/"
echo "  2) Run: docker compose -f ${APP_DIR}/docker-compose.data.yml up -d --build --remove-orphans"
