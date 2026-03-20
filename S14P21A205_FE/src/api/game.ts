import client from "./client";

export type GameWaitingStatus = "WAITING" | "IN_PROGRESS";

export interface GameWaitingResponse {
  status: GameWaitingStatus;
  nextSeasonNumber: number | null;
  currentDay: number | null;
  nextSeasonStartTime: number | null;
  seasonPhase: string | null;
  phaseRemainingSeconds: number | null;
  gameTime: string | null;
  tick: number | null;
  joinEnabled: boolean | null;
  joinPlayableFromDay: number | null;
}

export interface SeasonJoinRequest {
  locationId: number;
  storeName: string;
}

export interface SeasonJoinResponse {
  storeId: number;
  storeName: string;
  balance: number;
  playableFromDay: number;
  waitingForPlayableDay: boolean;
}

export interface CurrentSeasonTopRankingsResponse {
  seasonId: number;
  rankings: Array<{
    ranking: number;
    userName: string;
    storeName: string;
    roi: number;
    revenue: number;
  }>;
  refreshedAt: string;
}

export interface CurrentSeasonRankingItem {
  rank: number | null;
  userId: number;
  nickname: string;
  storeName: string;
  locationName: string;
  menuName: string;
  roi: number;
  totalRevenue: number;
  rewardPoints: number;
  isBankrupt: boolean;
}

export interface CurrentSeasonFinalRankingsResponse {
  seasonId: number;
  rankings: CurrentSeasonRankingItem[];
  myRankings: CurrentSeasonRankingItem[];
}

export async function getCurrentSeasonFinalRankings() {
  const { data } = await client.get<CurrentSeasonFinalRankingsResponse>(
    "/api/game/seasons/current/rankings/final",
  );
  return data;
}

export async function getGameWaitingStatus() {
  const { data } = await client.get<GameWaitingResponse>("/api/game/waiting");
  return data;
}

export async function joinCurrentSeason(payload: SeasonJoinRequest) {
  const { data } = await client.post<SeasonJoinResponse>(
    "/api/game/seasons/current/join",
    payload,
  );
  return data;
}

export async function getCurrentSeasonTopRankings() {
  const { data } = await client.get<CurrentSeasonTopRankingsResponse>(
    "/api/game/seasons/current/rankings/top",
  );
  return data;
}

export interface GameDayReportResponse {
  seasonId: number;
  day: number;
  locationName: string;
  menuName: string;
  revenue: number;
  totalCost: number;
  netProfit: number;
  visitors: number;
  salesCount: number;
  stockRemaining: number;
  stockDisposedCount: number;
  reputationScore: number;
  reputationChange: number;
  tomorrowWeather: { condition: string } | null;
  isNextDayOrderDay: boolean | null;
  consecutiveDeficitDays: number;
  isBankrupt: boolean;
}

export async function getDayReport(day: number) {
  const { data } = await client.get<GameDayReportResponse>(
    `/api/game/day/reports/${day}`,
  );
  return data;
}

export async function getAllDayReports(currentDay: number) {
  const promises = Array.from({ length: currentDay }, (_, i) =>
    getDayReport(i + 1),
  );
  return Promise.all(promises);
}

// --- 영업 중 페이지용 ---

export interface CustomerTick {
  tick: number;
  customerCount: number;
  baseFloatingPopulation: number;
  populationGrowthRate: number;
  currentFloatingPopulation: number;
  regionStoreCount: number;
  rValue: number;
}

export interface GameActionStatus {
  discountUsed: boolean;
  donationUsed: boolean;
  influencerUsed: boolean;
  snsUsed: boolean;
  leafletUsed: boolean;
  friendUsed: boolean;
  emergencyOrderPending: boolean;
  emergencyOrderArriveAt: string | null;
}

export interface AppliedEvent {
  eventType: string;
  eventName: string;
  newsTitle: string;
  appliedAt: string;
}

export interface GameStateResponse {
  serverTime: string;
  seasonId: number;
  day: number;
  population: string;
  lastCalculatedAt: string;
  cash: number;
  customerCount: number;
  customerTick: CustomerTick;
  inventory: { totalStock: number };
  actionStatus: GameActionStatus;
  appliedEvents: AppliedEvent[];
}

export interface CurrentSeasonTimeResponse {
  seasonPhase: string;
  currentDay: number;
  phaseRemainingSeconds: number;
  serverTime: string;
  seasonStartTime: string;
  gameTime: string | null;
  tick: number | null;
  joinEnabled: boolean;
  joinPlayableFromDay: number | null;
}

export interface GameDayStartResponse {
  startTime: string;
  endTime: string;
  weatherType: string;
  weatherMultiplier: number;
  initialBalance: number;
  initialStock: number;
  eventSchedule: Array<{
    time: string;
    type: string;
    newsTitle: string;
    populationMultiplier: number;
    balanceChange: number;
  }>;
  marketSnapshot: {
    avgMenuPrice: number;
    regionStoreCount: number;
    totalFloatingPopulation: number;
  };
}

/** 영업일 시작 */
export async function startGameDay() {
  const { data } = await client.post<GameDayStartResponse>("/api/game/day/start");
  return data;
}

/** 실시간 게임 상태 조회 */
export async function getGameDayState() {
  const { data } = await client.get<GameStateResponse>("/api/game/day/state");
  return data;
}

/** 현재 시즌 시간 정보 (타이머 보정용) */
export async function getSeasonTime() {
  const { data } = await client.get<CurrentSeasonTimeResponse>("/api/game/seasons/time");
  return data;
}
