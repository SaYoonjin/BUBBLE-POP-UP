import { useState } from "react";

interface PriceSliderProps {
  menuName: string;
  price: number;
  min: number;
  max: number;
  step: number;
  costPrice: number;
  defaultPrice: number;
  defaultPriceLabel: string;
  onChange: (price: number) => void;
}

export default function PriceSlider({
  menuName,
  price,
  min,
  max,
  step,
  costPrice,
  defaultPrice,
  defaultPriceLabel,
  onChange,
}: PriceSliderProps) {
  const [showTooltip, setShowTooltip] = useState(false);
  const margin = price - costPrice;
  const isProfit = margin > 0;

  return (
    <div className="bg-white rounded-[1.5rem] p-6 md:p-7 shadow-soft border border-transparent flex flex-col h-full">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h3 className="text-base md:text-lg font-bold text-slate-900">가격 설정</h3>
          <p className="text-sm text-slate-400 mt-1">{menuName} 판매가를 조정합니다.</p>
        </div>
        <div className="relative">
          <button
            type="button"
            onClick={() => setShowTooltip((prev) => !prev)}
            className="size-9 rounded-full bg-slate-50 border border-slate-200 text-slate-400 hover:text-primary-dark hover:border-primary/20 hover:bg-primary/5 transition-colors flex items-center justify-center"
            aria-label="가격 설정 범위 보기"
          >
            <span className="material-symbols-outlined text-[20px]">payments</span>
          </button>
          {showTooltip && (
            <div className="absolute right-0 top-11 z-10 w-52 rounded-2xl border border-slate-200 bg-white px-3.5 py-3 text-[13px] leading-relaxed text-slate-600 shadow-lg">
              원가부터 권장가의 2배까지 설정할 수 있습니다.
            </div>
          )}
        </div>
      </div>

      <div className="flex flex-col justify-center grow gap-6">
        <div className="text-center py-1">
          <p className="text-slate-400 text-[13px] font-medium mb-2">판매 가격</p>
          <p className="text-[2.5rem] md:text-[2.875rem] font-black text-slate-900 tracking-tight">
            ₩{price.toLocaleString()}
          </p>
          <p className="mt-2.5 text-[13px] text-primary-dark font-semibold">
            {defaultPriceLabel} ₩{defaultPrice.toLocaleString()}
          </p>
        </div>

        <div className="w-full px-1">
          <input
            type="range"
            min={min}
            max={max}
            step={step}
            value={price}
            onChange={(e) => onChange(Number(e.target.value))}
            className="w-full h-2 bg-slate-100 rounded-lg appearance-none cursor-pointer accent-primary hover:accent-primary-dark transition-all"
          />
          <div className="flex justify-between text-xs text-slate-400 mt-2.5 font-medium">
            <span>최소 ₩{min.toLocaleString()}</span>
            <span>최대 ₩{max.toLocaleString()}</span>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 mt-auto pt-3">
          <div className="bg-slate-50 rounded-2xl p-3 text-center">
            <p className="text-xs text-slate-400 font-medium mb-1">원가</p>
            <p className="font-bold text-slate-700 text-base md:text-lg">₩{costPrice.toLocaleString()}</p>
          </div>
          <div className={`rounded-2xl p-3 text-center border ${isProfit ? "bg-primary/5 border-primary/10" : "bg-red-50 border-red-100"}`}>
            <p className={`text-xs font-medium mb-1 ${isProfit ? "text-primary-dark" : "text-red-400"}`}>마진</p>
            <p className={`font-bold text-base md:text-lg ${isProfit ? "text-primary" : "text-red-500"}`}>
              {isProfit ? "+" : ""}₩{margin.toLocaleString()}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
