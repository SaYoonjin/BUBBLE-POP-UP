import { useMemo, useState } from "react";
import { seoulDistricts } from "../../game/seoulDistricts";
import ModalWrapper from "./ModalWrapper";

const HOT_SPOT_IDS = new Set([1, 2, 3, 4, 5, 6, 7, 8]);

const districtIconMap: Record<string, string> = {
  홍대: "🎸",
  여의도: "🏙️",
  명동: "🛍️",
  이태원: "🌍",
  성수: "🧋",
  건대: "🎓",
  강남: "💼",
  잠실: "🏟️",
};

const congestionTone: Record<string, string> = {
  "매우 혼잡": "text-rose-500",
  혼잡: "text-amber-500",
  보통: "text-slate-600",
  여유: "text-sky-600",
  "매우 여유": "text-slate-500",
};

interface MoveRegion {
  id: number;
  name: string;
  rent: string;
  congestion: string;
  moveCost: number;
  icon: string;
  isCurrent: boolean;
}

interface MoveModalProps {
  currentBalance: number;
  currentRegionName: string;
  onClose: () => void;
  onSubmit: (payload: { regionId: number; regionName: string; cost: number }) => void;
}

function parseCurrency(value: string) {
  return Number(value.replace(/[^\d]/g, ""));
}

function formatCurrency(value: number) {
  return `₩${value.toLocaleString("ko-KR")}`;
}

export default function MoveModal({
  currentBalance,
  currentRegionName,
  onClose,
  onSubmit,
}: MoveModalProps) {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [showInfoTooltip, setShowInfoTooltip] = useState(false);

  const regions = useMemo<MoveRegion[]>(
    () =>
      seoulDistricts
        .filter((district) => HOT_SPOT_IDS.has(district.id))
        .map((district) => ({
          id: district.id,
          name: district.name,
          rent: district.rent,
          congestion: district.congestion,
          moveCost: Math.round(parseCurrency(district.rent) * 7 * 0.1),
          icon: districtIconMap[district.name] ?? "📍",
          isCurrent: district.name === currentRegionName,
        })),
    [currentRegionName],
  );

  const selectedRegion = useMemo(
    () => regions.find((region) => region.id === selectedId) ?? null,
    [regions, selectedId],
  );
  const remainingBalance = selectedRegion
    ? currentBalance - selectedRegion.moveCost
    : currentBalance;
  const canAfford = selectedRegion
    ? currentBalance >= selectedRegion.moveCost
    : false;

  return (
    <ModalWrapper onClose={onClose}>
      <style>{`
        @keyframes moveRegionSelect {
          0% {
            transform: translateY(4px) scale(0.985);
            opacity: 0.92;
          }
          100% {
            transform: translateY(0) scale(1);
            opacity: 1;
          }
        }

        @keyframes moveRegionCheck {
          0% {
            transform: scale(0.72);
            opacity: 0;
          }
          100% {
            transform: scale(1);
            opacity: 1;
          }
        }

        @keyframes moveDetailIn {
          0% {
            transform: translateY(8px);
            opacity: 0;
          }
          100% {
            transform: translateY(0);
            opacity: 1;
          }
        }
      `}</style>

      <div className="flex items-center justify-between border-b border-slate-100 p-6 pb-4 pr-12">
        <h2 className="flex items-center gap-2 text-xl font-bold text-slate-800">
          <span className="text-2xl">🚚</span>
          팝업 지역 이전
        </h2>

        <div className="relative shrink-0">
          <button
            type="button"
            onClick={() => setShowInfoTooltip((prev) => !prev)}
            className="flex size-8 items-center justify-center rounded-full border border-amber-200 bg-amber-50 text-amber-600 transition-colors hover:bg-amber-100"
            aria-label="지역 이전 안내 보기"
          >
            <span className="material-symbols-outlined text-[16px]">info</span>
          </button>
          {showInfoTooltip && (
            <div className="absolute right-0 top-[calc(100%+0.5rem)] z-10 w-72 rounded-2xl border border-amber-200 bg-white px-3.5 py-3 text-xs leading-relaxed text-slate-600 shadow-lg">
              지역 이전은 다음 영업일부터 적용되며, 이동 비용과 이후 발생하는 일일 임대료는 별도로 지불해야 합니다.
            </div>
          )}
        </div>
      </div>

      <div className="space-y-5 p-6">
        {!selectedRegion && (
          <div className="space-y-3">
            <div>
              <p className="mb-1 text-sm font-bold text-slate-500">
                이전 가능한 지역
              </p>
              <p className="text-xs text-slate-400">
                이동 비용을 비교해 지역을 고르면, 다음 화면에서 상세 정보와 잔액을 바로 확인할 수 있습니다.
              </p>
            </div>

            <div className="space-y-2">
              {regions.map((region) => {
                const isSelected = selectedId === region.id;

                return (
                  <button
                    key={region.id}
                    type="button"
                    disabled={region.isCurrent}
                    onClick={() => {
                      if (!region.isCurrent) {
                        setSelectedId(region.id);
                      }
                    }}
                    className={`relative flex w-full items-center justify-between rounded-2xl border px-4 py-3.5 text-left transition-all ${
                      region.isCurrent
                        ? "cursor-not-allowed border-dashed border-slate-200 bg-slate-50/90 opacity-85"
                        : isSelected
                          ? "border-primary bg-primary/5 shadow-md ring-1 ring-primary/10"
                          : "border-slate-200 bg-white hover:-translate-y-0.5 hover:border-primary/40 hover:bg-slate-50"
                    }`}
                    style={
                      isSelected
                        ? { animation: "moveRegionSelect 0.22s ease-out" }
                        : undefined
                    }
                  >
                    <div className="flex min-w-0 items-center gap-3">
                      <div
                        className={`flex size-11 shrink-0 items-center justify-center rounded-2xl text-2xl ${
                          region.isCurrent ? "bg-white/70" : "bg-white shadow-sm"
                        }`}
                      >
                        {region.icon}
                      </div>

                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="text-sm font-bold text-slate-900">
                            {region.name}
                          </p>
                          <span
                            className={`rounded-full px-2 py-1 text-[10px] font-bold ${
                              region.isCurrent
                                ? "bg-slate-200 text-slate-600"
                                : "bg-primary/10 text-primary-dark"
                            }`}
                          >
                            {region.isCurrent ? "현재 지역" : "이동 가능"}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="ml-4 flex shrink-0 items-center gap-3">
                      <div className="text-right">
                        <p className="text-[11px] font-medium text-slate-400">
                          이동 비용
                        </p>
                        <p
                          className={`mt-1 text-sm font-bold ${
                            region.isCurrent ? "text-slate-400" : "text-slate-800"
                          }`}
                        >
                          {formatCurrency(region.moveCost)}
                        </p>
                      </div>

                      {isSelected && !region.isCurrent && (
                        <span
                          className="material-symbols-outlined text-[20px] text-primary"
                          style={{ animation: "moveRegionCheck 0.2s ease-out" }}
                        >
                          check_circle
                        </span>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {selectedRegion && (
          <div className="space-y-4" style={{ animation: "moveDetailIn 0.22s ease-out" }}>
            <div className="rounded-2xl border border-primary/15 bg-primary/5 p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="flex min-w-0 items-center gap-3">
                  <div className="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-white text-2xl shadow-sm">
                    {selectedRegion.icon}
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-bold uppercase tracking-[0.18em] text-slate-500">
                      선택한 지역
                    </p>
                    <div className="mt-1 flex flex-wrap items-center gap-2">
                      <h3 className="text-lg font-bold text-slate-900">
                        {selectedRegion.name}
                      </h3>
                      <span className="rounded-full bg-primary/10 px-2.5 py-1 text-[10px] font-bold text-primary-dark">
                        이동 예정
                      </span>
                    </div>
                  </div>
                </div>

                <div className="shrink-0 text-right">
                  <p className="text-[11px] font-medium text-slate-400">
                    이동 비용
                  </p>
                  <p className="mt-1 text-base font-bold text-slate-900">
                    {formatCurrency(selectedRegion.moveCost)}
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setSelectedId(null)}
                className="mt-4 inline-flex items-center gap-1.5 text-sm font-semibold text-primary-dark transition-colors hover:text-primary"
              >
                <span className="material-symbols-outlined text-[18px]">arrow_back</span>
                지역 다시 고르기
              </button>
            </div>

            <div
              className={`rounded-2xl border p-4 ${
                canAfford
                  ? "border-slate-100 bg-slate-50/80"
                  : "border-rose-200 bg-rose-50/70"
              }`}
            >
              <div className="mb-4 grid grid-cols-2 gap-3">
                <div className="rounded-xl border border-slate-100 bg-white/80 p-3">
                  <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">
                    일일 임대료
                  </p>
                  <p className="mt-1 text-sm font-bold text-slate-900">
                    {selectedRegion.rent}
                  </p>
                </div>
                <div className="rounded-xl border border-slate-100 bg-white/80 p-3">
                  <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">
                    혼잡도
                  </p>
                  <p
                    className={`mt-1 text-sm font-bold ${
                      congestionTone[selectedRegion.congestion] ?? "text-slate-600"
                    }`}
                  >
                    {selectedRegion.congestion}
                  </p>
                </div>
              </div>

              <div className="space-y-2.5">
                <div className="flex items-center justify-between text-sm text-slate-600">
                  <span>현재 잔액</span>
                  <span className="font-medium text-slate-800">
                    {formatCurrency(currentBalance)}
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm text-slate-600">
                  <span>이동 비용</span>
                  <span className="font-medium text-slate-800">
                    {formatCurrency(selectedRegion.moveCost)}
                  </span>
                </div>
                <div className="h-px w-full bg-slate-200" />
                <div className="flex items-center justify-between">
                  <span className="text-sm font-bold text-slate-700">
                    실행 후 잔액
                  </span>
                  <span
                    className={`text-lg font-bold ${
                      canAfford ? "text-primary-dark" : "text-rose-500"
                    }`}
                  >
                    {formatCurrency(remainingBalance)}
                  </span>
                </div>
              </div>

              {!canAfford && (
                <p className="mt-3 text-xs font-medium text-rose-500">
                  잔액이 부족해 지역 이전을 예약할 수 없습니다.
                </p>
              )}
            </div>

            <button
              type="button"
              onClick={() =>
                onSubmit({
                  regionId: selectedRegion.id,
                  regionName: selectedRegion.name,
                  cost: selectedRegion.moveCost,
                })
              }
              disabled={!canAfford}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-3.5 font-bold text-white shadow-md transition-all hover:bg-primary-dark hover:shadow-lg active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40"
            >
              <span>지역 이전 예약하기</span>
              <span className="material-symbols-outlined text-sm">arrow_forward</span>
            </button>
          </div>
        )}
      </div>
    </ModalWrapper>
  );
}
