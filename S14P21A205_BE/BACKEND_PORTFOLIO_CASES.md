# 백엔드 직무 지원 어필 사례 정리

> 기준: 현재 백엔드 저장소 코드와 설정을 바탕으로 정리한 사례입니다. 지원서에는 본인이 실제로 담당한 범위에 맞춰 1인칭으로 다듬어 사용하면 좋습니다.

## 프로젝트 수치 요약

- Spring Boot 4 / Java 17 기반 백엔드
- 메인 Java 파일 219개, 약 15,694라인
- 테스트 파일 29개, 테스트 케이스 106개, 약 5,532라인
- REST API 매핑 33개
- Spark ETL/분석 스크립트 11개, 약 1,261라인
- 로컬 인프라: MySQL, Redis, Hadoop NameNode/DataNode, Spark Master/Worker
- 운영 인프라: Docker Compose, Nginx, Redis AOF, Prometheus, Grafana, Alertmanager, node-exporter

## 1. 실시간 게임 상태 계산을 Redis 기반으로 분리

### 상황

게임 영업 중에는 매장별 재고, 매출, 고객 수, 구매 수, 잔액, 누적 비용, 액션 사용 여부가 계속 변합니다. 이 값을 매 요청마다 DB에 쓰거나 매번 처음부터 다시 계산하면 트래픽이 늘어날수록 DB 부하가 커지고, 사용자는 실시간 게임 화면에서 지연을 체감할 수 있습니다.

특히 이 프로젝트는 영업 시간이 실제 시간 2분으로 압축되어 있고, 10초 단위 틱으로 상태가 갱신됩니다. 하루 영업에는 총 12틱이 존재하므로 매장 수가 늘어나면 짧은 시간 안에 상태 계산과 조회가 반복됩니다.

근거 파일:
- `src/main/java/com/ssafy/S14P21A205/game/time/policy/GameTimePolicy.java`
- `src/main/java/com/ssafy/S14P21A205/game/day/service/GameDayStateService.java`
- `src/main/java/com/ssafy/S14P21A205/game/day/state/repository/GameDayStoreStateRedisRepository.java`

### 해결방법과 그렇게 생각한 이유

영업 중 상태는 Redis Hash로 관리하고, 하루 종료 시점의 확정 결과만 DB 리포트로 저장하는 구조를 선택했습니다.

사고 과정은 다음과 같습니다.

1. 영업 중 상태는 초 단위로 바뀌는 "현재값"이므로 영속성보다 빠른 읽기/쓰기와 원자적인 필드 갱신이 중요하다고 판단했습니다.
2. Redis Hash에 `balance`, `stock`, `cumulative_sales`, `cumulative_total_cost`, `capture_rate`, `sale_price`, `last_calculated_at` 등 필드를 나누어 저장하면 특정 액션이 일부 필드만 바꾸는 경우에도 전체 객체를 DB에 다시 쓰지 않아도 됩니다.
3. 틱별 히스토리는 별도 Redis Hash(`tick_log`)로 남겨 디버깅과 리포트 생성에 활용할 수 있게 했습니다.
4. `GameDayStateService`는 마지막 계산 시점 이후의 틱만 순차 처리합니다. 즉, 매 요청마다 0틱부터 재계산하지 않고 `state.tick()` 이후부터 현재 틱까지 진행합니다.
5. Redis 상태가 없거나 시작 응답이 없는 경우에는 `GameDayStartService.ensureCurrentDayState`로 초기 상태를 자동 생성해 복구 가능성을 높였습니다.

### 성과

- 하루 영업 2분, 10초 틱 기준 매장당 최대 12번의 상태 갱신 단위를 Redis에서 처리하도록 분리했습니다.
- 실시간 상태에 필요한 필드 20개 이상을 Redis Hash에 구조화해 부분 갱신이 가능해졌습니다.
- DB는 확정 리포트 저장에 집중하고, 영업 중 빈번한 상태 변화는 Redis로 처리해 DB write 부하를 줄일 수 있는 구조를 만들었습니다.
- 관련 테스트로 `GameDayStateServiceTests`, `GameDayStartServiceTests`, `GameDayStoreStateRedisRepositoryTests` 등에서 상태 초기화, 저장, 복구 흐름을 검증했습니다.

## 2. 실시간 시즌 TOP 랭킹 캐싱 구조 설계

### 상황

시즌 랭킹은 사용자가 자주 확인하는 데이터지만, 매 요청마다 모든 매장의 과거 리포트와 당일 실시간 상태를 합산해 ROI를 계산하면 비용이 큽니다. 특히 랭킹은 정렬과 동률 처리까지 필요해서 사용자 요청 경로에 그대로 두면 응답 시간이 길어질 수 있습니다.

근거 파일:
- `src/main/java/com/ssafy/S14P21A205/game/season/scheduler/RealtimeSeasonRankingTickTask.java`
- `src/main/java/com/ssafy/S14P21A205/game/season/repository/SeasonRankingRedisRepository.java`
- `src/main/java/com/ssafy/S14P21A205/game/season/service/SeasonRankingService.java`

### 해결방법과 그렇게 생각한 이유

랭킹 생성과 랭킹 조회를 분리했습니다. 백그라운드 틱 태스크가 현재 시즌 매장을 조회하고, 과거 일차의 DB 리포트와 당일 Redis 누적 매출/비용을 합산해 ROI 기준 TOP 10만 Redis에 캐싱합니다. API는 캐시된 TOP 랭킹을 읽어 응답합니다.

이렇게 판단한 이유는 다음과 같습니다.

1. TOP 랭킹은 초정밀 실시간보다 "주기적으로 최신화된 스냅샷"이 사용자 경험상 충분하다고 봤습니다.
2. 랭킹 조회 요청마다 N개 매장의 DB/Redis 데이터를 모두 합산하지 않고, 10초 주기 태스크에서 한 번 계산해 공유하면 같은 계산의 반복을 줄일 수 있습니다.
3. 동률일 경우 같은 등수를 부여하고, 이후 정렬 안정성을 위해 `ROI desc -> userId asc -> storeId asc` 순으로 정렬해 결과가 흔들리지 않게 했습니다.
4. 진행 중 시즌이 없거나 매장이 없으면 Redis 캐시를 삭제해 오래된 랭킹이 노출되지 않도록 했습니다.

### 성과

- 현재 시즌 TOP 10 랭킹 API를 Redis 단건 조회에 가깝게 단순화했습니다.
- 시즌 랭킹 계산은 10초 주기 게임 틱 태스크와 결합되어 최신성을 유지합니다.
- 과거 DB 리포트와 당일 Redis 상태를 합산해 "확정 데이터 + 실시간 데이터"를 동시에 반영했습니다.
- 랭킹 보상 포인트를 1등 30점, 2등 20점, 3등 10점, 그 외 5점으로 자동 산정하도록 구현했습니다.
- `RealtimeSeasonRankingTickTaskTests`와 `SeasonRankingServiceTests`로 랭킹 정렬, 캐시 초기화, 조회 정책을 검증했습니다.

## 3. 시즌 라이프사이클과 게임 시간을 정책 객체로 모델링

### 상황

게임은 시즌 시작, 입지 선택, 영업 준비, 영업 중, 일일 리포트, 시즌 요약, 다음 시즌 대기까지 여러 상태를 가집니다. 프론트엔드와 백엔드가 같은 시간 개념을 공유하지 못하면 "지금 주문 가능한가", "지금 영업 상태인가", "오늘 몇 일차인가" 같은 판단이 API마다 달라질 위험이 있습니다.

근거 파일:
- `src/main/java/com/ssafy/S14P21A205/game/time/policy/GameTimePolicy.java`
- `src/main/java/com/ssafy/S14P21A205/game/time/service/SeasonTimelineService.java`
- `src/main/java/com/ssafy/S14P21A205/game/season/service/CurrentSeasonTimeService.java`
- `src/main/java/com/ssafy/S14P21A205/game/season/service/SeasonLifecycleService.java`

### 해결방법과 그렇게 생각한 이유

게임 시간 계산을 `GameTimePolicy`와 `SeasonTimelineService`로 모았습니다.

구체적으로는 입지 선택 2분, 영업 준비 50초, 영업 2분, 리포트 10초, 시즌 요약 2분, 다음 시즌 대기 5분을 하나의 정책으로 정의했습니다. 실제 영업 시간은 10:00~22:00로 표현하되, 실제 시간 2분을 게임 내 12시간으로 매핑했습니다.

이 구조를 선택한 이유는 다음과 같습니다.

1. 시간 정책이 여러 서비스에 흩어지면 "주문 가능 시간", "액션 가능 시간", "랭킹 갱신 시간"이 서로 달라지는 버그가 생깁니다.
2. 정책 객체로 모으면 테스트에서 특정 시각을 넣어 페이즈, 일차, 남은 시간, 게임 시각, 틱을 검증할 수 있습니다.
3. `Clock`을 주입해 테스트와 운영에서 시간 의존 로직을 분리할 수 있습니다.

### 성과

- 시즌 1개를 7일 기준으로 운영할 때 입지 선택, 7일치 영업 루프, 시즌 요약, 다음 시즌 대기까지 전체 흐름을 자동 계산합니다.
- 영업 2분을 10초 틱 12개로 나눠 상태 갱신, 랭킹 갱신, 뉴스 생성 타이밍의 기준을 통일했습니다.
- `GameTimePolicyTests`, `CurrentSeasonTimeServiceTests`, `ClockConfigTests`로 시간 정책과 테스트 가능성을 확보했습니다.

## 4. 시즌 시작 전 Spark ETL 데이터 정합성 검증

### 상황

게임 시즌은 유동인구, 교통, 날씨, 이벤트, 뉴스 데이터에 의존합니다. 특정 지역만 데이터 일자가 부족하거나, 교통 데이터와 유동인구 데이터의 배치가 다르면 시즌 시작 후 사용자별 결과가 불공정해질 수 있습니다.

근거 파일:
- `src/main/java/com/ssafy/S14P21A205/game/season/service/SeasonLifecycleService.java`
- `src/main/java/com/ssafy/S14P21A205/game/scheduler/SparkEtlScheduler.java`
- `spark/jobs/etl_population_score.py`
- `spark/jobs/etl_traffic_score.py`
- `spark/jobs/list_available_dates.py`

### 해결방법과 그렇게 생각한 이유

시즌 시작 전에 Spark ETL을 실행하고, 생성된 데이터의 `sourceBatchKey`와 날짜 구간을 검증한 뒤 시즌을 시작하도록 만들었습니다.

사고 과정은 다음과 같습니다.

1. 단순히 데이터가 "있다/없다"만 확인하면 지역별 날짜가 어긋난 문제를 잡을 수 없다고 봤습니다.
2. 그래서 유동인구와 교통 데이터 모두에서 단일 배치키가 존재하는지 확인하고, 모든 지역이 같은 날짜 목록을 갖는지 검증했습니다.
3. 시즌 총 일수만큼 정확히 데이터가 있어야 하므로 7일 연속 구간을 HDFS에서 찾아 랜덤 선택하고, 실패 시 제한된 날짜 범위에서 fallback하도록 했습니다.
4. Spark ETL은 트랜잭션 밖에서 실행하도록 분리했습니다. Spark 쪽 DDL/외부 프로세스 실행이 Spring 트랜잭션과 섞이면 락이나 반복 실행 문제가 생길 수 있기 때문입니다.

### 성과

- 시즌 시작 데이터 조건을 "배치키 일치 + 지역별 날짜 정렬 + 총 일수 일치"로 검증해 데이터 불일치로 인한 런타임 오류를 줄였습니다.
- Spark job 11개로 인구, 교통, 뉴스, 검증, 날짜 조회 흐름을 구성했습니다.
- ETL 실행 타임아웃을 인구/교통 10분, 뉴스 3분으로 제한해 외부 프로세스가 무한 대기하지 않도록 했습니다.
- 2023-01-01부터 2024-12-25 범위에서 7일 연속 데이터를 선택하도록 해 시즌별 데이터 다양성을 확보했습니다.

## 5. 일일 마감 처리에서 동기 정산과 비동기 뉴스 생성을 분리

### 상황

하루 영업 종료 시점에는 모든 매장의 일일 리포트를 저장하고, 마지막 날이면 최종 랭킹도 확정해야 합니다. 동시에 뉴스/랭킹 집계도 필요하지만, 뉴스 생성이 느려지면 리포트 저장과 시즌 진행 자체가 지연될 수 있습니다.

근거 파일:
- `src/main/java/com/ssafy/S14P21A205/game/day/service/SeasonDayClosingService.java`
- `src/main/java/com/ssafy/S14P21A205/game/day/service/GameDayReportService.java`
- `src/main/java/com/ssafy/S14P21A205/game/season/service/SeasonFinalRankingService.java`

### 해결방법과 그렇게 생각한 이유

정산과 부가 집계를 분리했습니다. 일일 리포트 저장과 최종 랭킹 저장은 동기로 처리하고, Redis 기반 순위 집계와 마감 뉴스 생성은 `CompletableFuture`와 별도 executor로 비동기 처리했습니다.

이유는 다음과 같습니다.

1. 정산 결과는 다음 일차의 주문 가능 금액, 재고 이월, 최종 랭킹에 직접 영향을 주기 때문에 반드시 순서가 보장되어야 합니다.
2. 반면 뉴스 생성은 사용자 경험상 조금 늦게 도착해도 되는 부가 기능입니다.
3. 따라서 정산을 먼저 확정하고, 실패 가능성이 있는 뉴스 생성은 별도 스레드에서 처리해 핵심 게임 루프를 보호했습니다.

### 성과

- 모든 매장의 일일 리포트 저장을 우선 보장하고, 마지막 날에는 최종 랭킹까지 동기로 확정합니다.
- 뉴스/랭킹 갱신 실패가 시즌 마감 전체 실패로 번지지 않도록 격리했습니다.
- 커밋 히스토리에도 일일 리포트 동기 처리, 뉴스 생성 분리, 실시간 알림/랭킹 연동 수정 흐름이 남아 있어 운영 중 병목을 개선한 사례로 설명하기 좋습니다.

## 6. 주문/액션 도메인의 복잡한 비즈니스 규칙을 방어적으로 검증

### 상황

사용자는 일반 발주, 긴급 발주, 할인, 기부, 홍보 액션을 수행할 수 있습니다. 이 기능들은 잔액, 재고, 판매가, 도착 시간, 이벤트 효과, 교통 지연, 아이템 할인율, 뉴스 랭킹을 동시에 고려합니다. 검증이 느슨하면 음수 재고, 중복 액션, 허용 범위 밖 가격, 잔액 초과 구매 같은 문제가 발생합니다.

근거 파일:
- `src/main/java/com/ssafy/S14P21A205/order/service/OrderServiceImpl.java`
- `src/main/java/com/ssafy/S14P21A205/action/service/ActionServiceImpl.java`
- `src/main/java/com/ssafy/S14P21A205/game/day/resolver/TrafficDelayResolver.java`
- `src/main/java/com/ssafy/S14P21A205/game/day/resolver/EventEffectResolver.java`

### 해결방법과 그렇게 생각한 이유

각 액션과 주문의 실행 전에 게임 상태, 자금, 재고, 일차, 페이즈, 중복 사용 여부를 검증했습니다.

핵심 판단은 다음과 같습니다.

1. 일반 발주는 준비 시간에만 가능하고, 1/3/5/7일차에만 허용했습니다. 운영상 발주 가능한 시점을 제한해야 게임 밸런스와 재고 예측이 가능합니다.
2. 발주 수량은 50~500개로 제한했습니다. 지나치게 작은 주문은 게임 의미가 약하고, 지나치게 큰 주문은 밸런스를 깨기 때문입니다.
3. 판매가는 원가 기반 최소 판매가와 추천가의 2배 사이로 제한했습니다. 사용자 자유도는 주되, 수익률 계산이 비정상적으로 튀는 것을 막기 위한 선택입니다.
4. 할인/기부/홍보/긴급발주는 Redis에 액션 사용 여부를 저장해 하루 1회 제한을 강제했습니다.
5. 긴급 발주는 교통 지연을 반영해 도착 시간을 계산하고, 도착 시점에 메뉴/가격/재고가 반영되도록 했습니다.

### 성과

- 일반 발주, 긴급 발주, 할인, 기부, 홍보 등 5개 주요 플레이어 액션을 하나의 게임 상태 모델과 연결했습니다.
- `ORDER-001`~`ORDER-006`, `ACTION-001`~`ACTION-003` 등 명확한 에러코드로 검증 실패를 API 응답에 반영했습니다.
- `OrderServiceImplTests`, `ActionServiceImplTests`, `TrafficDelayResolverTests`, `EventEffectResolverTests`로 성공/실패 경로를 포함한 비즈니스 규칙을 검증했습니다.

## 7. 뉴스/트렌드 데이터를 게임 밸런스에 반영

### 상황

단순 랜덤 게임이면 사용자가 지역/메뉴 선택의 이유를 체감하기 어렵습니다. 이 프로젝트는 뉴스, 메뉴 언급량, 지역/메뉴 랭킹, 이벤트를 게임 수요와 비용에 반영해야 했습니다.

근거 파일:
- `src/main/java/com/ssafy/S14P21A205/game/news/service/SparkNewsDataService.java`
- `src/main/java/com/ssafy/S14P21A205/game/news/service/NewsService.java`
- `src/main/java/com/ssafy/S14P21A205/game/day/resolver/NewsRankingResolver.java`
- `src/main/java/com/ssafy/S14P21A205/game/day/policy/StoreRankingPolicy.java`

### 해결방법과 그렇게 생각한 이유

Spark로 뉴스 데이터를 집계하고, 메뉴 언급량과 랭킹을 게임 시작/주문/액션 계산에 연결했습니다.

이렇게 설계한 이유는 다음과 같습니다.

1. 뉴스 데이터는 원천 데이터 크기가 커질 수 있으므로 애플리케이션 서버에서 직접 분석하기보다 Spark job으로 전처리하는 것이 적합하다고 봤습니다.
2. Spark 결과는 메뉴 언급 수와 랭킹 형태로 단순화해 백엔드 서비스에서 빠르게 사용할 수 있게 했습니다.
3. 뉴스 랭킹이 없을 때는 현재 시즌 매장 분포 기반 랭킹으로 fallback해 기능이 중단되지 않도록 했습니다.
4. 뉴스는 오픈 전/영업 중/마감 후 시나리오별로 생성되며, 시즌 시작 전 미리 생성 가능한 뉴스는 준비 단계에서 생성했습니다.

### 성과

- 뉴스 데이터와 게임 경제 시스템을 연결해 지역/메뉴 선택의 전략성을 강화했습니다.
- Spark 뉴스 ETL은 3분 타임아웃과 stdout/stderr drain 처리를 적용해 외부 프로세스 블로킹 위험을 줄였습니다.
- 뉴스 랭킹이 없을 때도 fallback 계산을 사용해 API 장애 대신 품질 저하 형태로 대응하도록 만들었습니다.

## 8. OAuth2 + JWT + Redis Refresh Token 기반 인증 구조

### 상황

프론트엔드와 백엔드가 분리된 서비스에서 Google/SSAFY OAuth 로그인을 지원해야 했습니다. 동시에 API는 stateless JWT로 보호하고, refresh token은 탈취/재사용 위험을 줄여야 했습니다.

근거 파일:
- `src/main/java/com/ssafy/S14P21A205/config/SecurityConfig.java`
- `src/main/java/com/ssafy/S14P21A205/auth/service/AuthService.java`
- `src/main/java/com/ssafy/S14P21A205/auth/service/JwtTokenService.java`
- `src/main/java/com/ssafy/S14P21A205/security/handler/AuthLoginSuccessHandler.java`
- `src/main/java/com/ssafy/S14P21A205/auth/service/AuthRedirectService.java`

### 해결방법과 그렇게 생각한 이유

OAuth 로그인 성공 후 자체 access/refresh JWT를 발급하고, refresh token의 `jti`를 Redis에 저장했습니다. refresh 요청 시 Redis에 저장된 `jti`와 사용자 ID가 맞는지 검증하고, 검증 후 기존 refresh token을 삭제한 뒤 새 토큰을 발급하는 회전 방식을 적용했습니다.

이 구조를 선택한 이유는 다음과 같습니다.

1. access token은 짧게, refresh token은 길게 가져가되 refresh token은 서버 저장소에서 폐기 가능해야 합니다.
2. Redis에 refresh `jti`를 저장하면 로그아웃이나 refresh 회전 시 서버 측 무효화가 가능합니다.
3. refresh token은 HttpOnly cookie로 내려 브라우저 JavaScript 접근을 제한하고, access token은 redirect fragment에 담아 callback 페이지에서 처리하게 했습니다.
4. OAuth redirect는 허용 origin 검증을 거쳐 open redirect 위험을 줄였습니다.

### 성과

- access token 기본 TTL 30분, refresh token 기본 TTL 14일 정책을 설정했습니다.
- refresh token 회전과 로그아웃 시 Redis 키 삭제로 재사용 가능성을 낮췄습니다.
- Google과 SSAFY OAuth provider를 모두 고려한 설정 구조를 만들었습니다.
- `SecurityConfigTests`, `SsafyOAuthSettingsTests`로 보안 설정과 provider 설정 검증을 포함했습니다.

## 9. 예외 응답과 에러코드 표준화

### 상황

도메인이 게임, 주문, 액션, 상점, 인증으로 늘어나면서 예외가 서비스마다 다르게 내려가면 프론트엔드에서 처리하기 어렵습니다. 특히 게임 상태처럼 실패 이유가 다양할수록 HTTP status와 내부 코드가 일관되어야 합니다.

근거 파일:
- `src/main/java/com/ssafy/S14P21A205/exception/ErrorCode.java`
- `src/main/java/com/ssafy/S14P21A205/exception/BaseException.java`
- `src/main/java/com/ssafy/S14P21A205/exception/ErrorResponse.java`
- `src/main/java/com/ssafy/S14P21A205/exception/GlobalExceptionHandler.java`

### 해결방법과 그렇게 생각한 이유

도메인별 에러코드를 enum으로 정의하고, 전역 예외 핸들러에서 `BaseException`, validation 예외, 인증/인가 예외, 알 수 없는 예외를 공통 응답으로 변환했습니다.

이유는 다음과 같습니다.

1. 프론트엔드는 HTTP status만으로 정확한 사용자 메시지를 만들기 어렵기 때문에 `ORDER-006`, `ACTION-001` 같은 안정적인 내부 코드가 필요합니다.
2. 4xx와 5xx 로그 레벨을 구분해 사용자 입력 오류와 서버 장애를 운영상 다르게 볼 수 있어야 합니다.
3. validation 예외는 첫 번째 필드 오류를 요약해 내려 API 사용자가 빠르게 수정할 수 있게 했습니다.

### 성과

- 공통, 인증, 게임, 상점, 주문, 액션, 쇼핑 도메인에 걸쳐 20개 이상의 에러코드를 정의했습니다.
- 예외 응답 형식을 표준화해 API 33개에서 공통된 실패 처리가 가능해졌습니다.
- 서버 오류는 error 로그, 사용자 입력/권한 오류는 warn/info 로그로 분리해 운영 분석성을 높였습니다.

## 10. 운영 배포와 관측 가능성 구성

### 상황

백엔드 서버는 로컬 개발 환경과 운영 환경의 요구사항이 다릅니다. 로컬에서는 MySQL/Redis/Hadoop/Spark를 쉽게 띄워야 하고, 운영에서는 HTTPS, Redis 영속화, 애플리케이션 헬스체크, 메트릭 수집, 서버 리소스 모니터링이 필요합니다.

근거 파일:
- `docker-compose.local.yml`
- `docker-compose.yml`
- `Dockerfile`
- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`
- `monitoring/prometheus/prometheus.yml`
- `docker-compose.monitoring.yml`
- `ops/scripts/setup_server_nginx.sh`

### 해결방법과 그렇게 생각한 이유

로컬과 운영 Compose 파일을 분리하고, 운영에서는 Docker Compose로 backend, redis, nginx를 묶었습니다. 로컬은 Spring Boot Docker Compose integration으로 개발자가 `./gradlew bootRun`만 실행해도 인프라를 연결할 수 있게 했습니다.

운영 관측은 Actuator의 `/actuator/prometheus`를 열고 Prometheus가 15초 주기로 수집하도록 구성했습니다. node-exporter는 로컬 모니터링 서버와 웹/데이터 서버를 SSH tunnel로 수집할 수 있게 분리했습니다.

이렇게 판단한 이유는 다음과 같습니다.

1. 로컬 개발자는 복잡한 빅데이터 인프라를 수동으로 띄우면 온보딩 비용이 커지므로 Compose로 표준화해야 합니다.
2. 운영에서는 애플리케이션과 프록시, Redis를 같은 배포 단위로 묶되, DB는 외부 RDS 환경 변수를 받도록 분리하는 편이 관리가 쉽습니다.
3. Prometheus/Grafana를 통해 JVM/HTTP/서버 리소스를 관찰할 수 있어 장애 대응 근거가 생깁니다.

### 성과

- 로컬 Compose에 MySQL, Redis, Hadoop NameNode/DataNode, Spark Master/Worker, ETL job 컨테이너를 구성했습니다.
- 운영 Compose에 backend, Redis AOF, Nginx HTTPS 프록시를 구성했습니다.
- Prometheus scrape interval 15초, Spring Actuator `/actuator/prometheus`, node-exporter, Grafana dashboard, Alertmanager 구성을 포함했습니다.
- `setup_server_nginx.sh`로 컨테이너 기동, Let's Encrypt 인증서 발급, HTTPS Nginx 재시작을 5단계 자동화했습니다.

## 11. Flyway와 profile 분리로 운영 DB 변경 안정화

### 상황

운영 DB는 개발처럼 `ddl-auto=create/update`로 관리하면 데이터 손실이나 예기치 않은 스키마 변경 위험이 큽니다. 동시에 로컬 개발에서는 초기 데이터와 편의 설정이 필요합니다.

근거 파일:
- `src/main/resources/application-local.yml`
- `src/main/resources/application-prod.yml`
- `src/main/resources/db/migration/V1__drop_pending_location_reserved_day.sql`
- `src/main/resources/db/migration/V2__drop_reputation_score_from_season_ranking_record.sql`

### 해결방법과 그렇게 생각한 이유

profile을 local/prod로 분리하고, prod에서는 `ddl-auto=validate`, Flyway enabled, SQL init never로 설정했습니다.

이유는 다음과 같습니다.

1. 운영에서는 애플리케이션이 엔티티 기준으로 테이블을 자동 변경하기보다, 명시적인 migration 파일로 변경 이력을 남겨야 합니다.
2. `validate`를 사용하면 엔티티와 DB 스키마 불일치를 배포 시점에 빠르게 감지할 수 있습니다.
3. 로컬과 운영의 datasource, Redis, Docker Compose 사용 여부를 환경 변수로 분리해 같은 코드베이스를 여러 환경에서 안전하게 실행할 수 있습니다.

### 성과

- 운영 profile에서 Flyway migration 기반 스키마 변경과 Hibernate validate를 적용했습니다.
- migration 파일 2개로 실제 스키마 변경 이력을 관리하고 있습니다.
- `.env`, `.env.prod`, `SPRING_DOCKER_COMPOSE_ENABLED=false` 등 환경 변수 기반 실행 정책을 문서화했습니다.

## 12. 테스트로 시간/랭킹/정산/상태 저장 같은 고위험 로직 검증

### 상황

이 프로젝트의 위험 지점은 단순 CRUD가 아니라 시간 기반 상태 전환, 실시간 상태 저장, 정산, 랭킹, 주문/액션 검증입니다. 이 영역은 작은 조건 실수도 사용자 결과 불공정으로 이어질 수 있습니다.

근거 파일:
- `src/test/java/com/ssafy/S14P21A205/game/time/policy/GameTimePolicyTests.java`
- `src/test/java/com/ssafy/S14P21A205/game/day/service/GameDayStateServiceTests.java`
- `src/test/java/com/ssafy/S14P21A205/game/day/service/GameDayReportServiceTests.java`
- `src/test/java/com/ssafy/S14P21A205/game/season/scheduler/RealtimeSeasonRankingTickTaskTests.java`
- `src/test/java/com/ssafy/S14P21A205/order/service/OrderServiceImplTests.java`
- `src/test/java/com/ssafy/S14P21A205/action/service/ActionServiceImplTests.java`

### 해결방법과 그렇게 생각한 이유

도메인별로 성공 경로와 실패 경로를 나눠 테스트했습니다. 특히 시간 정책, Redis 저장소 직렬화/역직렬화, 랭킹 정렬, 주문 가능 일차, 액션 중복 사용, 잔액 부족 같은 케이스를 테스트 대상으로 잡았습니다.

이유는 다음과 같습니다.

1. 시간 기반 로직은 운영 중 특정 시각에만 터지는 버그가 많으므로 `Clock` 주입과 정책 테스트가 중요합니다.
2. Redis 상태는 문자열/JSON 변환이 섞이기 때문에 직렬화 테스트가 없으면 런타임에서만 문제가 드러납니다.
3. 주문/액션 검증은 사용자 입력과 게임 밸런스를 동시에 지키는 영역이라 실패 경로 테스트가 특히 중요합니다.

### 성과

- 테스트 파일 29개, 테스트 케이스 106개를 구성했습니다.
- 테스트 코드는 약 5,532라인으로, 메인 Java 코드 약 15,694라인 대비 약 35% 수준의 검증 코드를 보유하고 있습니다.
- 게임 시간, 시즌, 랭킹, 일일 리포트, Redis 상태, 주문, 액션, 보안 설정까지 핵심 백엔드 흐름을 폭넓게 검증했습니다.

## 지원서에 바로 쓰기 좋은 문장 예시

- "실시간 게임 상태를 DB가 아닌 Redis Hash로 분리해 10초 틱 단위의 잦은 상태 갱신을 처리하고, 하루 종료 시점에만 확정 리포트를 DB에 저장하도록 설계했습니다."
- "랭킹 조회 API의 반복 계산 비용을 줄이기 위해 과거 DB 리포트와 당일 Redis 누적값을 10초 주기 배치에서 합산하고, TOP 10 결과를 Redis에 캐싱했습니다."
- "시즌 라이프사이클을 정책 객체로 분리해 입지 선택, 준비, 영업, 리포트, 시즌 요약, 다음 시즌 대기까지 모든 API가 동일한 시간 판단을 사용하도록 만들었습니다."
- "Spark ETL 결과의 배치키와 날짜 정합성을 검증한 뒤 시즌을 시작하도록 설계해 지역별 데이터 불일치로 인한 불공정한 게임 결과를 방지했습니다."
- "OAuth2 로그인 이후 자체 JWT를 발급하고 refresh token jti를 Redis에 저장/회전시켜 로그아웃과 토큰 재발급을 서버 측에서 제어할 수 있게 했습니다."
- "운영 환경에서는 Docker Compose, Nginx HTTPS, Redis AOF, Actuator/Prometheus/Grafana를 구성해 배포와 관측 가능성을 함께 확보했습니다."

