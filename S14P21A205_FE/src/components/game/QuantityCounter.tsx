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
    <div className="bg-white rounded-3xl p-8 shadow-soft border border-transparent flex flex-col h-full">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-slate-900">수량 준비</h3>
          <p className="text-sm text-slate-400 mt-1">영업 전에 준비할 수량을 정합니다.</p>
        </div>
        <span className="material-symbols-outlined text-slate-300">inventory_2</span>
      </div>

      <div className="flex flex-col justify-center grow gap-8">
        <div className="text-center py-2">
          <p className="text-slate-400 text-sm font-medium mb-2">준비 수량</p>
          <p className="text-6xl font-black text-slate-900 tracking-tight">{quantity}개</p>
        </div>

        <div className="w-full px-2">
          <input
            type="range"
            min={min}
            max={max}
            step={step}
            value={quantity}
            onChange={(e) => onChange(Number(e.target.value))}
            className="w-full h-2 bg-slate-100 rounded-lg appearance-none cursor-pointer accent-primary hover:accent-primary-dark transition-all"
          />
          <div className="flex justify-between text-xs text-slate-400 mt-3 font-medium px-1">
            <span>{min}개</span>
            <span>{max}개</span>
          </div>
          <p className="mt-2 text-xs text-slate-400 px-1">슬라이더로 50개부터 500개까지 조정할 수 있습니다.</p>
        </div>
      </div>
    </div>
  );
}
