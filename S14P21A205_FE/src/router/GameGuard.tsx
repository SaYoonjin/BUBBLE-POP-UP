import { useEffect, useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getSeasonTime } from "../api/game";
import { phaseToRoute, type SeasonPhase } from "../constants/gameTime";

function isAllowedRoute(phase: SeasonPhase, day: number | null, pathname: string): boolean {
  // LOCATION_SELECTION 페이즈: setup 하위 경로 + waiting 모두 허용
  if (phase === "LOCATION_SELECTION") {
    return pathname.startsWith("/game/setup") || pathname === "/game/waiting";
  }

  // DAY_PREPARING 페이즈: waiting도 허용 (첫 진입 시 대기화면에서 넘어오는 경우)
  if (phase === "DAY_PREPARING" && pathname === "/game/waiting") {
    return true;
  }

  const expected = phaseToRoute(phase, day);
  return expected !== null && pathname === expected;
}

export default function GameGuard() {
  const location = useLocation();
  const [state, setState] = useState<{
    status: "loading" | "allowed" | "redirect";
    redirectTo?: string;
  }>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;

    async function check() {
      try {
        const timeData = await getSeasonTime();
        const phase = timeData.seasonPhase as SeasonPhase;
        const day = timeData.currentDay;

        if (cancelled) return;

        if (isAllowedRoute(phase, day, location.pathname)) {
          setState({ status: "allowed" });
        } else {
          const target = phaseToRoute(phase, day) ?? "/";
          setState({ status: "redirect", redirectTo: target });
        }
      } catch {
        // API 실패 (시즌 없음 등) → 홈으로
        if (!cancelled) {
          setState({ status: "redirect", redirectTo: "/" });
        }
      }
    }

    setState({ status: "loading" });
    check();

    return () => { cancelled = true; };
  }, [location.pathname]);

  if (state.status === "loading") {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#FDFDFB] text-slate-900 font-display">
        <p className="text-lg font-semibold">게임 상태 확인 중...</p>
      </div>
    );
  }

  if (state.status === "redirect") {
    return <Navigate to={state.redirectTo!} replace />;
  }

  return <Outlet />;
}
