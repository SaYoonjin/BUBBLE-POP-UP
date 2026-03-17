interface PriceSliderProps {
  menuName: string;
  price: number;
  min: number;
  max: number;
  step: number;
  costPrice: number;
  recommendedPrice: number;
  defaultPrice: number;
  defaultPriceLabel: string;
  previousPrice?: number;
  onChange: (price: number) => void;
}

export default function PriceSlider({
  menuName,
  price,
  min,
  max,
  step,
  costPrice,
  recommendedPrice,
  defaultPrice,
  defaultPriceLabel,
  previousPrice,
  onChange,
}: PriceSliderProps) {
  const margin = price - costPrice;
  const isProfit = margin > 0;

  return (
    <div className="bg-white rounded-3xl p-8 shadow-soft border border-transparent flex flex-col h-full">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-slate-900">가격 설정</h3>
          <p className="text-sm text-slate-400 mt-1">{menuName} 판매가를 조정합니다.</p>
        </div>
        <span className="material-symbols-outlined text-slate-300">payments</span>
      </div>

      <div className="flex flex-col justify-center grow gap-8">
        <div className="text-center py-2">
          <p className="text-slate-400 text-sm font-medium mb-2">판매 가격</p>
          <p className="text-6xl font-black text-slate-900 tracking-tight">
            ₩{price.toLocaleString()}
          </p>
          <p className="mt-3 text-sm text-primary-dark font-semibold">
            기본값: {defaultPriceLabel} ₩{defaultPrice.toLocaleString()}
          </p>
        </div>

        <div className="w-full px-2">
          <input
            type="range"
            min={min}
            max={max}
            step={step}
            value={price}
            onChange={(e) => onChange(Number(e.target.value))}
            className="w-full h-2 bg-slate-100 rounded-lg appearance-none cursor-pointer accent-primary hover:accent-primary-dark transition-all"
          />
          <div className="flex justify-between text-xs text-slate-400 mt-3 font-medium px-1">
            <span>최소 ₩{min.toLocaleString()}</span>
            <span>최대 ₩{max.toLocaleString()}</span>
          </div>
          <p className="mt-2 text-xs text-slate-400 px-1">범위: 원가부터 권장가의 2배까지</p>
        </div>

        <div className={`grid gap-4 mt-auto pt-4 ${previousPrice ? "grid-cols-3" : "grid-cols-2"}`}>
          <div className="bg-slate-50 rounded-2xl p-4 text-center">
            <p className="text-xs text-slate-400 font-medium mb-1">원가</p>
            <p className="font-bold text-slate-700 text-lg">₩{costPrice.toLocaleString()}</p>
          </div>
          <div className="bg-slate-50 rounded-2xl p-4 text-center">
            <p className="text-xs text-slate-400 font-medium mb-1">권장가</p>
            <p className="font-bold text-slate-700 text-lg">₩{recommendedPrice.toLocaleString()}</p>
          </div>
          {previousPrice !== undefined && (
            <div className="bg-slate-50 rounded-2xl p-4 text-center">
              <p className="text-xs text-slate-400 font-medium mb-1">이전 판매가</p>
              <p className="font-bold text-slate-700 text-lg">₩{previousPrice.toLocaleString()}</p>
            </div>
          )}
          <div className={`rounded-2xl p-4 text-center border ${isProfit ? "bg-primary/5 border-primary/10" : "bg-red-50 border-red-100"}`}>
            <p className={`text-xs font-medium mb-1 ${isProfit ? "text-primary-dark" : "text-red-400"}`}>마진</p>
            <p className={`font-bold text-lg ${isProfit ? "text-primary" : "text-red-500"}`}>
              {isProfit ? "+" : ""}₩{margin.toLocaleString()}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
