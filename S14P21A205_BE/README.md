# BUBBLE POP-UP Backend

실시간 팝업스토어 경영 시뮬레이션 서비스 **BUBBLE POP-UP**의 Spring Boot 백엔드 저장소입니다.

사용자는 시즌에 참가해 점포 입지와 메뉴를 선택하고, 발주와 액션을 활용해 하루 단위로 매출을 운영합니다. 백엔드는 시즌 진행, 실시간 게임 상태, 랭킹 집계, 인증, 데이터 파이프라인 연동, 운영 모니터링을 담당합니다.

## Overview

- OAuth2 로그인 이후 JWT 기반 보호 API 인증 처리
- 시즌 참가, 영업 시작, 일일 리포트, 최종 정산까지 이어지는 게임 흐름 제공
- MySQL과 Redis를 분리 사용해 실시간 상태와 영속 데이터를 함께 관리
- Spark + HDFS 기반 외부 데이터 ETL 결과를 게임 환경 데이터에 반영
- Prometheus + Grafana 기반 운영 지표 수집 및 모니터링 구성

## Key Features

### 1. Authentication
- Google, SSAFY OAuth2 로그인 지원
- JWT access token 기반 API 인증
- Redis를 활용한 refresh token 수명주기 관리

### 2. Season and Gameplay
- 시즌 대기, 참가, 진행 상태 조회 API 제공
- 점포 위치 변경, 메뉴 선택, 정규 발주, 긴급 발주, 프로모션/기부/할인 액션 지원
- 일자별 영업 결과 리포트와 시즌 요약 조회 제공

### 3. Realtime Ranking
- 과거 누적 리포트는 MySQL에서 조회
- 당일 진행 중인 매출/비용 상태는 Redis에서 조회
- 계산된 실시간 TOP 랭킹은 Redis에 캐시해 반복 조회 부하 감소

### 4. Data Pipeline
- Spark 작업으로 인구/유동인구 점수 ETL 수행
- HDFS 적재 데이터를 기반으로 게임 환경 데이터 생성
- 시즌 진행 로직과 ETL 결과를 연결해 게임 밸런스에 반영

### 5. Observability
- `/actuator/health`, `/actuator/prometheus` 노출
- Prometheus 수집과 Grafana 대시보드 구성
- 요청량, 에러율, P95 지연시간, JVM 상태, DB 커넥션 상태 추적

## Tech Stack

- Language: Java 17
- Framework: Spring Boot 4, Spring MVC, Spring Security
- Persistence: Spring Data JPA, MySQL, Flyway
- Cache: Redis
- Data/Batch: Spark, HDFS
- Infra: Docker Compose, Nginx
- Monitoring: Prometheus, Grafana

## Architecture

- **MySQL**
  - 사용자, 시즌, 점포, 발주, 일일 리포트, 최종 랭킹 등 영속 데이터 저장
- **Redis**
  - 실시간 영업 상태, 현재 랭킹, 토큰 식별값 등 빠른 조회가 필요한 데이터 저장
- **Spark + HDFS**
  - 외부 데이터 기반 ETL 처리와 환경 점수 산출
- **Spring Boot**
  - 인증, 게임 도메인 로직, 스케줄러, API, 모니터링 엔드포인트 제공

## Main Domains

- `auth`: OAuth2 로그인, JWT 재발급/로그아웃
- `game.day`: 영업 시작, 실시간 상태, 일일 리포트
- `game.season`: 시즌 참가, 대기 상태, 실시간/최종 랭킹, 시즌 요약
- `store`, `order`, `action`, `shop`: 점포 운영 관련 비즈니스 로직
- `game.news`: 뉴스 및 랭킹 요약 조회

## Project Structure

```text
src/main/java/com/ssafy/S14P21A205
├─ auth
├─ action
├─ game
│  ├─ day
│  ├─ news
│  ├─ season
│  └─ scheduler
├─ order
├─ shop
├─ store
├─ user
├─ config
└─ exception
```

## Local Development

### 1. Prepare environment variables

`.env` 파일에 로컬 실행 값을 채웁니다.

Required:
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

Optional:
- `SSAFY_CLIENT_ID`
- `SSAFY_CLIENT_SECRET`
- `SSAFY_AUTHORIZATION_URI`
- `SSAFY_TOKEN_URI`
- `SSAFY_USER_INFO_URI`

### 2. Run application

```bash
./gradlew bootRun
```

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Spring Boot Docker Compose integration으로 `docker-compose.local.yml` 인프라가 자동 연결됩니다.

문제가 생기면 아래 명령으로 로컬 컨테이너를 정리한 뒤 다시 실행합니다.

```bash
docker compose -f docker-compose.local.yml down --remove-orphans
```

## Build and Test

```bash
./gradlew clean bootJar
./gradlew test
```

생성된 애플리케이션 JAR은 `build/libs/` 아래에 위치합니다.

## Deployment

운영 배포는 Docker Compose와 Nginx 기반으로 구성되어 있습니다.

주요 파일:
- `docker-compose.yml`
- `ops/scripts/bootstrap_ec2.sh`
- `ops/scripts/setup_server_nginx.sh`
- `.env.prod`

배포 전 준비 항목:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `REDIS_HOST`
- `NGINX_SERVER_NAME`
- `NGINX_SSL_CERT_PATH`
- `NGINX_SSL_KEY_PATH`
- OAuth client values

## Monitoring

- Prometheus 설정: [monitoring/prometheus/prometheus.yml](monitoring/prometheus/prometheus.yml)
- Grafana 대시보드: [monitoring/grafana/dashboards/infra-overview.json](monitoring/grafana/dashboards/infra-overview.json)

## Notes

- `.env`, `.env.*`, `.idea/`, `*.iml` 등 로컬 전용 파일은 커밋하지 않습니다.
- 운영 환경에서는 `SPRING_DOCKER_COMPOSE_ENABLED=false` 설정이 필요합니다.
