# BubbleBubble Frontend Guide

> 팝업스토어 경영 시뮬레이션 게임 "BubbleBubble"의 프론트엔드 가이드

---

## 1. 기술 스택

| 항목 | 기술 | 버전 |
|------|------|------|
| 프레임워크 | React | 19.2.4 |
| 언어 | TypeScript | ~5.9.3 |
| 빌드 도구 | Vite | - |
| 라우팅 | react-router-dom | 7.13.1 |
| HTTP 클라이언트 | axios | - |
| 스타일링 | Tailwind CSS | 3.4.19 |
| 아이콘 | Material Symbols Outlined (Google CDN) | - |

```bash
# 개발 서버 실행
npm run dev          # http://localhost:5173

# 빌드
npm run build        # tsc -b && vite build

# 린트
npm run lint
```

---

## 2. 프로젝트 구조

```
S14P21A205_FE/
├── public/
├── src/
│   ├── main.tsx                    # 앱 진입점 (StrictMode + RouterProvider)
│   ├── App.tsx                     # RouterProvider 렌더링
│   ├── index.css                   # 글로벌 CSS (Tailwind + glass-panel + scrollbar)
│   │
│   ├── router/
│   │   ├── index.tsx               # 전체 라우트 정의
│   │   └── PrivateRoute.tsx        # 인증 체크 (localStorage.accessToken)
│   │
│   ├── pages/                      # 18개 페이지
│   │   ├── LobbyPage.tsx           # 랜딩 페이지
│   │   ├── LoginPage.tsx           # 로그인 (Google, SSAFY)
│   │   ├── DashboardPage.tsx       # 대시보드 (포인트, 아이템, 시즌)
│   │   ├── MyPage.tsx              # 마이페이지 (시즌 기록)
│   │   ├── NewsPage.tsx            # 뉴스 페이지
│   │   ├── LocationSelectPage.tsx  # 지역 선택 (서울 지도)
│   │   ├── BrandNamingPage.tsx     # 브랜드 네이밍 (미구현)
│   │   ├── PrepPage.tsx            # 영업 준비 (메뉴/가격/수량)
│   │   ├── PlayPage.tsx            # ★ 영업 중 (유니티 배경 + React 오버레이)
│   │   ├── ReportPage.tsx          # 일일 리포트 (미구현)
│   │   ├── RankingPage.tsx         # 랭킹 (미구현)
│   │   ├── ComponentTestPage.tsx   # 모던 컴포넌트 테스트
│   │   ├── Cozy*.tsx               # 코지 테마 변형 (6개)
│   │   └── ...
│   │
│   ├── components/
│   │   ├── common/                 # 공통 UI 컴포넌트 (10+개)
│   │   ├── cozy/                   # 코지 테마 전용 컴포넌트
│   │   ├── game/                   # 게임 메카닉 컴포넌트
│   │   ├── play/                   # 영업 중 UI 컴포넌트
│   │   ├── lobby/                  # (비어있음)
│   │   └── ranking/                # (비어있음)
│   │
│   ├── api/
│   │   ├── client.ts               # axios 인스턴스 (인터셉터 설정)
│   │   └── *.ts                    # API 모듈들 (전부 빈 파일, TODO)
│   │
│   ├── hooks/
│   │   └── useAuth.ts              # 인증 훅 (미구현)
│   │
│   ├── types/
│   │   └── *.ts                    # 타입 정의들 (전부 빈 파일, TODO)
│   │
│   └── constants/
│       └── index.ts                # API_BASE_URL 등
│
├── tailwind.config.ts              # 테마 설정 (색상, 폰트, 그림자)
├── vite.config.ts                  # Vite 설정
└── package.json
```

---

## 3. 라우팅 구조

### 3.1 모던 테마 라우트

| 경로 | 페이지 | 인증 | 설명 |
|------|--------|------|------|
| `/` | LobbyPage | X | 랜딩 페이지 |
| `/login` | LoginPage | X | 로그인 |
| `/dashboard` | DashboardPage | O | 대시보드 |
| `/mypage` | MyPage | O | 마이페이지 |
| `/news` | NewsPage | X | 뉴스 |
| `/game/setup/location` | LocationSelectPage | O | 지역 선택 |
| `/game/setup/naming` | BrandNamingPage | O | 브랜드 네이밍 |
| `/game/:day/prep` | PrepPage | O | 영업 준비 |
| `/game/:day/play` | PlayPage | O | **영업 중** |
| `/game/:day/report` | ReportPage | O | 일일 리포트 |
| `/ranking` | RankingPage | O | 랭킹 |

### 3.2 코지 테마 라우트

| 경로 | 페이지 | 설명 |
|------|--------|------|
| `/cozy` | CozyLobbyPage | 코지 랜딩 |
| `/cozy/login` | CozyLoginPage | 코지 로그인 |
| `/cozy/dashboard` | CozyDashboardPage | 코지 대시보드 |
| `/cozy/mypage` | CozyMyPage | 코지 마이페이지 |
| `/cozy/prep` | CozyPrepPage | 코지 영업 준비 |

### 3.3 인증 흐름

```
PrivateRoute.tsx
  → localStorage.accessToken 존재?
    → Yes: children 렌더
    → No: /login 으로 리다이렉트
```

---

## 4. 게임 플로우 (페이지 흐름)

```
[로비] → [로그인] → [대시보드]
                        │
                        ▼
               [지역 선택] ─→ 8개 서울 지역 중 택1
                        │        (홍대, 여의도, 명동, 이태원,
                        │         성수, 건대, 강남, 잠실)
                        ▼
               [브랜드 네이밍] ─→ 팝업스토어 이름 입력
                        │
            ┌───────────┤ (매일 반복)
            ▼           │
     [영업 준비]         │   메뉴 선택, 가격 설정, 수량 결정
            │           │
            ▼           │
      [영업 중] ◄────── │   ★ 유니티 배경 + React UI 오버레이
            │           │   액션: 할인, 긴급발주, 홍보, 나눔, 팝업이전
            │           │
            ▼           │
     [일일 리포트] ──── ┘   매출, 비용, 순이익 확인
            │
            ▼ (시즌 종료 시)
        [랭킹]
```

---

## 5. 디자인 시스템

### 5.1 두 가지 테마

프로젝트에는 **모던(Modern)**과 **코지(Cozy)** 두 가지 디자인 테마가 공존합니다.

#### 모던 테마
- 깔끔하고 미니멀한 디자인
- Glass morphism (반투명 블러 효과)
- sage green (#A8BFA9) 기반 컬러
- Spline Sans + Noto Sans KR 폰트

#### 코지 테마
- 따뜻한 핸드메이드 느낌
- 종이/나무 질감, 회전된 카드
- hot pink (#f4257b) 기반 컬러
- Be Vietnam Pro + Playfair Display + Caveat(손글씨) 폰트
- 우드 배경 / 버블 배경 토글 가능

### 5.2 색상 팔레트

```
── 모던 테마 ──────────────────────────────────────────
primary         #A8BFA9   세이지 그린 (메인 액센트)
primary-dark    #8DA98E   진한 세이지 (호버/활성)
primary-light   #d3e0d4   연한 세이지
accent-rose     #D4A5A5   로즈 액센트
rose-soft       #e6a5a5   부드러운 로즈 (경고)
rose-dark       #d18a8a   진한 로즈
background-light #FDFDFB  배경색
card-light      #FFFFFF   카드 배경

── 코지 테마 ──────────────────────────────────────────
cozy-primary    #f4257b   핫 핑크
cozy-primary-dark #d01663 진한 핑크
cozy-paper      #fdfbf7   크림색 종이
cozy-cream      #F3F1EC   연한 크림
cozy-warm       #E6D8C3   따뜻한 베이지
cozy-ink        #2c2c2c   진한 글씨
cozy-wood-light #e4d4c5   밝은 나무
cozy-wood-dark  #8b5e3c   진한 나무
cozy-sage       #A8BFA9   세이지
cozy-sage-green #8BA888   진한 세이지
cozy-dusty-rose #D9A9B5   더스티 로즈
```

### 5.3 타이포그래피

```
── 모던 ──
font-display  →  "Spline Sans", "Noto Sans KR", sans-serif
font-body     →  "Spline Sans", "Noto Sans KR", sans-serif
font-mono     →  ui-monospace, SFMono-Regular, monospace

── 코지 ──
font-cozy-display  →  "Be Vietnam Pro", sans-serif     (굵은 제목)
font-cozy-serif    →  "Playfair Display", serif         (우아한 헤딩)
font-cozy-hand     →  "Caveat", cursive                 (손글씨 라벨)
```

### 5.4 그림자

```
shadow-soft       →  미세한 그림자 (카드용)
shadow-premium    →  큰 확산 그림자 (프리미엄 카드)
shadow-cozy-paper →  종이 느낌 깊은 그림자
shadow-cozy-float →  떠있는 듯한 강한 그림자
shadow-cozy-clay  →  클레이모피즘 (안팎 그림자)
shadow-cozy-tactile → 버튼 눌림 효과 (border-b + translate-y)
```

### 5.5 글로벌 CSS 유틸리티

```css
/* 반투명 유리 패널 (헤더, 액션바에 사용) */
.glass-panel {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

/* 커스텀 스크롤바 (이벤트 사이드바 등) */
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background-color: #cbd5e1; border-radius: 20px; }
```

### 5.6 아이콘 사용법

```tsx
// Material Symbols Outlined (Google CDN으로 로딩됨)
<span className="material-symbols-outlined text-primary text-[20px]">
  storefront
</span>

// 이모지 (시각적 포인트)
<span className="text-2xl">🏪</span>
```

---

## 6. 컴포넌트 상세

### 6.1 공통 컴포넌트 (`components/common/`)

| 컴포넌트 | 용도 | Props |
|----------|------|-------|
| `Button` | 범용 버튼 | variant: primary/outline/ghost/danger, size: sm/md/lg |
| `Modal` | 모달 다이얼로그 | isOpen, onClose, title, children |
| `Badge` | 태그/상태 표시 | - |
| `Header` | 페이지 헤더 | variant: lobby/dashboard/game |
| `StatCard` | 통계 카드 | label, value |
| `NewsCard` | 뉴스 카드 | title, content, image? |
| `RankCard` | 랭킹 항목 | rank, name, change |
| `ActionButton` | 아이콘+텍스트 버튼 | - |
| `EventToast` | 알림 토스트 | - |
| `FloatingBubbles` | 배경 버블 애니메이션 | size, position, delay |
| `GuestHeader` | 비로그인 헤더 | - |
| `AppHeader` | 로그인 후 헤더 | 프로필 드롭다운 포함 |
| `CountdownTimer` | 카운트다운 | - |
| `ItemSelector` | 아이템 선택기 | 체크박스 리스트 |
| `SeasonCTA` | 시즌 시작 CTA | - |
| `BankruptWarning` | 파산 경고 배너 | - |

### 6.2 게임 컴포넌트 (`components/game/`)

| 컴포넌트 | 용도 | 사용 페이지 |
|----------|------|------------|
| `DistrictMap` | 서울 지역 인터랙티브 맵 | LocationSelectPage |
| `DistrictDetailPanel` | 지역 상세 모달 | LocationSelectPage |
| `MenuSelector` | 메뉴 선택 그리드 | PrepPage |
| `PriceSlider` | 가격 조절 슬라이더 | PrepPage |
| `QuantityCounter` | 수량 조절 (±버튼) | PrepPage |
| `RankingPanel` | 미니 랭킹 패널 | PrepPage |
| `NewsAccordion` | 뉴스 아코디언 | PrepPage |

### 6.3 영업 중 컴포넌트 (`components/play/`)

```
components/play/
├── PlayHeader.tsx              # 상단 헤더
├── EventSidebar.tsx            # 우측 이벤트 피드
├── ActionBar.tsx               # 하단 액션 버튼 5개
└── modals/
    ├── ModalWrapper.tsx        # 공통 모달 래퍼
    ├── DiscountModal.tsx       # 할인
    ├── EmergencyOrderModal.tsx # 긴급발주
    ├── PromotionModal.tsx      # 홍보하기
    ├── ShareModal.tsx          # 나눔
    └── MoveModal.tsx           # 팝업이전
```

#### PlayHeader
```
┌──────────────────────────────────────────────────────────────┐
│ 📍성수 | 🏪윤진이의 까까이 | 🍪쿠키    DAY 3  02:33    👥1,240 👋24 📦85 💰450 │
└──────────────────────────────────────────────────────────────┘
```
- glass-panel 스타일 (반투명 블러)
- 좌: 위치/가게명/메뉴 | 중앙: 날짜/타이머 | 우: 유동인구/손님/재고/잔액

#### EventSidebar
```
┌─────────────────┐
│ 🔔 실시간 이벤트    │
│                 │
│ ▎Good News      │
│ ▎SNS 입소문! 🎉   │
│ ▎+15% 증가       │
│                 │
│ ▎Warning        │
│ ▎갑작스런 소나기 ☔  │
│ ▎야외 손님 감소    │
│                 │
│ ▎System         │
│ ▎재고 부족 ⚠️     │
│ ▎재발주 필요      │
└─────────────────┘
```
- 이벤트 타입별 색상: good(초록), warning(로즈), system(파랑)
- border-l-4로 좌측 색상 바 표시

#### ActionBar
```
         ┌─────────────────────────────────────┐
         │  🏷️   🚚   [ 📢 ]   🤲   🚛      │
         │  할인  긴급  홍보하기  나눔  팝업이전    │
         └─────────────────────────────────────┘
```
- 중앙 "홍보하기"가 더 크고 위로 돌출 (-mt-6)
- 각 버튼 클릭 시 해당 액션 모달 오픈

#### 액션 모달 상세

**🏷️ 할인 (DiscountModal)**
- 할인율 슬라이더 (0~50%, 5% 단위)
- 수요 증가율 자동 계산 (1 + rate × 0.0175)
- 할인 적용가 표시 (원가 취소선 + 할인가)
- 1일 1회 제한 안내

**🚚 긴급발주 (EmergencyOrderModal)**
- 메뉴 아이템 드롭다운 선택
- 수량 입력
- 비용 계산: 원가 × 수량 + 수수료(50%)
- 교통 상태 / 예상 도착 시각 표시
- 경고: "메뉴 변경 시 기존 재고 폐기"

**📢 홍보하기 (PromotionModal)**
- 4가지 채널 카드 선택:
  - 🤳 인플루언서 (₩50,000)
  - 📱 SNS 광고 (₩30,000)
  - 📰 전단지 배포 (₩10,000)
  - 🗣️ 지인 추천 (무료)
- 기대 효과: 유동인구 보너스 +15%

**🤲 나눔 (ShareModal)**
- 현재 재고 표시
- 나눔 수량 입력 (최대 = 현재 재고)
- 효과: 가게 평판 상승
- 1일 1회 제한

**🚛 팝업이전 (MoveModal)**
- 이전 가능 지역 라디오 버튼 선택
- 각 지역: 아이콘, 이름, 유동인구, 이전 비용
- 경고: "이전은 다음 날부터 적용"
- 버튼 색상: rose-soft (다른 모달과 차별화)

### 6.4 코지 테마 컴포넌트 (`components/cozy/`)

| 컴포넌트 | 용도 | 특징 |
|----------|------|------|
| `CozyWoodBackground` | 전체 배경 래퍼 | wood/bubble 모드 토글 (Context API) |
| `CozyButton` | 버튼 | primary/wood/ghost/danger, tactile 그림자 |
| `CozyHeader` | 헤더 | lobby/dashboard/game 변형 |
| `CozyModal` | 모달 | 핀(📌) 장식, 종이 텍스처 |
| `CozyNewsCard` | 뉴스 카드 | 종이 질감, 살짝 회전 |
| `CozySeasonCalendar` | 시즌 달력 | 카운트다운 포함 |
| `CozyStickyNote` | 스티키 노트 | 테이프 장식, yellow/pink |
| `CozyItemChecklist` | 체크리스트 | 점선 구분, 체크박스 |
| `CozySeasonRecord` | 시즌 기록 | 도장 스타일 랭크 |
| `CozyBadge` | 배지 | green/rose/gray/gold |
| `CozyMenuSelector` | 메뉴 선택 | 가격표 스타일 |
| `CozyPriceTag` | 가격표 | 종이 질감 |
| `CozyQuantityDial` | 수량 다이얼 | 회전 다이얼 UI |
| `CozyNotebook` | 노트북 | 랭킹/통계 표시 |
| `CozyNewspaper` | 신문 | 뉴스 기사 스타일 |

---

## 7. API 레이어

### 7.1 HTTP 클라이언트 (`api/client.ts`)

```typescript
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
});

// 자동으로 Authorization 헤더 추가
client.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

### 7.2 API 모듈 (전부 미구현)

```
api/
├── client.ts    ✅ 완료 (axios 인스턴스)
├── auth.ts      ⬜ TODO
├── user.ts      ⬜ TODO
├── game.ts      ⬜ TODO
├── action.ts    ⬜ TODO
├── shop.ts      ⬜ TODO
├── order.ts     ⬜ TODO
├── news.ts      ⬜ TODO
└── store.ts     ⬜ TODO
```

### 7.3 환경 변수

```bash
# .env (파일 미생성 상태)
VITE_API_BASE_URL=http://localhost:8080   # 백엔드 서버 주소
```

---

## 8. 상태 관리

현재 **별도 상태관리 라이브러리 없음**. 모든 상태는 `useState`로 각 컴포넌트에서 로컬 관리.

| 영역 | 현재 방식 | 향후 필요 |
|------|-----------|----------|
| 인증 | localStorage.accessToken | 전역 Auth Context |
| 게임 상태 | 페이지별 useState + MOCK 데이터 | 전역 Game Store (Zustand 등) |
| UI 상태 | 컴포넌트 로컬 state | 그대로 유지 가능 |
| 코지 배경 모드 | CozyWoodBackground의 Context API | 그대로 유지 |

---

## 9. 타입 정의

현재 타입은 각 컴포넌트 파일 내에 인라인 `interface`로 정의되어 있음.

```
types/
├── user.ts      ⬜ 빈 파일
├── auth.ts      ⬜ 빈 파일
├── game.ts      ⬜ 빈 파일
├── action.ts    ⬜ 빈 파일
├── shop.ts      ⬜ 빈 파일
├── order.ts     ⬜ 빈 파일
├── news.ts      ⬜ 빈 파일
└── store.ts     ⬜ 빈 파일
```

### 주요 인라인 타입 예시

```typescript
// LocationSelectPage.tsx
interface District {
  id: number; name: string; x: string; y: string;
  rent: string; congestion: string; grade: string;
  tags: string[]; description: string;
}

// EventSidebar.tsx
interface GameEvent {
  id: number; type: "good" | "warning" | "system";
  title: string; description: string; time: string;
}

// ActionBar.tsx
type ActionType = "discount" | "emergency" | "promotion" | "share" | "move";
```

---

## 10. 주요 페이지 상세

### 10.1 LocationSelectPage (지역 선택)

**경로**: `/game/setup/location`

서울 8개 지역을 인터랙티브 맵으로 보여주고, 선택한 지역에 브랜드명을 입력하는 페이지.

**지역 데이터**:
| 지역 | 임대료 | 혼잡도 | 등급 | 태그 |
|------|--------|--------|------|------|
| 홍대 | ₩400,000 | High | A | Youth, Music, Art |
| 여의도 | ₩350,000 | Medium | B | Finance, Office |
| 명동 | ₩500,000 | Very High | S | Tourist, Shopping |
| 이태원 | ₩300,000 | Medium | B | Global, Food |
| 성수 | ₩300,000 | High | S | Hip_Vibe, Cafe_Tour, Fashion |
| 건대 | ₩250,000 | Medium | B | University, Nightlife |
| 강남 | ₩600,000 | Very High | S | Premium, Business |
| 잠실 | ₩350,000 | High | A | Family, Entertainment |

**동작 흐름**:
1. 지도에서 지역 노드에 마우스 호버 → 임대료/혼잡도 툴팁 표시
2. 지역 클릭 → 모달 팝업 (지역 상세 정보 + 브랜드명 입력)
3. 브랜드명 입력 후 "이 지역에 오픈하기" → `/game/1/prep`로 이동

### 10.2 PrepPage (영업 준비)

**경로**: `/game/:day/prep`

영업 전 메뉴/가격/수량을 결정하는 페이지.

**좌측 패널**: 메뉴 선택 그리드 (마라탕, 붕어빵, 쿠키, 버블티, 샌드위치) → 가격 슬라이더 → 수량 카운터
**우측 패널**: 유동인구/매출 랭킹 + 뉴스 아코디언

### 10.3 PlayPage (영업 중) ★

**경로**: `/game/:day/play`

게임의 핵심 화면. **배경은 Unity**가 렌더링하고, **React는 UI 오버레이**만 담당.

```
┌──────────────────────────────────────────────────────────────┐
│ [PlayHeader]  위치 | 가게명 | 메뉴    DAY N  타이머    통계     │
├──────────────────────────────────────────────┬───────────────┤
│                                              │ [EventSidebar]│
│                                              │               │
│           (Unity 배경 - 투명)                  │  🔔 이벤트     │
│           가게, 손님 등 3D 렌더링               │  ▎Good News   │
│                                              │  ▎Warning     │
│                                              │  ▎System      │
│                                              │               │
│         ┌─────────────────────────┐          │               │
│         │ [ActionBar] 하단 액션바   │          │               │
│         │ 할인 긴급 [홍보] 나눔 이전  │          │               │
│         └─────────────────────────┘          │               │
└──────────────────────────────────────────────┴───────────────┘
```

**모달 동작**: ActionBar 버튼 클릭 → 해당 ActionType의 모달이 중앙에 표시

---

## 11. 스타일링 패턴

### 11.1 Glass Morphism (모던 테마 핵심)

```tsx
// 반투명 블러 패널
<div className="glass-panel rounded-2xl shadow-xl">
  {/* 콘텐츠 */}
</div>
```

### 11.2 모달 패턴

```tsx
// ModalWrapper.tsx
<div className="fixed inset-0 z-50">
  {/* 백드롭: 검은 반투명 + 블러 */}
  <div className="bg-black/40 backdrop-blur-[2px]" onClick={onClose} />
  {/* 모달 본체: 둥근 모서리 + 그림자 + 진입 애니메이션 */}
  <div className="w-[420px] bg-white rounded-[28px] shadow-2xl">
    <button>✕</button>  {/* 우상단 닫기 */}
    {children}
  </div>
</div>
```

### 11.3 버튼 패턴

```tsx
// 주요 액션 버튼
<button className="bg-primary hover:bg-primary-dark text-white font-bold
  py-4 rounded-xl shadow-lg shadow-primary/30
  transition-all active:scale-[0.98]">
  시작하기
</button>

// 위험 액션 버튼 (팝업이전)
<button className="bg-rose-soft hover:bg-rose-dark text-white ...">
  이전하기
</button>
```

### 11.4 카드 패턴

```tsx
// 정보 카드
<div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
  <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">라벨</span>
  <span className="text-lg font-bold text-slate-800">값</span>
</div>
```

---

## 12. 구현 현황

| 영역 | 상태 | 비고 |
|------|------|------|
| 라우팅 | ✅ 완료 | 18개 경로 |
| 모던 UI 컴포넌트 | ✅ 완료 | 10+ 공통 컴포넌트 |
| 코지 UI 컴포넌트 | ✅ 완료 | 15+ 코지 컴포넌트 |
| 로비/로그인 | ✅ 완료 | 모던 + 코지 |
| 대시보드/마이페이지 | ✅ 완료 | 모던 + 코지 |
| 지역 선택 | ✅ 완료 | 맵 + 모달 |
| 영업 준비 | ✅ 완료 | 메뉴/가격/수량 |
| **영업 중** | ✅ **완료** | 헤더 + 이벤트 + 액션바 + 모달 5개 |
| 브랜드 네이밍 | ⬜ 미구현 | stub |
| 일일 리포트 | ⬜ 미구현 | stub |
| 랭킹 | ⬜ 미구현 | stub |
| API 연동 | ⬜ 미구현 | client.ts만 완료, 나머지 빈 파일 |
| 타입 정의 | ⬜ 미구현 | 인라인 interface만 있음 |
| 상태 관리 | ⬜ 미구현 | useState만 사용 중 |
| 인증 훅 | ⬜ 미구현 | useAuth.ts 빈 파일 |

---

## 13. 개발 가이드

### 새 페이지 추가 순서
1. `src/pages/NewPage.tsx` 생성
2. `src/router/index.tsx`에 라우트 추가
3. 인증 필요 시 `PrivateRoute` children 내부에 배치

### 새 컴포넌트 추가 순서
1. 용도에 맞는 디렉토리 선택:
   - `common/` → 범용 UI
   - `game/` → 게임 메카닉 (준비 단계)
   - `play/` → 영업 중 UI
   - `cozy/` → 코지 테마 전용
2. Props를 interface로 정의
3. Tailwind 클래스로 스타일링 (기존 색상/그림자 재사용)

### API 연동 순서
1. `src/types/도메인.ts`에 타입 정의
2. `src/api/도메인.ts`에 API 함수 작성 (client 인스턴스 사용)
3. 페이지에서 `useEffect` + `useState`로 호출 (향후 React Query 도입 가능)

### 환경 변수
```bash
# .env 파일 생성
VITE_API_BASE_URL=http://localhost:8080
```
