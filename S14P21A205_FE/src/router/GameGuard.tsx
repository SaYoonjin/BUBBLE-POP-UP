import { useCallback, useEffect, useRef, useState } from "react";
import { Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import { getSeasonTime } from "../api/game";
import { phaseToRoute, type SeasonPhase } from "../constants/gameTime";

/** GameGuard가 하위 페이지에 전달하는 context */
export interface GameGuardContext {
  phase: SeasonPhase;
  day: number;
  phaseRemainingSeconds: number;
  phaseEndTimestamp: number;
}

function isAllowedRoute(phase: SeasonPhase, day: number | null, pathname: string): boolean {
  // LOCATION_SELECTION: setup 하위 경로 + waiting 허용
  if (phase === "LOCATION_SELECTION") {
    return pathname.startsWith("/game/setup") || pathname === "/game/waiting";
  }

  // DAY_PREPARING: waiting도 허용 (대기화면에서 전환 시)
  if (phase === "DAY_PREPARING" && pathname === "/game/waiting") {
    return true;
  }

  // SEASON_SUMMARY, NEXT_SEASON_WAITING: /ranking 허용
  if (phase === "SEASON_SUMMARY" || phase === "NEXT_SEASON_WAITING") {
    return pathname === "/ranking";
  }

  const expected = phaseToRoute(phase, day);
  return expected !== null && pathname === expected;
}

type GuardState =
  | { status: "loading" }
  | { status: "redirect"; redirectTo: string }
  | { status: "allowed"; context: GameGuardContext };

export default function GameGuard() {
  const location = useLocation();
  const navigate = useNavigate();
  const [state, setState] = useState<GuardState>({ status: "loading" });
  const timerRef = useRef<ReturnType<typeof setTimeout>>(null);

  const checkAndRoute = useCallback(async () => {
    try {
      const timeData = await getSeasonTime();
      const phase = timeData.seasonPhase as SeasonPhase;
      const day = timeData.currentDay;
      const remaining = timeData.phaseRemainingSeconds;

      const correctRoute = phaseToRoute(phase, day) ?? "/";

      // 현재 경로가 허용된 경로인지 확인
      if (isAllowedRoute(phase, day, location.pathname)) {
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
      } else {
        setState({ status: "redirect", redirectTo: correctRoute });
        return { allowed: false, remaining: 0 };
      }
    } catch {
      setState({ status: "redirect", redirectTo: "/" });
      return { allowed: false, remaining: 0 };
    }
  }, [location.pathname]);

  // 자동 전환 스케줄러
  const scheduleTransition = useCallback((remainingSeconds: number) => {
    if (timerRef.current) clearTimeout(timerRef.current);

    // 1초 여유 추가 (서버가 확실히 다음 페이즈로 넘어간 뒤 확인)
    const delayMs = (remainingSeconds + 1) * 1000;

    timerRef.current = setTimeout(async () => {
      try {
        const timeData = await getSeasonTime();
        const phase = timeData.seasonPhase as SeasonPhase;
        const day = timeData.currentDay;
        const nextRoute = phaseToRoute(phase, day) ?? "/";

        // 현재 경로와 다르면 자동 이동
        if (!isAllowedRoute(phase, day, location.pathname)) {
          navigate(nextRoute, { replace: true });
        } else {
          // 같은 페이즈가 계속되면 다시 스케줄
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
      if (result.allowed && result.remaining > 0) {
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
    return <Navigate to={state.redirectTo} replace />;
  }

  return <Outlet context={state.context} />;
}
