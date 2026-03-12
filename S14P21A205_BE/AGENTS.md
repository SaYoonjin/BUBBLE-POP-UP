# Repository Guidelines

## Project Structure & Module Organization
This repository is a Spring Boot backend.
- Application code: `src/main/java/com/ssafy/S14P21A205`
- Main domains: `auth`, `user`, `config`, `security/handler`, `exception`
- Resources/config: `src/main/resources` (`application.yml`, `banner.txt`)
- Tests: `src/test/java/com/ssafy/S14P21A205`
- DevOps/deploy files: `ops/scripts`, `ops/nginx/docker`, `Dockerfile`, `docker-compose.yml`
- Local infrastructure: `docker-compose.local.yml`
- Environment files: `.env` for local, `.env.prod` for deployment

## Build, Test, and Development Commands
- `./gradlew bootRun`: run app locally (includes Spring Boot Docker Compose integration for local infra).
- `./gradlew clean bootJar`: build deployable JAR to `build/libs/`.
- `./gradlew test`: run unit/integration tests.
- `docker compose -f docker-compose.local.yml down --remove-orphans`: clean stale local containers if startup conflicts occur.

## Coding Style & Naming Conventions
- Language/runtime: Java 17, Spring Boot 4.
- Use consistent 4-space indentation and keep methods focused.
- Package names are lowercase by domain (for example, `...auth.service`, `...user.controller`).
- Class/record/enum names: `PascalCase`; methods/fields: `camelCase`.
- DTO naming: `*Request`, `*Response`.
- Keep API paths consistent under `/api/...`.

## Testing Guidelines
- Testing stack: JUnit 5 with Spring Boot test support.
- Place tests in mirrored packages under `src/test/java`.
- Test class naming: `*Tests`.
- For changed business logic, add at least:
- one success-path test
- one validation/error-path test

## Commit & Pull Request Guidelines
- Follow concise conventional prefixes used in history: `feat:`, `fix:`, `refactor:`, `chore:`.
- Write commit subjects in imperative form (example: `fix: validate OAuth client properties`).
- PRs should include:
- what changed and why
- config/env impact (`.env` keys, compose/service changes)
- linked issue/task ID
- API examples (or screenshots for Swagger/UI-impacting changes)

## Security & Configuration Tips
- Never commit `.env` or real secrets.
- `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are required for OAuth client startup.
- Set `SPRING_DOCKER_COMPOSE_ENABLED=false` in production runtime environments.
