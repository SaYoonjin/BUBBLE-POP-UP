interface PriceSliderProps {
  price: number;
  min: number;
  max: number;
  step: number;
  costPrice: number;
  onChange: (price: number) => void;
}

export default function PriceSlider({ price, min, max, step, costPrice, onChange }: PriceSliderProps) {
  const margin = price - costPrice;
  const isProfit = margin > 0;

  return (
    <div className="bg-white rounded-3xl p-8 shadow-soft border border-transparent flex flex-col h-full">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-bold text-slate-900">가격 설정</h3>
        <span className="material-symbols-outlined text-slate-300">payments</span>
      </div>

      <div className="flex flex-col justify-center grow gap-8">
        <div className="text-center py-2">
          <p className="text-slate-400 text-sm font-medium mb-2">판매 가격</p>
          <p className="text-6xl font-black text-slate-900 tracking-tight">
            ₩{price.toLocaleString()}
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
            <span>₩{min.toLocaleString()}</span>
            <span>₩{max.toLocaleString()}</span>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 mt-auto pt-4">
          <div className="bg-slate-50 rounded-2xl p-4 text-center">
            <p className="text-xs text-slate-400 font-medium mb-1">원가</p>
            <p className="font-bold text-slate-700 text-lg">₩{costPrice.toLocaleString()}</p>
          </div>
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
