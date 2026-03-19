# 일일 리포트 페이지 (ReportPage)

## 개요

- **경로**: `/game/:day/report`
- **목적**: 하루 영업 결과를 요약해서 보여주는 리포트 화면
- **API**: `GET /game/day/reports/{day}` → `DayReportResponse`

---

## API 응답 구조

```typescript
interface ProfitByDay {
  first: number | null;    // 1일차 순이익
  second: number | null;   // 2일차 순이익
  third: number | null;    // ...
  fourth: number | null;
  fifth: number | null;
  sixth: number | null;
  seventh: number | null;  // 7일차 순이익
}

interface DayReportResponse {
  seasonId: number;
  day: number;                    // 현재 일차 (1~7)
  storeName: string;              // 가게 이름
  locationName: string;           // 지역명
  menuName: string;               // 메뉴명
  revenue: number;                // 매출
  totalCost: number;              // 지출
  profit: ProfitByDay;            // 7일치 순이익 (미래일은 null)
  visitors: number;               // 방문객 수
  salesCount: number;             // 판매 수량
  stockRemaining: number;         // 남은 재고
  stockDisposedCount: number;     // 폐기된 재고
  reputationScore: number;        // 평판 점수
  reputationChange: number;       // 평판 변동값
  tomorrowWeather: {              // 내일 날씨 (파산 시 null)
    condition: "CLEAR" | "CLOUDY" | "RAIN" | "SNOW" | "STORM"
  } | null;
  isNextDayOrderDay: boolean | null;  // 내일이 발주일인지 (파산 시 null)
  consecutiveDeficitDays: number;     // 연속 적자 일수 (3이면 파산)
}
```

### 정상 응답 예시

```json
{
  "seasonId": 12,
  "day": 3,
  "storeName": "윤진이네 쫀쫀쿠키",
  "locationName": "성수",
  "menuName": "쫀쫀쿠키",
  "revenue": 1200000,
  "totalCost": 320000,
  "profit": {
    "first": 400000,
    "second": 600000,
    "third": 880000,
    "fourth": null,
    "fifth": null,
    "sixth": null,
    "seventh": null
  },
  "visitors": 150,
  "salesCount": 142,
  "stockRemaining": 45,
  "stockDisposedCount": 8,
  "reputationScore": 3.8,
  "reputationChange": 0.2,
  "tomorrowWeather": { "condition": "CLEAR" },
  "isNextDayOrderDay": false,
  "consecutiveDeficitDays": 0
}
```

### 파산 시 응답 예시

```json
{
  "seasonId": 12,
  "day": 5,
  "storeName": "윤진이네 쫀쫀쿠키",
  "locationName": "성수",
  "menuName": "쫀쫀쿠키",
  "revenue": 120000,
  "totalCost": 330000,
  "profit": {
    "first": 400000,
    "second": -100000,
    "third": -300000,
    "fourth": -500000,
    "fifth": -210000,
    "sixth": null,
    "seventh": null
  },
  "visitors": 23,
  "salesCount": 18,
  "stockRemaining": 120,
  "stockDisposedCount": 45,
  "reputationScore": 1.2,
  "reputationChange": -0.8,
  "tomorrowWeather": null,
  "isNextDayOrderDay": null,
  "consecutiveDeficitDays": 3
}
```

---

## 파일 구조

```
src/
├── pages/
│   └── ReportPage.tsx              # 메인 페이지
└── components/
    ├── common/
    │   ├── AppHeader.tsx            # 앱 헤더
    │   ├── Badge.tsx                # 시즌/지역/메뉴 뱃지
    │   ├── CountdownTimer.tsx       # 카운트다운 (pill variant)
    │   └── StatCard.tsx             # 통계 카드 (8개)
    └── report/
        ├── ProfitChart.tsx          # 수익 그래프
        ├── WeatherCard.tsx          # 내일 날씨 카드
        └── BankruptModal.tsx        # 파산 모달
```

---

## UI 구성

### 헤더 영역

- **뱃지 3개**: `시즌 12` (gray) · `성수` (green) · `쫀쫀쿠키` (gold) — Badge 공통 컴포넌트 사용
- **가게 이름**: storeName (큰 제목)
- **일차 정보**: "Day 3 운영 결과를 확인하세요." / 파산 시 "Day 5 영업 종료 — 파산 상태"
- **카운트다운 타이머**: CountdownTimer `pill` variant, 아래에 "다음날로 자동 이동" 텍스트

### 알림 배너

| 조건 | 배너 |
|------|------|
| `consecutiveDeficitDays > 0` (파산 아님) | 빨간 배너: "N일 연속 적자 중 (3일 연속 시 파산)" |
| `consecutiveDeficitDays >= 3` (파산) | 진한 빨간 배너: "파산했습니다: N일 연속 적자 발생" |
| `isNextDayOrderDay === true` | 프라이머리 컬러 배너: "내일은 발주일입니다! 재고를 확인하세요." |

### 통계 카드 (4x2 그리드)

| 카드 | 값 | 비고 |
|------|-----|------|
| 매출 | revenue | - |
| 지출 | totalCost | - |
| 순이익 | profit[현재day] | 적자일 때만 빨간 배경 (`highlight={todayProfit < 0}`) |
| 방문객 수 | visitors | - |
| 평판 | reputationScore | `reputationChange`로 변동값 표시 |
| 판매 수량 | salesCount | subtext에 menuName |
| 남은 재고 | stockRemaining | subtext: 홀수일 "다음날 이월" / 짝수일·7일 "전량 폐기" / 파산 "압류됨" |
| 폐기된 재고 | stockDisposedCount | 50개 초과 시 빨간 배경 |

### 수익 그래프 (ProfitChart)

- `profit` 객체(first~seventh)를 차트 데이터로 변환
- 가장 큰 절대값 기준으로 나머지 바 높이를 비율로 계산
- 양수 바는 0선 위, 음수 바는 0선 아래로 분리 표시
- 음수가 있을 경우 0선(실선) 표시
- 값 라벨은 바 위/아래에 배치 (겹침 방지)
- 바 등장 애니메이션: `duration-700 ease-out`, 순차 delay (`idx * 100ms`)

### 내일 날씨 (WeatherCard)

- condition enum을 프론트에서 매핑:

| condition | emoji | label |
|-----------|-------|-------|
| CLEAR | ☀️ | 맑음 |
| CLOUDY | ☁️ | 흐림 |
| RAIN | 🌧️ | 비 |
| SNOW | ❄️ | 눈 |
| STORM | ⛈️ | 폭풍 |

- 파산 시 (`condition === null`): disabled 상태 (grayscale)

---

## 게임 규칙 반영

### 파산 판별

- `consecutiveDeficitDays >= 3` → 파산
- 파산 시: `tomorrowWeather`, `isNextDayOrderDay` 모두 `null`

### 재고 폐기 규칙

- **홀수일 (1, 3, 5)**: 남은 재고 → 다음날 이월
- **짝수일 (2, 4, 6) + 마지막날 (7)**: 남은 재고 → 전량 폐기

### 발주일

- **짝수일 리포트**에서 `isNextDayOrderDay: true` → "내일은 발주일" 배너 표시
- API의 `isNextDayOrderDay` 값으로 제어

### 파산 모달 (BankruptModal)

- **7일차가 아닌 파산**: "다시 시작하기" 버튼만
- **7일차(마지막날) 파산**: "랭킹 확인하기" + "다시 시작하기" 둘 다 표시
- `isLastDay` prop으로 제어 (`data.day === 7`)

---

## 카운트다운 동작

- 10초 카운트다운
- 정상: 0초 도달 시 `/game/{day+1}/prep`으로 이동
- 파산: 0초 도달 시 BankruptModal 표시

---

## TODO

- [ ] mock 데이터 → 실제 API 호출로 교체 (`GET /game/day/reports/{day}`)
- [ ] AppHeader nickname을 실제 유저 정보로 교체
