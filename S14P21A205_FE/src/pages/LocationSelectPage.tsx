import axios from "axios";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import type { GameGuardContext } from "../router/GameGuard";
import { getGameWaitingStatus, joinCurrentSeason, type GameWaitingResponse } from "../api/game";
import CountdownTimer from "../components/common/CountdownTimer";
import DistrictDetailPanel from "../components/game/DistrictDetailPanel";
import SeoulMap3D from "../components/game/SeoulMap3D";
import { seoulDistricts } from "../components/game/seoulDistricts";
import { LOCATION_SELECTION_DEADLINE_STORAGE_KEY } from "../constants";
import { clearStoredBrandName, setStoredBrandName } from "../hooks/useBrandName";
import { useGameStore } from "../stores/useGameStore";
import type { WaitingRouteState } from "../types/waiting";
import {
  applyDiscount,
  getSelectedDiscountMultiplier,
  getSelectedDiscountPercent,
  getStoredSelectedDashboardItems,
} from "../utils/dashboardItems";

const DEFAULT_PREP_DAY = 1;
const INITIAL_CAPITAL = 10_000_000;
const PREP_SECONDS = 50;
const BUSINESS_SECONDS = 120;
const REPORT_SECONDS = 10;
const DAY_SECONDS = PREP_SECONDS + BUSINESS_SECONDS + REPORT_SECONDS;
const MIDSEASON_CUTOFF_DAY = 6;

type SelectionMode = "opening_window" | "midseason";

interface SelectionWindowState {
  mode: SelectionMode;
  endTimestampMs: number;
  cutoffTimestampMs?: number;
  timerLabel: string;
  helperText: string;
}

function parseCurrency(value: string) {
  return Number(value.replace(/[^\d]/g, ""));
}

function formatCurrency(value: number) {
  return `₩${value.toLocaleString("ko-KR")}`;
}



function clearLocationSelectionDeadline() {
  try {
    sessionStorage.removeItem(LOCATION_SELECTION_DEADLINE_STORAGE_KEY);
  } catch {
    // Ignore storage access failures and continue navigation.
  }
}

function isLocationSelectionAvailable(waitingStatus: GameWaitingResponse) {
  return (
    waitingStatus.status === "IN_PROGRESS" &&
    typeof waitingStatus.currentDay === "number" &&
    waitingStatus.currentDay >= 1 &&
    waitingStatus.currentDay <= 5
  );
}

function getSecondsUntilDayStart(waitingStatus: GameWaitingResponse, targetDay: number) {
  const currentDay = waitingStatus.currentDay;
  const remaining = Math.max(0, waitingStatus.phaseRemainingSeconds ?? 0);

  if (typeof currentDay !== "number") {
    return remaining;
  }

  if (targetDay <= currentDay) {
    return 0;
  }

  const remainingFullDays = Math.max(0, targetDay - currentDay - 1);

  switch (waitingStatus.seasonPhase) {
    case "LOCATION_SELECTION":
      return remaining + Math.max(0, targetDay - 1) * DAY_SECONDS;
    case "DAY_PREPARING":
      return remaining + BUSINESS_SECONDS + REPORT_SECONDS + remainingFullDays * DAY_SECONDS;
    case "DAY_BUSINESS":
      return remaining + REPORT_SECONDS + remainingFullDays * DAY_SECONDS;
    case "DAY_REPORT":
      return remaining + remainingFullDays * DAY_SECONDS;
    default:
      return remaining;
  }
}

function buildSelectionWindow(
  waitingStatus: GameWaitingResponse,
  locationSelectionDeadlineMs: number,
): SelectionWindowState | null {
  if (!isLocationSelectionAvailable(waitingStatus)) {
    return null;
  }

  const currentDay = waitingStatus.currentDay ?? 1;
  const nextPrepDay = waitingStatus.seasonPhase === "LOCATION_SELECTION" ? 1 : currentDay + 1;

  const baseWindow = {
    endTimestampMs: locationSelectionDeadlineMs,
    timerLabel: `${nextPrepDay}일차 영업 준비까지`,
    helperText: "영업 준비가 시작되기 전에 지역을 고르고 팝업 브랜드명을 입력해주세요.",
  };

  if (waitingStatus.seasonPhase === "LOCATION_SELECTION") {
    return {
      mode: "opening_window",
      ...baseWindow,
    };
  }

  // midseason: 다음 영업 시작(prep)까지의 시간으로 타이머 설정
  const secondsUntilNextPrep = getSecondsUntilDayStart(waitingStatus, nextPrepDay);
  return {
    mode: "midseason",
    ...baseWindow,
    endTimestampMs: Date.now() + secondsUntilNextPrep * 1000,
    cutoffTimestampMs:
      Date.now() + getSecondsUntilDayStart(waitingStatus, MIDSEASON_CUTOFF_DAY) * 1000,
  };
}

function resolveJoinErrorMessage(error: unknown) {
  if (!axios.isAxiosError(error)) {
    return "시즌 참여 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";
  }

  const responseMessage = error.response?.data?.message;

  if (typeof responseMessage === "string" && responseMessage.trim()) {
    return responseMessage;
  }

  if (error.response?.status === 409) {
    return "이미 이번 시즌에 참여했습니다.";
  }

  return "시즌 참여 요청을 완료하지 못했습니다. 입력한 정보와 시즌 상태를 다시 확인해주세요.";
}

export default function LocationSelectPage() {
  const guardContext = useOutletContext<GameGuardContext>();
  // 서버 시간 기반: 현재 페이즈 종료까지 남은 시간
  const [locationSelectionDeadlineMs] = useState(() => guardContext.phaseEndTimestamp);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectionWindow, setSelectionWindow] = useState<SelectionWindowState | null>(null);
  const [isJoining, setIsJoining] = useState(false);
  const [isAccessChecking, setIsAccessChecking] = useState(true);
  const [joinError, setJoinError] = useState<string | null>(null);
  const [selectedDashboardItems] = useState(getStoredSelectedDashboardItems);
  const navigate = useNavigate();

  const selectedDistrict = seoulDistricts.find((district) => district.id === selectedId);
  const selectedInteriorCost = selectedDistrict
    ? Math.round(parseCurrency(selectedDistrict.rent) * 7 * 0.1)
    : null;
  const rentDiscountMultiplier = useMemo(
    () => getSelectedDiscountMultiplier(selectedDashboardItems, "RENT"),
    [selectedDashboardItems],
  );
  const rentDiscountPercent = useMemo(
    () => getSelectedDiscountPercent(selectedDashboardItems, "RENT"),
    [selectedDashboardItems],
  );
  const discountedRent = selectedDistrict
    ? applyDiscount(parseCurrency(selectedDistrict.rent), rentDiscountMultiplier)
    : null;

  useEffect(() => {
    let isCancelled = false;

    async function verifySeasonAccess() {
      try {
        const waitingStatus = await getGameWaitingStatus();

        if (isCancelled) {
          return;
        }

        const nextSelectionWindow = buildSelectionWindow(
          waitingStatus,
          locationSelectionDeadlineMs,
        );

        if (!nextSelectionWindow) {
          clearLocationSelectionDeadline();
          navigate("/", { replace: true });
          return;
        }

        setSelectionWindow(nextSelectionWindow);
        setIsAccessChecking(false);
      } catch {
        if (!isCancelled) {
          clearLocationSelectionDeadline();
          navigate("/", { replace: true });
        }
      }
    }

    void verifySeasonAccess();

    return () => {
      isCancelled = true;
    };
  }, [locationSelectionDeadlineMs, navigate]);

  useEffect(() => {
    if (selectionWindow?.mode !== "midseason" || !selectionWindow.cutoffTimestampMs) {
      return;
    }

    const remainingMs = selectionWindow.cutoffTimestampMs - Date.now();

    if (remainingMs <= 0) {
      clearLocationSelectionDeadline();
      clearStoredBrandName();
      navigate("/", {
        replace: true,
        state: { showMidSeasonSetupExpiredModal: true },
      });
      return;
    }

    const timer = window.setTimeout(() => {
      clearLocationSelectionDeadline();
      clearStoredBrandName();
      navigate("/", {
        replace: true,
        state: { showMidSeasonSetupExpiredModal: true },
      });
    }, remainingMs);

    return () => {
      window.clearTimeout(timer);
    };
  }, [navigate, selectionWindow]);

  const handleComplete = async (brandName: string) => {
    if (!selectedDistrict || isJoining || !selectionWindow) {
      return;
    }

    setJoinError(null);
    setIsJoining(true);
    setStoredBrandName(brandName);

    try {
      const joinResponse = await joinCurrentSeason({
        locationId: selectedDistrict.id,
        storeName: brandName,
      });
      // join 응답의 playableFromDay를 store에 저장 (GameGuard에서 참조)
      useGameStore.getState().setPlayableFromDay(joinResponse.playableFromDay);
      const nextPrepPath = `/game/${joinResponse.playableFromDay ?? DEFAULT_PREP_DAY}/prep`;
      const remainingSelectionSeconds = Math.max(
        0,
        Math.ceil((selectionWindow.endTimestampMs - Date.now()) / 1000),
      );

      if (joinResponse.waitingForPlayableDay) {
        clearLocationSelectionDeadline();

        // 다음 영업준비까지 남은 시간 계산
        const latestStatus = await getGameWaitingStatus();
        const secondsUntilNextPrep = getSecondsUntilDayStart(
          latestStatus,
          joinResponse.playableFromDay,
        );

        const waitingState: WaitingRouteState = {
          mode: "next_business_day",
          brandName,
          districtName: selectedDistrict.name,
          nextPath: nextPrepPath,
          targetDay: joinResponse.playableFromDay,
          endTimestampMs: Date.now() + secondsUntilNextPrep * 1000,
        };

        navigate("/game/waiting", { state: waitingState });
        return;
      }

      if (selectionWindow.mode === "opening_window" && remainingSelectionSeconds > 0) {
        const waitingState: WaitingRouteState = {
          mode: "prep_locked",
          brandName,
          districtName: selectedDistrict.name,
          endTimestampMs: selectionWindow.endTimestampMs,
          nextPath: nextPrepPath,
          targetDay: joinResponse.playableFromDay,
        };

        navigate("/game/waiting", { state: waitingState });
        return;
      }

      clearLocationSelectionDeadline();
      navigate(nextPrepPath);
    } catch (error) {
      setJoinError(resolveJoinErrorMessage(error));
    } finally {
      setIsJoining(false);
    }
  };

  const handleTimerComplete = () => {
    if (isJoining || !selectionWindow) {
      return;
    }

    clearLocationSelectionDeadline();
    // 시간 끝나면 새로고침 → GameGuard가 새 페이즈에 맞게 리다이렉트
    // 6~7일차면 joinEnabled=false → GameGuard가 홈으로 보냄
    navigate(0);
  };

  if (isAccessChecking || !selectionWindow) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#FDFDFB] font-display text-slate-400">
        참여 가능 상태를 확인하는 중입니다.
      </div>
    );
  }

  return (
    <div className="relative h-screen w-full overflow-hidden bg-[#FDFDFB] font-display text-slate-800">
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage:
            "radial-gradient(circle at top left, rgba(168,191,169,0.18), transparent 34%), radial-gradient(circle at bottom right, rgba(212,165,165,0.14), transparent 30%)",
        }}
      />
      <div className="absolute left-[-8%] top-[8%] size-56 rounded-full bg-primary/10 blur-3xl" />
      <div className="absolute bottom-[10%] right-[-6%] size-64 rounded-full bg-accent-rose/10 blur-3xl" />

      <div className="pointer-events-none absolute left-0 top-0 z-[60] w-full p-4 sm:p-6">
        <div className="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
          <div className="flex items-center gap-4">
            <div className="flex h-16 items-center gap-3 rounded-[22px] border border-white/70 bg-white/90 px-6 shadow-premium backdrop-blur">
              <div className="flex size-10 items-center justify-center rounded-2xl bg-primary/15 text-primary-dark">
                <span className="material-symbols-outlined text-2xl">location_on</span>
              </div>
              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.24em] text-slate-400">
                  Step 1
                </p>
                <p className="text-base font-bold leading-tight text-slate-800">지역 선택</p>
              </div>
            </div>
            <p className="hidden text-sm text-slate-500 sm:block">{selectionWindow.helperText}</p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-stretch">
            <div className="flex min-h-16 items-center gap-3 rounded-[22px] border border-white/70 bg-white/90 px-5 py-3 shadow-premium backdrop-blur">
              <div className="flex size-10 items-center justify-center rounded-2xl bg-amber-100 text-amber-600">
                <span className="material-symbols-outlined text-xl">account_balance_wallet</span>
              </div>
              <div className="flex flex-col leading-tight">
                <span className="text-[10px] font-bold uppercase tracking-[0.24em] text-slate-400">
                  초기 자본
                </span>
                <span className="font-mono text-lg font-bold text-primary-dark">
                  {formatCurrency(INITIAL_CAPITAL)}
                </span>
              </div>
            </div>

            <div className="flex min-h-16 min-w-[196px] flex-col justify-center rounded-[22px] border border-white/70 bg-white/90 px-5 py-3 shadow-premium backdrop-blur">
              <span className="text-[10px] font-bold uppercase tracking-[0.24em] text-slate-400">
                {selectionWindow.timerLabel}
              </span>
              <div className="mt-1">
                <CountdownTimer
                  endTimestampMs={selectionWindow.endTimestampMs}
                  label={selectionWindow.timerLabel}
                  onComplete={handleTimerComplete}
                  variant="inline"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <SeoulMap3D selectedId={selectedId} onSelect={setSelectedId} />

      {selectedDistrict && (
        <DistrictDetailPanel
          district={selectedDistrict}
          interiorCost={selectedInteriorCost !== null ? formatCurrency(selectedInteriorCost) : null}
          discountedRent={discountedRent !== null ? formatCurrency(discountedRent) : null}
          rentDiscountLabel={
            rentDiscountPercent > 0 ? `아이템 적용으로 ${rentDiscountPercent}% 할인` : null
          }
          isSubmitting={isJoining}
          submitError={joinError}
          onComplete={handleComplete}
          onClose={() => {
            if (!isJoining) {
              setJoinError(null);
              setSelectedId(null);
            }
          }}
        />
      )}
    </div>
  );
}
