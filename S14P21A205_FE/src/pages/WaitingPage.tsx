import { useMemo } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import CountdownTimer from "../components/common/CountdownTimer";
import { LOCATION_SELECTION_DEADLINE_STORAGE_KEY } from "../constants";
import type { WaitingRouteState } from "../types/waiting";

function isWaitingRouteState(value: unknown): value is WaitingRouteState {
  if (!value || typeof value !== "object") {
    return false;
  }

  const state = value as Partial<WaitingRouteState>;

  return (
    (state.mode === "prep_locked" || state.mode === "next_business_day") &&
    typeof state.brandName === "string" &&
    typeof state.districtName === "string" &&
    typeof state.nextPath === "string"
  );
}

export default function WaitingPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const routeState = isWaitingRouteState(location.state) ? location.state : null;
  const locationSelectHref =
    routeState?.mode === "next_business_day"
      ? "/game/setup/location?entry=midseason"
      : "/game/setup/location";

  const content = useMemo(() => {
    if (!routeState) {
      return {
        eyebrow: "Waiting State",
        title: "대기 정보를 찾을 수 없습니다.",
        description: "다시 지역 선택으로 이동해 참여 흐름을 시작해주세요.",
        statusLabel: "대기 정보 없음",
        badgeTone: "border-slate-200 bg-white/80 text-slate-600",
        iconTone: "bg-slate-900 text-white",
        ringTone: "border-slate-200",
      };
    }

    if (routeState.mode === "next_business_day") {
      return {
        eyebrow: "Mid-season Join",
        title: `DAY ${routeState.targetDay ?? 2} 영업 합류 대기 중`,
        description: `${routeState.districtName}에서 "${routeState.brandName}" 팝업 오픈 준비를 마쳤습니다. 다음 영업일이 시작되면 자동으로 입장합니다.`,
        statusLabel: routeState.endTimestampMs
          ? `DAY ${routeState.targetDay ?? 2} 영업 준비까지`
          : "다음 영업일 입장 대기",
        badgeTone: "border-amber-200 bg-amber-50/90 text-amber-700",
        iconTone: "bg-amber-500 text-white",
        ringTone: "border-amber-200/80",
      };
    }

    return {
      eyebrow: "Opening Window",
      title: "오픈 대기 중",
      description: `${routeState.districtName}에서 "${routeState.brandName}" 팝업을 등록했습니다. 팝업 오픈을 위한 설정 시간이 끝나면 영업 준비 화면으로 자동 이동합니다.`,
      statusLabel: "설정 시간 종료까지",
      badgeTone: "border-primary/20 bg-primary/12 text-primary-dark",
      iconTone: "bg-primary-dark text-white",
      ringTone: "border-primary/20",
    };
  }, [routeState]);

  const handleComplete = () => {
    if (routeState?.nextPath) {
      try {
        sessionStorage.removeItem(LOCATION_SELECTION_DEADLINE_STORAGE_KEY);
      } catch {
        // Ignore storage access failures and continue navigation.
      }

      navigate(routeState.nextPath, { replace: true });
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#FDFDFB] px-4 py-8 font-display text-slate-900 sm:px-6">
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage:
            "radial-gradient(circle at 18% 20%, rgba(168,191,169,0.22), transparent 34%), radial-gradient(circle at 82% 18%, rgba(212,165,165,0.18), transparent 28%), radial-gradient(circle at 50% 100%, rgba(248,250,252,0.95), rgba(253,253,251,0.78))",
        }}
      />
      <div className="absolute left-[-4rem] top-[15%] size-40 rounded-full bg-primary/10 blur-3xl" />
      <div className="absolute right-[-5rem] top-[10%] size-52 rounded-full bg-accent-rose/10 blur-3xl" />
      <div className="absolute bottom-[-6rem] left-1/2 size-72 -translate-x-1/2 rounded-full bg-primary/10 blur-3xl" />

      <div className="relative z-10 w-full max-w-3xl">
        <div className="rounded-[2rem] border border-white/70 bg-white/88 p-6 shadow-premium backdrop-blur sm:p-8">
          <div className="flex flex-col gap-8 lg:flex-row lg:items-center lg:justify-between">
            <div className="max-w-xl">
              <div
                className={`inline-flex items-center gap-2 rounded-full border px-3 py-1 text-[11px] font-bold uppercase tracking-[0.24em] ${content.badgeTone}`}
              >
                <span className="material-symbols-outlined text-[14px]">schedule</span>
                <span>{content.eyebrow}</span>
              </div>
              <h1 className="mt-5 text-3xl font-black tracking-tight text-slate-900 sm:text-[2.35rem]">
                {content.title}
              </h1>
              <p className="mt-3 max-w-lg text-sm leading-7 text-slate-500 sm:text-[15px]">
                {content.description}
              </p>

              <div className="mt-6 grid max-w-md grid-cols-1 items-stretch gap-3 sm:grid-cols-2">
                <div className="h-full rounded-2xl border border-slate-200 bg-slate-50/85 px-4 py-3 text-center">
                  <p className="text-[11px] font-bold uppercase tracking-[0.18em] text-slate-400">
                    팝업 브랜드명
                  </p>
                  <p className="mt-1 text-base font-bold text-slate-900">
                    {routeState?.brandName ?? "-"}
                  </p>
                </div>
                <div className="flex h-full flex-col rounded-2xl border border-slate-200 bg-slate-50/85 px-4 py-3 text-center">
                  <p className="text-[11px] font-bold uppercase tracking-[0.18em] text-slate-400">
                    입점 지역
                  </p>
                  <div className="flex flex-1 items-center justify-center">
                    <p className="text-base font-bold text-slate-900">
                      {routeState?.districtName ?? "-"}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div className="flex w-full max-w-sm flex-col items-center rounded-[2rem] border border-slate-100 bg-slate-50/80 px-6 py-7 text-center shadow-soft">
              <div className="relative flex size-40 items-center justify-center">
                <div className={`absolute inset-0 rounded-full border ${content.ringTone} animate-pulse`} />
                <div className={`absolute inset-4 rounded-full border ${content.ringTone} animate-[spin_10s_linear_infinite]`} />
                <div className={`relative flex size-20 items-center justify-center rounded-full shadow-lg ${content.iconTone}`}>
                  <span className="material-symbols-outlined text-[2rem]">schedule</span>
                </div>
              </div>

              <p className="mt-4 text-[11px] font-bold uppercase tracking-[0.24em] text-slate-400">
                {content.statusLabel}
              </p>

              {typeof routeState?.endTimestampMs === "number" ? (
                <div className="mt-3">
                  <CountdownTimer
                    key={`${routeState.mode}-${routeState.endTimestampMs}`}
                    endTimestampMs={routeState.endTimestampMs}
                    label={content.statusLabel}
                    onComplete={handleComplete}
                    variant="display"
                    showIcon={false}
                  />
                </div>
              ) : (
                <div className="mt-3 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-600 shadow-soft">
                  다음 영업일 시작 시 자동 입장
                </div>
              )}

              <p className="mt-4 text-sm leading-6 text-slate-500">
                {routeState?.endTimestampMs
                  ? "남은 시간이 모두 지나면 다음 단계로 자동 이동합니다."
                  : "대기 상태는 실제 영업 오픈 시점과 연결되면 자동 입장 흐름으로 교체됩니다."}
              </p>
            </div>
          </div>

          <div className="mt-8 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-5">
            <p className="text-sm text-slate-400">
              진행 상태를 유지하려면 이 화면을 닫지 마세요.
            </p>
            <Link
              to={locationSelectHref}
              className="inline-flex h-11 items-center justify-center rounded-2xl border border-slate-200 px-4 text-sm font-semibold text-slate-600 transition-colors hover:border-slate-300 hover:text-slate-900"
            >
              지역 선택으로 돌아가기
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
