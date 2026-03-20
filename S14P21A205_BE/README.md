# S14P21A205 Backend

## 1) Local Development (Team)

### 1-1. First setup
```bash
Fill `.env` with the shared local development values.
```

Required values in `.env`:
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

Optional values (when enabling SSAFY login):
- `SSAFY_CLIENT_ID`
- `SSAFY_CLIENT_SECRET`
- `SSAFY_AUTHORIZATION_URI`
- `SSAFY_TOKEN_URI`
- `SSAFY_USER_INFO_URI`

### 1-2. Run
```bash
./gradlew bootRun
```

- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Stop app: `Ctrl + C`

Notes:
- This project includes Spring Boot Docker Compose integration.
- `bootRun` automatically links local infra in `docker-compose.local.yml` when needed.
- Do not commit `.env`.

If startup fails due to leftover containers, clean with:
```bash
docker compose -f docker-compose.local.yml down --remove-orphans
```
and run `./gradlew bootRun` again.

## 2) Production Server (Ubuntu + Docker Compose)

### 2-0. Fast path (automated script)
After preparing deployment files and `.env.prod`, run:
```bash
DOMAIN=<YOUR_API_DOMAIN> APP_DIR=/home/ubuntu/S14P21A205 CERTBOT_EMAIL=you@example.com \
bash ops/scripts/bootstrap_ec2.sh
```

Then upload the repo deployment files + `build/libs/app.jar` and run:
```bash
DOMAIN=<YOUR_API_DOMAIN> APP_DIR=/home/ubuntu/S14P21A205 CERTBOT_EMAIL=you@example.com \
bash ops/scripts/setup_server_nginx.sh
```

### 2-1. Prepare env file
Prepare `.env.prod` with the real deployment values.

Required production values:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` for RDS
- `REDIS_HOST=redis`
- `NGINX_SERVER_NAME`
- `NGINX_SSL_CERT_PATH`
- `NGINX_SSL_KEY_PATH`
- OAuth client values

### 2-2. Prepare app binary
```bash
./gradlew clean bootJar
```
Rename or copy the built jar as `build/libs/app.jar` before deploy.

### 2-3. Bootstrap web server
```bash
sudo DEPLOY_USER=ubuntu APP_DIR=/home/ubuntu/S14P21A205 \
bash ops/scripts/bootstrap_ec2.sh
```

### 2-4. Upload deployment files
```bash
rsync -az Dockerfile docker-compose.yml ops/ ubuntu@<WEB_SERVER_IP>:/home/ubuntu/S14P21A205/
scp build/libs/app.jar ubuntu@<WEB_SERVER_IP>:/home/ubuntu/S14P21A205/build/libs/app.jar
scp .env.prod ubuntu@<WEB_SERVER_IP>:/home/ubuntu/S14P21A205/.env.prod
```

### 2-5. Start HTTP container and issue certificate
```bash
DOMAIN=<YOUR_API_DOMAIN> APP_DIR=/home/ubuntu/S14P21A205 CERTBOT_EMAIL=you@example.com \
bash /home/ubuntu/S14P21A205/ops/scripts/setup_server_nginx.sh
```

### 2-6. Verify
```bash
curl -I https://<YOUR_API_DOMAIN>
```

## 3) OAuth Redirect URI

Google OAuth Client must include both:
- `http://localhost:8080/login/oauth2/code/google`
- `https://<YOUR_API_DOMAIN>/login/oauth2/code/google`

SSAFY OAuth Client (if used) must include both:
- `http://localhost:8080/login/oauth2/code/ssafy`
- `https://<YOUR_API_DOMAIN>/login/oauth2/code/ssafy`

Login start API examples:
- Google: `/auth/login?provider=google`
- SSAFY: `/auth/login?provider=ssafy`

## 4) GitLab CI/CD (Self-Hosted Runner -> Web Server Docker Compose)

This repo includes `.gitlab-ci.yml` in the parent directory with:
- `build-backend`: build `build/libs/app.jar`
- `bootstrap-webserver` (manual, one-time): install Docker/Certbot on the web server
- `deploy-web`: upload Docker deployment files and run `docker compose up -d --build`

Required GitLab CI variables:
- `SSH_PRIVATE_KEY`: private key for server SSH
- `WEB_SERVER_IP`: EC2 public DNS/IP
- `WEB_USER`: default `ubuntu`
- `WEB_APP_DIR`: default `/home/ubuntu/S14P21A205`
- `PROD_ENV_FILE_B64`: base64-encoded `.env.prod` content
- `WEB_DOMAIN`: API domain for certificate issue
- `CERTBOT_EMAIL`: email for Let's Encrypt
