import { useState } from "react";
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
  regions: Region[];
  onClose: () => void;
  onSubmit: (regionId: number) => void;
}

export default function MoveModal({ regions, onClose, onSubmit }: MoveModalProps) {
  const [selectedId, setSelectedId] = useState<number | null>(null);

  return (
    <ModalWrapper onClose={onClose}>
      <div className="p-6 pb-4 border-b border-slate-100 flex items-center justify-between pr-12">
        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
          <span className="text-2xl">🚛</span> 팝업 이전
        </h2>
      </div>

      <div className="p-6 space-y-5">
        {/* Region list */}
        <div className="space-y-3">
          <p className="text-sm font-bold text-slate-500 mb-2">이전 가능한 지역</p>
          {regions.map((region) => (
            <label
              key={region.id}
              className={`group relative flex items-center justify-between p-4 rounded-xl border-2 cursor-pointer transition-all bg-slate-50 hover:bg-white ${
                selectedId === region.id ? "border-rose-soft" : "border-slate-100 hover:border-rose-soft/50"
              }`}
              onClick={() => setSelectedId(region.id)}
            >
              <div className="flex items-center gap-3">
                <div className={`w-10 h-10 rounded-full ${region.iconBg} flex items-center justify-center text-xl`}>
                  {region.icon}
                </div>
                <div>
                  <div className="font-bold text-slate-800">{region.name}</div>
                  <div className="text-xs text-slate-500">
                    유동인구: <span className={`${region.populationColor} font-bold`}>{region.population}</span>
                  </div>
                </div>
              </div>
              <div className="text-right">
                <div className="text-xs text-slate-400">이전 비용</div>
                <div className="font-bold text-slate-800">₩{region.moveCost.toLocaleString()}</div>
              </div>
            </label>
          ))}
        </div>

        {/* Warning */}
        <div className="bg-amber-50 rounded-xl p-4 flex gap-3 border border-amber-100">
          <span className="material-symbols-outlined text-amber-500 shrink-0">warning</span>
          <div className="space-y-1">
            <p className="text-xs font-bold text-amber-700">이동 중 영업 중단 안내</p>
            <p className="text-xs text-amber-600/90 leading-relaxed">지역 이전은 다음 날부터 적용됩니다.</p>
          </div>
        </div>

        <button
          onClick={() => selectedId && onSubmit(selectedId)}
          disabled={!selectedId}
          className="w-full bg-rose-soft hover:bg-rose-dark text-white font-bold py-3.5 rounded-xl shadow-md transition-all active:scale-[0.98] flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <span>지역 이전하기</span>
          <span className="material-symbols-outlined text-sm">arrow_forward</span>
        </button>
      </div>
    </ModalWrapper>
  );
}
