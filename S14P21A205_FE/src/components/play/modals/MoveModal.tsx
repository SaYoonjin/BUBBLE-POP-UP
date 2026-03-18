import { useMemo, useState } from "react";
import ActionBalanceSummary from "../../common/ActionBalanceSummary";
import ModalWrapper from "./ModalWrapper";

interface Region {
  id: number;
  name: string;
  icon: string;
  iconBg: string;
  population: string;
  populationColor: string;
  moveCost: number;
}

interface MoveModalProps {
  currentBalance: number;
  regions: Region[];
  onClose: () => void;
  onSubmit: (payload: { regionId: number; cost: number }) => void;
}

export default function MoveModal({
  currentBalance,
  regions,
  onClose,
  onSubmit,
}: MoveModalProps) {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const selectedRegion = useMemo(
    () => regions.find((region) => region.id === selectedId) ?? null,
    [regions, selectedId],
  );
  const canAfford = selectedRegion
    ? currentBalance >= selectedRegion.moveCost
    : false;

  return (
    <ModalWrapper onClose={onClose}>
      <div className="flex items-center justify-between border-b border-slate-100 p-6 pb-4 pr-12">
        <h2 className="flex items-center gap-2 text-xl font-bold text-slate-800">
          <span className="text-2xl">🧭</span>
          영업 이전
        </h2>
      </div>

      <div className="space-y-5 p-6">
        <div className="space-y-3">
          <p className="mb-2 text-sm font-bold text-slate-500">
            이전 가능한 지역
          </p>
          {regions.map((region) => (
            <button
              key={region.id}
              type="button"
              className={`group relative flex w-full items-center justify-between rounded-xl border-2 bg-slate-50 p-4 text-left transition-all hover:bg-white ${
                selectedId === region.id
                  ? "border-rose-soft"
                  : "border-slate-100 hover:border-rose-soft/50"
              }`}
              onClick={() => setSelectedId(region.id)}
            >
              <div className="flex items-center gap-3">
                <div
                  className={`flex h-10 w-10 items-center justify-center rounded-full text-xl ${region.iconBg}`}
                >
                  {region.icon}
                </div>
                <div>
                  <div className="font-bold text-slate-800">{region.name}</div>
                  <div className="text-xs text-slate-500">
                    유동인구:{" "}
                    <span className={`${region.populationColor} font-bold`}>
                      {region.population}
                    </span>
                  </div>
                </div>
              </div>
              <div className="text-right">
                <div className="text-xs text-slate-400">이동 비용</div>
                <div className="font-bold text-slate-800">
                  ₩{region.moveCost.toLocaleString()}
                </div>
              </div>
            </button>
          ))}
        </div>

        <div className="flex gap-3 rounded-xl border border-amber-100 bg-amber-50 p-4">
          <span className="material-symbols-outlined shrink-0 text-amber-500">
            warning
          </span>
          <div className="space-y-1">
            <p className="text-xs font-bold text-amber-700">
              이동 중 영업 중단 안내
            </p>
            <p className="text-xs leading-relaxed text-amber-600/90">
              지역 이전은 다음 영업일부터 적용됩니다.
            </p>
          </div>
        </div>

        {selectedRegion ? (
          <ActionBalanceSummary
            currentBalance={currentBalance}
            actionCost={selectedRegion.moveCost}
            costLabel="이동 비용"
          />
        ) : (
          <div className="rounded-2xl border border-slate-100 bg-slate-50/80 p-4">
            <div className="mb-2 flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px] text-primary-dark">
                account_balance_wallet
              </span>
              <span className="text-xs font-bold uppercase tracking-[0.18em] text-slate-600">
                잔액 요약
              </span>
            </div>
            <p className="text-sm text-slate-500">
              지역을 선택하면 실행 후 잔액이 계산됩니다.
            </p>
          </div>
        )}

        <button
          type="button"
          onClick={() =>
            selectedRegion &&
            onSubmit({
              regionId: selectedRegion.id,
              cost: selectedRegion.moveCost,
            })
          }
          disabled={!selectedRegion || !canAfford}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-rose-soft py-3.5 font-bold text-white shadow-md transition-all active:scale-[0.98] hover:bg-rose-dark disabled:cursor-not-allowed disabled:opacity-40"
        >
          <span>지역 이전하기</span>
          <span className="material-symbols-outlined text-sm">arrow_forward</span>
        </button>
      </div>
    </ModalWrapper>
  );
}
