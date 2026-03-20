import { useCallback, useEffect, useRef, useState } from "react";
import { Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import { getSeasonTime, type CurrentSeasonTimeResponse } from "../api/game";
import { getStore, type StoreResponse } from "../api/store";
import { phaseToRoute, type SeasonPhase } from "../constants/gameTime";
import { useGameStore } from "../stores/useGameStore";
import type { WaitingRouteState } from "../types/waiting";

/** GameGuard가 하위 페이지에 전달하는 context */
export interface GameGuardContext {
  phase: SeasonPhase;
  day: number;
  phaseRemainingSeconds: number;
  phaseEndTimestamp: number;
}

interface RedirectTarget {
  path: string;
  state?: WaitingRouteState;
}

/** 참여 완료 유저의 경로 허용 판정 */
function isAllowedForJoinedUser(
  phase: SeasonPhase,
  day: number | null,
  pathname: string,
  waitingForPlayableDay: boolean,
): boolean {
  if (waitingForPlayableDay) {
    return pathname === "/game/waiting";
  }
  if (phase === "LOCATION_SELECTION") {
    // 이미 참여한 유저 → waiting + prep 허용 (waiting 타이머 끝나면 prep으로 이동)
    return pathname === "/game/waiting" || pathname === `/game/${day}/prep`;
  }
  if (phase === "DAY_PREPARING" && pathname === "/game/waiting") {
    return true;
  }
  if (phase === "SEASON_SUMMARY" || phase === "NEXT_SEASON_WAITING") {
    return pathname === "/ranking";
  }
  const expected = phaseToRoute(phase, day);
  return expected !== null && pathname === expected;
}

/** 미참여 유저의 경로 허용 판정 */
function isAllowedForNewUser(pathname: string): boolean {
  return pathname.startsWith("/game/setup") || pathname === "/game/waiting";
}

/** waiting 페이지로 보낼 때 route state 생성 */
function buildWaitingState(
  timeData: CurrentSeasonTimeResponse,
  storeData: StoreResponse | null,
  waitingForPlayableDay: boolean,
): WaitingRouteState {
  const brandName = storeData?.popupName ?? "-";
  const districtName = storeData?.location ?? "-";

  if (waitingForPlayableDay) {
    const targetDay = timeData.joinPlayableFromDay ?? (timeData.currentDay + 1);
    return {
      mode: "next_business_day",
      brandName,
      districtName,
      nextPath: `/game/${targetDay}/prep`,
      endTimestampMs: Date.now() + timeData.phaseRemainingSeconds * 1000,
      targetDay,
    };
  }

  // LOCATION_SELECTION 중 이미 참여한 유저 → 오픈 대기
  return {
    mode: "prep_locked",
    brandName,
    districtName,
    nextPath: `/game/${timeData.currentDay}/prep`,
    endTimestampMs: Date.now() + timeData.phaseRemainingSeconds * 1000,
  };
}

/** 참여 완료 유저의 리다이렉트 대상 */
function resolveJoinedUserTarget(
  phase: SeasonPhase,
  day: number | null,
  waitingForPlayableDay: boolean,
  timeData: CurrentSeasonTimeResponse,
  storeData: StoreResponse | null,
): RedirectTarget {
  if (waitingForPlayableDay || phase === "LOCATION_SELECTION") {
    return {
      path: "/game/waiting",
      state: buildWaitingState(timeData, storeData, waitingForPlayableDay),
    };
  }
  return { path: phaseToRoute(phase, day) ?? "/" };
}

/** 미참여 유저의 리다이렉트 대상 */
function resolveNewUserTarget(joinEnabled: boolean): RedirectTarget {
  return { path: joinEnabled ? "/game/setup/location" : "/" };
}

type GuardState =
  | { status: "loading" }
  | { status: "redirect"; target: RedirectTarget }
  | { status: "allowed"; context: GameGuardContext };

export default function GameGuard() {
  const location = useLocation();
  const navigate = useNavigate();
  const [state, setState] = useState<GuardState>({ status: "loading" });
  const timerRef = useRef<ReturnType<typeof setTimeout>>(null);

  const checkAndRoute = useCallback(async () => {
    try {
      let timeData = await getSeasonTime();

      // 남은 시간이 0이면 서버가 아직 페이즈 전환 안 한 것 → 1초 후 재시도
      if (timeData.phaseRemainingSeconds <= 0) {
        await new Promise((r) => setTimeout(r, 1000));
        timeData = await getSeasonTime();
      }

      const phase = timeData.seasonPhase as SeasonPhase;
      const day = timeData.currentDay;
      const remaining = timeData.phaseRemainingSeconds;
      const joinEnabled = timeData.joinEnabled;

      // 참여 여부 확인
      let joined = false;
      let storeData: StoreResponse | null = null;
      try {
        storeData = await getStore();
        joined = true;
      } catch {
        joined = false;
      }

      // gameStore에 playableFromDay가 있으면 join 직후 상태 → 이걸로 판정
      // 없으면 (새로고침 등) → BE StoreResponse에 playableFromDay 없으므로 대기 스킵
      const storedPlayableFromDay = useGameStore.getState().playableFromDay;
      const waitingForPlayableDay = joined
        && storedPlayableFromDay != null
        && day < storedPlayableFromDay;

      // playableFromDay에 도달하면 store 클리어 (더 이상 대기 불필요)
      if (joined && storedPlayableFromDay != null && day >= storedPlayableFromDay) {
        useGameStore.getState().clearGame();
      }

      const pathname = location.pathname;
      let allowed = false;
      let target: RedirectTarget = { path: "/" };

      if (joined) {
        allowed = isAllowedForJoinedUser(phase, day, pathname, waitingForPlayableDay);
        target = resolveJoinedUserTarget(phase, day, waitingForPlayableDay, timeData, storeData);
      } else {
        if (joinEnabled) {
          allowed = isAllowedForNewUser(pathname);
          target = resolveNewUserTarget(joinEnabled);
        }
      }

      if (allowed) {
        setState({
          status: "allowed",
          context: {
            phase,
            day,
            phaseRemainingSeconds: remaining,
            phaseEndTimestamp: Date.now() + remaining * 1000,
          },
        });
        return { allowed: true, remaining };
      }

      setState({ status: "redirect", target });
      return { allowed: false, remaining: 0 };
    } catch {
      setState({ status: "redirect", target: { path: "/" } });
      return { allowed: false, remaining: 0 };
    }
  }, [location.pathname]);

  // 자동 전환 스케줄러
  const scheduleTransition = useCallback((remainingSeconds: number) => {
    if (timerRef.current) clearTimeout(timerRef.current);

    const delayMs = (remainingSeconds + 1) * 1000;

    timerRef.current = setTimeout(async () => {
      try {
        const timeData = await getSeasonTime();
        const phase = timeData.seasonPhase as SeasonPhase;
        const day = timeData.currentDay;

        let joined = false;
        let storeData: StoreResponse | null = null;
        try {
          storeData = await getStore();
          joined = true;
        } catch {
          joined = false;
        }

        const storedPFD = useGameStore.getState().playableFromDay;
        const waiting = joined && storedPFD != null && day < storedPFD;

        let target: RedirectTarget;
        if (joined) {
          target = resolveJoinedUserTarget(phase, day, waiting, timeData, storeData);
        } else {
          target = resolveNewUserTarget(timeData.joinEnabled);
        }

        const stillAllowed = joined
          ? isAllowedForJoinedUser(phase, day, location.pathname, waiting)
          : false;

        if (!stillAllowed) {
          navigate(target.path, { replace: true, state: target.state });
        } else {
          scheduleTransition(timeData.phaseRemainingSeconds);
        }
      } catch {
        navigate("/", { replace: true });
      }
    }, delayMs);
  }, [location.pathname, navigate]);

  // 페이지 진입 시 체크 + 타이머 설정
  useEffect(() => {
    let cancelled = false;

    setState({ status: "loading" });

    checkAndRoute().then((result) => {
      if (cancelled) return;
      // waiting 페이지는 자체 타이머로 전환 처리 → GameGuard 타이머 스킵
      const isWaiting = location.pathname === "/game/waiting";
      if (result.allowed && result.remaining > 0 && !isWaiting) {
        scheduleTransition(result.remaining);
      }
    });

    return () => {
      cancelled = true;
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [checkAndRoute, scheduleTransition]);

  if (state.status === "loading") {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#FDFDFB] text-slate-900 font-display">
        <p className="text-lg font-semibold">게임 상태 확인 중...</p>
      </div>
    );
  }

  if (state.status === "redirect") {
    return <Navigate to={state.target.path} state={state.target.state} replace />;
  }

  return <Outlet context={state.context} />;
}
