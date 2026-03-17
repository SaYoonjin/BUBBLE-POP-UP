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
  return (
    <div className="bg-white rounded-[1.5rem] p-6 md:p-7 shadow-soft border border-transparent flex flex-col h-full">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h3 className="text-base md:text-lg font-bold text-slate-900">수량 준비</h3>
          <p className="text-sm text-slate-400 mt-1">영업 전에 준비할 수량을 정합니다.</p>
        </div>
        <span className="material-symbols-outlined text-[20px] text-slate-300">inventory_2</span>
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
