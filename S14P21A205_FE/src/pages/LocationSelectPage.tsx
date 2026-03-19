import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import CountdownTimer from "../components/common/CountdownTimer";
import DistrictDetailPanel from "../components/game/DistrictDetailPanel";
import SeoulMap3D from "../components/game/SeoulMap3D";
import { seoulDistricts } from "../components/game/seoulDistricts";
import { LOCATION_SELECTION_DEADLINE_STORAGE_KEY } from "../constants";
import type { WaitingRouteState } from "../types/waiting";

const LOCATION_SELECTION_SECONDS = 120;
const PREP_TRANSITION_PATH = "/game/1/prep";
const MID_SEASON_PREP_PATH = "/game/2/prep";
const MOCK_NEXT_BUSINESS_DAY_WAIT_SECONDS = 18;

function parseCurrency(value: string) {
  return Number(value.replace(/[^\d]/g, ""));
}

function formatCurrency(value: number) {
  return `₩${value.toLocaleString("ko-KR")}`;
}

function getOrCreateLocationSelectionDeadline() {
  const nextDeadline = Date.now() + LOCATION_SELECTION_SECONDS * 1000;

  try {
    const storedDeadline = sessionStorage.getItem(
      LOCATION_SELECTION_DEADLINE_STORAGE_KEY,
    );
    const parsedDeadline = Number(storedDeadline);

    if (Number.isFinite(parsedDeadline) && parsedDeadline > Date.now()) {
      return parsedDeadline;
    }

    sessionStorage.setItem(
      LOCATION_SELECTION_DEADLINE_STORAGE_KEY,
      String(nextDeadline),
    );
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

export default function LocationSelectPage() {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectionDeadlineMs] = useState(getOrCreateLocationSelectionDeadline);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const selectedDistrict = seoulDistricts.find((d) => d.id === selectedId);
  const selectedInteriorCost = selectedDistrict
    ? Math.round(parseCurrency(selectedDistrict.rent) * 7 * 0.1)
    : null;
  const joinMode = searchParams.get("entry") === "midseason" ? "midseason" : "season-start";

  const handleComplete = (brandName: string) => {
    if (!selectedDistrict) {
      return;
    }

    const remainingSelectionSeconds = Math.max(
      0,
      Math.ceil((selectionDeadlineMs - Date.now()) / 1000),
    );

    if (joinMode === "midseason") {
      const waitingState: WaitingRouteState = {
        mode: "next_business_day",
        brandName,
        districtName: selectedDistrict.name,
        endTimestampMs: Date.now() + MOCK_NEXT_BUSINESS_DAY_WAIT_SECONDS * 1000,
        nextPath: MID_SEASON_PREP_PATH,
        targetDay: 2,
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
        nextPath: PREP_TRANSITION_PATH,
        targetDay: 1,
      };

      navigate("/game/waiting", { state: waitingState });
      return;
    }

    clearLocationSelectionDeadline();
    navigate(PREP_TRANSITION_PATH);
  };

  const handleTimerComplete = () => {
    clearLocationSelectionDeadline();
    navigate(PREP_TRANSITION_PATH);
  };

  return (
    <div className="relative h-screen w-full overflow-hidden bg-[#FDFDFB] font-display text-slate-800">
      {/* Background decorations */}
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage:
            "radial-gradient(circle at top left, rgba(168,191,169,0.18), transparent 34%), radial-gradient(circle at bottom right, rgba(212,165,165,0.14), transparent 30%)",
        }}
      />
      <div className="absolute left-[-8%] top-[8%] size-56 rounded-full bg-primary/10 blur-3xl" />
      <div className="absolute bottom-[10%] right-[-6%] size-64 rounded-full bg-accent-rose/10 blur-3xl" />

      {/* Top bar */}
      <div className="absolute top-0 left-0 w-full z-[60] p-4 sm:p-6 pointer-events-none">
        <div className="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
          <div className="flex items-center gap-4">
            <div className="flex h-16 items-center gap-3 rounded-[22px] border border-white/70 bg-white/90 px-6 shadow-premium backdrop-blur">
              <div className="flex size-10 items-center justify-center rounded-2xl bg-primary/15 text-primary-dark">
                <span className="material-symbols-outlined text-2xl">location_on</span>
              </div>
              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.24em] text-slate-400">Step 1</p>
                <p className="text-base font-bold leading-tight text-slate-800">지역 선택</p>
              </div>
            </div>
            <p className="hidden text-sm text-slate-500 sm:block">
              2분 안에 지역을 고르고, 팝업 브랜드명을 입력하세요.
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-stretch">
            <div className="flex min-h-16 items-center gap-3 rounded-[22px] border border-white/70 bg-white/90 px-5 py-3 shadow-premium backdrop-blur">
              <div className="flex size-10 items-center justify-center rounded-2xl bg-amber-100 text-amber-600">
                <span className="material-symbols-outlined text-xl">account_balance_wallet</span>
              </div>
              <div className="flex flex-col leading-tight">
                <span className="text-[10px] font-bold uppercase tracking-[0.24em] text-slate-400">초기 자본</span>
                <span className="font-mono text-lg font-bold text-primary-dark">₩10,000,000</span>
              </div>
            </div>

            <div className="flex min-h-16 min-w-[196px] flex-col justify-center rounded-[22px] border border-white/70 bg-white/90 px-5 py-3 shadow-premium backdrop-blur">
              <span className="text-[10px] font-bold uppercase tracking-[0.24em] text-slate-400">제한 시간</span>
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

      {/* 3D Map */}
      <SeoulMap3D selectedId={selectedId} onSelect={setSelectedId} />

      {/* Detail Modal */}
      {selectedDistrict && (
        <DistrictDetailPanel
          district={selectedDistrict}
          interiorCost={selectedInteriorCost !== null ? formatCurrency(selectedInteriorCost) : null}
          onComplete={handleComplete}
          onClose={() => setSelectedId(null)}
        />
      )}
    </div>
  );
}
