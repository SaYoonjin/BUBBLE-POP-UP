import axios from "axios";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { joinCurrentSeason } from "../api/game";
import CountdownTimer from "../components/common/CountdownTimer";
import DistrictDetailPanel from "../components/game/DistrictDetailPanel";
import SeoulMap3D from "../components/game/SeoulMap3D";
import { seoulDistricts } from "../components/game/seoulDistricts";
import { LOCATION_SELECTION_DEADLINE_STORAGE_KEY } from "../constants";
import { setStoredBrandName } from "../hooks/useBrandName";
import type { WaitingRouteState } from "../types/waiting";
import {
  applyDiscount,
  getSelectedDiscountMultiplier,
  getSelectedDiscountPercent,
  getStoredSelectedDashboardItems,
} from "../utils/dashboardItems";

const LOCATION_SELECTION_SECONDS = 120;
const DEFAULT_PREP_DAY = 1;
const INITIAL_CAPITAL = 10_000_000;

function parseCurrency(value: string) {
  return Number(value.replace(/[^\d]/g, ""));
}

function formatCurrency(value: number) {
  return `₩${value.toLocaleString("ko-KR")}`;
}

function getOrCreateLocationSelectionDeadline() {
  const nextDeadline = Date.now() + LOCATION_SELECTION_SECONDS * 1000;

  try {
    const storedDeadline = sessionStorage.getItem(LOCATION_SELECTION_DEADLINE_STORAGE_KEY);
    const parsedDeadline = Number(storedDeadline);

    if (Number.isFinite(parsedDeadline) && parsedDeadline > Date.now()) {
      return parsedDeadline;
    }

    sessionStorage.setItem(LOCATION_SELECTION_DEADLINE_STORAGE_KEY, String(nextDeadline));
  } catch {
    return nextDeadline;
  }

  return nextDeadline;
}

function clearLocationSelectionDeadline() {
  try {
    sessionStorage.removeItem(LOCATION_SELECTION_DEADLINE_STORAGE_KEY);
  } catch {
    // Ignore storage access failures and continue navigation.
  }
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
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectionDeadlineMs] = useState(getOrCreateLocationSelectionDeadline);
  const [isJoining, setIsJoining] = useState(false);
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

  const handleComplete = async (brandName: string) => {
    if (!selectedDistrict || isJoining) {
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
      const nextPrepPath = `/game/${joinResponse.playableFromDay ?? DEFAULT_PREP_DAY}/prep`;
      const remainingSelectionSeconds = Math.max(
        0,
        Math.ceil((selectionDeadlineMs - Date.now()) / 1000),
      );

      if (joinResponse.waitingForPlayableDay) {
        clearLocationSelectionDeadline();

        const waitingState: WaitingRouteState = {
          mode: "next_business_day",
          brandName,
          districtName: selectedDistrict.name,
          nextPath: nextPrepPath,
          targetDay: joinResponse.playableFromDay,
        };

        navigate("/game/waiting", { state: waitingState });
        return;
      }

      if (remainingSelectionSeconds > 0) {
        const waitingState: WaitingRouteState = {
          mode: "prep_locked",
          brandName,
          districtName: selectedDistrict.name,
          endTimestampMs: selectionDeadlineMs,
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
    if (isJoining) {
      return;
    }

    clearLocationSelectionDeadline();
    navigate(`/game/${DEFAULT_PREP_DAY}/prep`);
  };

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
            <p className="hidden text-sm text-slate-500 sm:block">
              2분 안에 지역을 고르고 팝업 브랜드명을 입력해주세요.
            </p>
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
                제한 시간
              </span>
              <div className="mt-1">
                <CountdownTimer
                  endTimestampMs={selectionDeadlineMs}
                  label="지역 선택 제한 시간"
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
