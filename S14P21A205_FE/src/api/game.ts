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
