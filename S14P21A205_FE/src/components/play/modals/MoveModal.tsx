import axios from "axios";
import { useMemo, useState } from "react";
import ModalWrapper from "./ModalWrapper";

interface ApiErrorResponse {
  message?: string;
}

export interface MoveRegion {
  id: number;
  name: string;
  rent: number;
  moveCost: number;
  trafficRank: number | null;
  icon: string;
}

interface MoveModalProps {
  currentBalance: number;
  currentRegionName: string;
  regions: MoveRegion[];
  isInitializing?: boolean;
  initializationError?: string | null;
  onClose: () => void;
  onSubmit: (payload: { regionId: number; regionName: string; cost: number }) => Promise<void> | void;
}

function formatCurrency(value: number) {
  return `₩${value.toLocaleString("ko-KR")}`;
}

function formatTrafficRank(rank: number | null) {
  return typeof rank === "number" ? `${rank}위` : "순위 정보 없음";
}

export default function MoveModal({
  currentBalance,
  currentRegionName,
  regions,
  isInitializing = false,
  initializationError = null,
  onClose,
  onSubmit,
}: MoveModalProps) {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [showInfoTooltip, setShowInfoTooltip] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const selectedRegion = useMemo(
    () => regions.find((region) => region.id === selectedId) ?? null,
    [regions, selectedId],
  );
  const remainingBalance = selectedRegion
    ? currentBalance - selectedRegion.moveCost
    : currentBalance;
  const canAfford = selectedRegion ? currentBalance >= selectedRegion.moveCost : false;

  const handleSubmit = async () => {
    if (!selectedRegion || !canAfford || isSubmitting) {
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      await Promise.resolve(
        onSubmit({
          regionId: selectedRegion.id,
          regionName: selectedRegion.name,
          cost: selectedRegion.moveCost,
        }),
      );
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setSubmitError(
          error.response?.data?.message ?? "매장 이전 예약에 실패했습니다. 잠시 후 다시 시도해주세요.",
        );
      } else {
        setSubmitError("매장 이전 예약에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

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
          <span className="text-2xl">📍</span>
          영업 위치 이전
        </h2>

        <div className="relative shrink-0">
          <button
            type="button"
            onClick={() => setShowInfoTooltip((prev) => !prev)}
            className="flex size-8 items-center justify-center rounded-full border border-amber-200 bg-amber-50 text-amber-600 transition-colors hover:bg-amber-100"
            aria-label="매장 이전 안내 보기"
          >
            <span className="material-symbols-outlined text-[16px]">info</span>
          </button>
          {showInfoTooltip && (
            <div className="absolute right-0 top-[calc(100%+0.5rem)] z-10 w-72 rounded-2xl border border-amber-200 bg-white px-3.5 py-3 text-xs leading-relaxed text-slate-600 shadow-lg">
              매장 이전은 다음 영업일부터 적용되며, 이동 비용은 즉시 차감됩니다.
            </div>
          )}
        </div>
      </div>

      <div className="space-y-5 p-6">
        {isInitializing ? (
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
            이전 가능한 지역을 불러오는 중입니다.
          </div>
        ) : initializationError ? (
          <div className="rounded-2xl border border-red-200 bg-red-50/80 px-4 py-8 text-center text-sm text-red-600">
            {initializationError}
          </div>
        ) : !selectedRegion ? (
          <div className="space-y-3">
            <div>
              <p className="mb-1 text-sm font-bold text-slate-500">이전 가능한 지역</p>
              <p className="text-xs text-slate-400">
                지역을 고르면 다음 단계에서 비용과 상세 정보를 확인할 수 있습니다.
              </p>
            </div>

            <div className="space-y-2">
              {regions.map((region) => {
                const isSelected = selectedId === region.id;
                const isCurrent = region.name === currentRegionName;

                return (
                  <button
                    key={region.id}
                    type="button"
                    disabled={isCurrent}
                    onClick={() => {
                      if (!isCurrent) {
                        setSelectedId(region.id);
                      }
                    }}
                    className={`relative flex w-full items-center justify-between rounded-2xl border px-4 py-3.5 text-left transition-all ${
                      isCurrent
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
                          isCurrent ? "bg-white/70" : "bg-white shadow-sm"
                        }`}
                      >
                        {region.icon}
                      </div>

                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="text-sm font-bold text-slate-900">{region.name}</p>
                          <span
                            className={`rounded-full px-2 py-1 text-[10px] font-bold ${
                              isCurrent
                                ? "bg-slate-200 text-slate-600"
                                : "bg-primary/10 text-primary-dark"
                            }`}
                          >
                            {isCurrent ? "현재 지역" : "이동 가능"}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="ml-4 flex shrink-0 items-center gap-3">
                      <div className="text-right">
                        <p className="text-[11px] font-medium text-slate-400">이동 비용</p>
                        <p
                          className={`mt-1 text-sm font-bold ${
                            isCurrent ? "text-slate-400" : "text-slate-800"
                          }`}
                        >
                          {formatCurrency(region.moveCost)}
                        </p>
                      </div>

                      {isSelected && !isCurrent && (
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

            {regions.length === 0 && (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
                표시할 지역이 없습니다.
              </div>
            )}
          </div>
        ) : (
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
                      <h3 className="text-lg font-bold text-slate-900">{selectedRegion.name}</h3>
                      <span className="rounded-full bg-primary/10 px-2.5 py-1 text-[10px] font-bold text-primary-dark">
                        이동 예정
                      </span>
                    </div>
                  </div>
                </div>

                <div className="shrink-0 text-right">
                  <p className="text-[11px] font-medium text-slate-400">이동 비용</p>
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
                <div className="flex flex-col items-center justify-center rounded-xl border border-slate-100 bg-white/80 p-3 text-center">
                  <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">
                    일일 임대료
                  </p>
                  <p className="mt-1 text-sm font-bold text-slate-900">
                    {formatCurrency(selectedRegion.rent)}
                  </p>
                </div>
                <div className="flex flex-col items-center justify-center rounded-xl border border-slate-100 bg-white/80 p-3 text-center">
                  <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">
                    혼잡도 순위
                  </p>
                  <p
                    className={`mt-1 text-sm font-bold ${
                      selectedRegion.trafficRank === null ? "text-slate-500" : "text-slate-900"
                    }`}
                  >
                    {formatTrafficRank(selectedRegion.trafficRank)}
                  </p>
                </div>
              </div>

              <div className="space-y-2.5">
                <div className="flex items-center justify-between text-sm text-slate-600">
                  <span>현재 보유금</span>
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
                  <span className="text-sm font-bold text-slate-700">실행 후 보유금</span>
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
                  보유금이 부족해 매장 이전을 예약할 수 없습니다.
                </p>
              )}
            </div>

            {submitError && (
              <div className="rounded-2xl border border-red-200 bg-red-50/80 px-4 py-3 text-sm text-red-600">
                {submitError}
              </div>
            )}

            <button
              type="button"
              onClick={() => void handleSubmit()}
              disabled={!canAfford || isSubmitting}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-3.5 font-bold text-white shadow-md transition-all hover:bg-primary-dark hover:shadow-lg active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40"
            >
              <span>{isSubmitting ? "이전 예약중..." : "매장 이전 예약하기"}</span>
              <span className="material-symbols-outlined text-sm">arrow_forward</span>
            </button>
          </div>
        )}
      </div>
    </ModalWrapper>
  );
}
