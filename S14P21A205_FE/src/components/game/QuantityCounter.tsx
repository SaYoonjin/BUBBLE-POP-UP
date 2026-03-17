import { useState } from "react";

interface QuantityCounterProps {
  quantity: number;
  min?: number;
  max?: number;
  step?: number;
  onChange: (quantity: number) => void;
}

export default function QuantityCounter({
  quantity,
  min = 50,
  max = 500,
  step = 10,
  onChange,
}: QuantityCounterProps) {
  const [showTooltip, setShowTooltip] = useState(false);

  return (
    <div className="bg-white rounded-[1.5rem] p-6 md:p-7 shadow-soft border border-transparent flex flex-col h-full">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h3 className="text-base md:text-lg font-bold text-slate-900">수량 준비</h3>
          <p className="text-sm text-slate-400 mt-1">영업 전에 준비할 수량을 정합니다.</p>
        </div>
        <div className="relative">
          <button
            type="button"
            onClick={() => setShowTooltip((prev) => !prev)}
            className="size-9 rounded-full bg-slate-50 border border-slate-200 text-slate-400 hover:text-primary-dark hover:border-primary/20 hover:bg-primary/5 transition-colors flex items-center justify-center"
            aria-label="수량 준비 안내 보기"
          >
            <span className="material-symbols-outlined text-[20px]">inventory_2</span>
          </button>
          {showTooltip && (
            <div className="absolute right-0 top-11 z-10 w-64 rounded-2xl border border-slate-200 bg-white px-3.5 py-3 text-[13px] leading-relaxed text-slate-600 shadow-lg">
              남은 재고는 2일 뒤 폐기됩니다. 정규 발주는 2, 4, 6일차에만 가능하니 과잉 준비에 주의하세요.
            </div>
          )}
        </div>
      </div>

      <div className="flex flex-col justify-center grow gap-6">
        <div className="text-center py-1">
          <p className="text-slate-400 text-[13px] font-medium mb-2">준비 수량</p>
          <p className="text-[2.5rem] md:text-[2.875rem] font-black text-slate-900 tracking-tight">{quantity}개</p>
        </div>

        <div className="w-full px-1">
          <input
            type="range"
            min={min}
            max={max}
            step={step}
            value={quantity}
            onChange={(e) => onChange(Number(e.target.value))}
            className="w-full h-2 bg-slate-100 rounded-lg appearance-none cursor-pointer accent-primary hover:accent-primary-dark transition-all"
          />
          <div className="flex justify-between text-xs text-slate-400 mt-2.5 font-medium">
            <span>{min}개</span>
            <span>{max}개</span>
          </div>
        </div>
      </div>
    </div>
  );
}
