# BUBBLE POP-UP Backend

실시간 팝업스토어 경영 시뮬레이션 서비스 `BUBBLE POP-UP`의 Spring Boot 백엔드입니다.

사용자는 시즌에 참여해 점포 입지와 메뉴를 선택하고, 발주와 액션으로 하루 단위 매출을 운영합니다. 백엔드는 인증, 시즌 운영, 실시간 상태 계산, 랭킹, 리포트, 외부 데이터 ETL, 운영 모니터링까지 담당합니다.

## 한눈에 보기

- 장르: 실시간 팝업스토어 경영 시뮬레이션
- 핵심 경험: 시즌 참가 -> 점포 개설 -> 영업 준비 -> 실시간 운영 -> 일일 리포트 -> 최종 랭킹
- 백엔드 포인트: OAuth2/JWT 인증, Redis 기반 실시간 상태 관리, MySQL 영속화, Spark + HDFS ETL, Prometheus/Grafana 모니터링
- 저장소 범위: 백엔드 애플리케이션과 로컬/운영 인프라 설정 포함

## 채용 담당자가 보면 좋은 포인트

- 단순 CRUD가 아니라 `실시간 시뮬레이션`, `스케줄링`, `랭킹 집계`, `외부 데이터 파이프라인`이 함께 들어간 서비스입니다.
- `Redis`와 `MySQL`의 역할을 분리해 빠르게 변하는 게임 상태와 최종 기록 데이터를 각각 다른 방식으로 관리합니다.
- `Spark + HDFS` 기반 ETL로 유동인구, 교통, 뉴스 데이터를 게임 환경 점수와 콘텐츠 생성에 연결했습니다.
- 운영 단계까지 고려해 `Docker Compose`, `Actuator`, `Prometheus`, `Grafana`, `Nginx` 구성을 포함합니다.

## 서비스 흐름

아래의 `이미지 자리`는 나중에 GIF를 넣을 수 있도록 첨부 예정 파일명 기준으로 남겨둔 위치입니다.

### 1. 서비스 소개와 로그인

서비스 소개 페이지에서 진입하고 Google 또는 SSAFY OAuth2로 로그인합니다.

> 이미지 자리: `랜딩페이지.gif`
> 이미지 자리: `로그인.gif`

### 2. 시즌 참여와 대시보드 확인

현재 시즌 상태를 확인하고, 보유 포인트와 아이템을 점검한 뒤 시즌에 참가합니다. 진행 중인 시즌에 중간 참여하는 흐름도 지원합니다.

> 이미지 자리: `대시보드_포인트,아이템.gif`
> 이미지 자리: `중간참여.gif`

### 3. 점포 개설과 영업 준비

지역을 선택하고 팝업스토어 이름과 전략을 정한 뒤, 메뉴와 입지를 기반으로 영업을 준비합니다. 정규 발주와 같은 준비 단계 액션도 이 구간에서 수행합니다.

> 이미지 자리: `지역 선택, 팝업명 설명.gif`
> 이미지 자리: `2일차_영업준비.gif`
> 이미지 자리: `3일차_영업준비.gif`
> 이미지 자리: `영업준비_정규발주.gif`

### 4. 영업 중 의사결정

영업이 시작되면 실시간 상태를 보며 매출과 비용을 관리합니다. 긴급 발주, 할인, 홍보, 나눔, 팝업 이전 같은 액션과 이벤트, 날씨 변화가 플레이 결과에 직접 영향을 줍니다.

> 이미지 자리: `영업_대기.gif`
> 이미지 자리: `영업중_실시간순위.gif`
> 이미지 자리: `영업중_액션(긴급발주).gif`
> 이미지 자리: `영업중_액션(할인).gif`
> 이미지 자리: `영업중_액션(홍보).gif`
> 이미지 자리: `영업중_액션(나눔).gif`
> 이미지 자리: `영업중_액션(팝업이전).gif`
> 이미지 자리: `영업중_이벤트.gif`
> 이미지 자리: `눈오는배경.gif`

### 5. 결과 확인과 다음 전략 수립

일일 리포트와 뉴스, 지역별 순위, 최종 랭킹을 통해 결과를 되돌아보고 다음 전략을 세웁니다. 운영 실패 시 파산 리포트도 제공합니다.

> 이미지 자리: `영업중_뉴스.gif`
> 이미지 자리: `일일리포트.gif`
> 이미지 자리: `2일차_일일리포트.gif`
> 이미지 자리: `최종랭킹_통산기록.gif`
> 이미지 자리: `파산리포트.gif`

## 주요 기능

### 인증과 사용자 관리

- Google, SSAFY OAuth2 로그인
- JWT access token 기반 보호 API 인증
- Redis 기반 refresh token 수명주기 관리
- 내 정보, 포인트, 통산 기록 조회 및 닉네임 변경

### 시즌과 게임 플레이

- 시즌 대기, 참가, 중간 참여, 진행 상태 조회
- 점포 위치 변경, 메뉴 선택, 상점 아이템 구매
- 정규 발주와 긴급 발주 지원
- 할인, 홍보, 나눔, 팝업 이전 액션 지원
- 영업 종료 후 일일 리포트와 시즌 요약 제공

### 실시간 상태와 랭킹

- 10초 주기 스케줄러로 게임 상태 업데이트
- 영업 중 매출, 비용, 재고, 고객 수를 Redis live state로 관리
- 당일 상태와 누적 리포트를 결합해 실시간 TOP 랭킹 계산
- 시즌 종료 시 최종 랭킹과 보상 포인트 확정

### 데이터 기반 환경 반영

- HDFS에 적재한 외부 데이터 기반 Spark ETL 수행
- 인구, 유동인구, 뉴스 언급량을 게임 환경 점수와 랭킹, 뉴스 콘텐츠에 반영
- 게임 밸런스와 콘텐츠를 정적 값이 아니라 데이터 흐름과 연결

### 운영과 관측

- `/actuator/health`, `/actuator/prometheus` 노출
- Prometheus 수집과 Grafana 대시보드 구성
- Docker Compose 기반 로컬 인프라와 운영 배포 스크립트 제공

## 기술 설계 포인트

### 1. Redis와 MySQL의 역할 분리

- `MySQL`은 사용자, 시즌, 점포, 주문, 일일 리포트, 최종 랭킹 같은 영속 데이터를 저장합니다.
- `Redis`는 실시간 영업 상태, 현재 랭킹, refresh token처럼 빠른 조회가 중요한 데이터를 저장합니다.
- 이 분리 덕분에 자주 바뀌는 상태를 RDB에 계속 갱신하지 않고, 마감 시점에 필요한 결과만 안정적으로 저장할 수 있습니다.

### 2. 실시간 시뮬레이션과 마감 처리 분리

- `GameTickScheduler`가 10초 주기로 각 tick task를 실행합니다.
- 영업 중 계산은 Redis 기반으로 빠르게 수행하고, 영업 종료 시 `SeasonDayClosingService`가 일일 리포트와 뉴스, 최종 랭킹을 비동기 처리합니다.
- 즉시성이 중요한 작업과 정합성이 중요한 작업을 나눠 서비스 책임을 분리했습니다.

### 3. Spark + HDFS ETL을 게임 로직과 연결

- `docker-compose.local.yml`에는 MySQL, Redis, HDFS, Spark Master/Worker까지 포함되어 있습니다.
- 외부 데이터는 HDFS에 적재한 뒤 Spark ETL로 가공하고, 가공 결과를 MySQL 서비스 테이블에 반영합니다.
- 백엔드 API는 그 결과를 다시 게임 환경 계산과 뉴스 생성에 활용합니다.

### 4. 운영을 고려한 백엔드 구성

- Swagger UI로 API를 빠르게 검증할 수 있습니다.
- Actuator와 Prometheus를 통해 애플리케이션 상태를 수집하고, Grafana 대시보드로 시각화할 수 있습니다.
- 개발 환경부터 운영 배포까지 이어지는 실행 흐름을 저장소 안에 함께 둔 프로젝트입니다.

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 4, Spring MVC, Spring Security |
| Auth | OAuth2 Client, JWT |
| Persistence | Spring Data JPA, MySQL, Flyway |
| Realtime / Cache | Redis |
| Data Pipeline | Spark, HDFS |
| Infra | Docker Compose, Nginx |
| Observability | Spring Actuator, Prometheus, Grafana |
| API Docs | Springdoc OpenAPI, Swagger UI |

## 주요 API

| 목적 | 엔드포인트 예시 |
| --- | --- |
| 로그인 시작 | `GET /auth/login` |
| 토큰 재발급 / 로그아웃 | `POST /auth/refresh`, `POST /auth/logout` |
| 시즌 대기 / 참가 | `GET /game/waiting`, `POST /game/seasons/current/join` |
| 점포 / 메뉴 / 위치 | `GET /stores`, `GET /stores/menus`, `PATCH /stores/location` |
| 발주 / 액션 | `POST /orders/regular`, `POST /actions/emergency-order`, `POST /actions/discount` |
| 실시간 상태 / 리포트 | `GET /game/day/state`, `GET /game/day/reports/{day}` |
| 뉴스 / 랭킹 / 요약 | `GET /news/{day}`, `GET /news/{day}/ranking`, `GET /game/seasons/summary` |

## 저장소 구조

```text
.
├─ src
│  ├─ main/java/com/ssafy/S14P21A205
│  │  ├─ action
│  │  ├─ auth
│  │  ├─ config
│  │  ├─ exception
│  │  ├─ game
│  │  │  ├─ day
│  │  │  ├─ environment
│  │  │  ├─ event
│  │  │  ├─ news
│  │  │  ├─ season
│  │  │  ├─ support
│  │  │  └─ time
│  │  ├─ order
│  │  ├─ security
│  │  ├─ shop
│  │  ├─ store
│  │  └─ user
│  └─ test/java/com/ssafy/S14P21A205
├─ monitoring
├─ ops
├─ spark
└─ docker-compose.local.yml
```

## 테스트

- JUnit 5 기반 테스트를 사용합니다.
- 서비스, 정책, 스케줄러, Redis repository 중심 테스트가 포함되어 있습니다.

```bash
./gradlew test
```

## 로컬 실행 방법

### 1. 환경 변수 준비

`.env` 파일에 아래 값을 설정합니다.

필수
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

선택
- `SSAFY_CLIENT_ID`
- `SSAFY_CLIENT_SECRET`
- `SSAFY_AUTHORIZATION_URI`
- `SSAFY_TOKEN_URI`
- `SSAFY_USER_INFO_URI`
- `JWT_SECRET`

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

실행 시 Spring Boot Docker Compose integration을 통해 `docker-compose.local.yml`의 로컬 인프라를 함께 사용할 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

문제가 생기면 아래 명령어로 로컬 컨테이너를 정리합니다.

```bash
docker compose -f docker-compose.local.yml down --remove-orphans
```

## 빌드와 배포

### 빌드

```bash
./gradlew clean bootJar
```

생성된 JAR은 `build/libs/`에 위치합니다.

### 운영 배포 관련 파일

- `docker-compose.yml`
- `ops/scripts/bootstrap_ec2.sh`
- `ops/scripts/setup_server_nginx.sh`
- `.env.prod`

운영 환경에서는 `SPRING_DOCKER_COMPOSE_ENABLED=false` 설정이 필요합니다.

## 모니터링

- Prometheus 설정: [monitoring/prometheus/prometheus.yml](monitoring/prometheus/prometheus.yml)
- Grafana 대시보드: [monitoring/grafana/dashboards/infra-overview.json](monitoring/grafana/dashboards/infra-overview.json)
