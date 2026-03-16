interface QuantityCounterProps {
  quantity: number;
  min?: number;
  max?: number;
  onChange: (quantity: number) => void;
}

export default function QuantityCounter({
  quantity,
  min = 50,
  max = 500,
  onChange,
}: QuantityCounterProps) {
  const adjust = (delta: number) => {
    const next = Math.max(min, Math.min(max, quantity + delta));
    onChange(next);
  };

  return (
    <div className="bg-white rounded-3xl p-8 shadow-soft border border-transparent flex flex-col h-full">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-bold text-slate-900">수량 준비</h3>
        <span className="material-symbols-outlined text-slate-300">inventory_2</span>
      </div>

      <div className="flex flex-col items-center justify-center grow gap-8">
        <div className="text-center">
          <p className="text-7xl font-black text-slate-900">{quantity}</p>
          <p className="text-sm text-slate-400 mt-3 font-medium">준비할 수량 (개)</p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => adjust(-10)}
            className="size-12 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-500 font-bold text-sm transition-colors flex items-center justify-center shadow-sm"
          >
            -10
          </button>
          <button
            onClick={() => adjust(-1)}
            className="size-12 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-500 font-bold text-sm transition-colors flex items-center justify-center shadow-sm"
          >
            -1
          </button>
          <div className="w-4" />
          <button
            onClick={() => adjust(1)}
            className="size-12 rounded-xl bg-primary/10 hover:bg-primary/20 text-primary-dark font-bold text-sm transition-colors border border-transparent flex items-center justify-center"
          >
            +1
          </button>
          <button
            onClick={() => adjust(10)}
            className="size-12 rounded-xl bg-primary/10 hover:bg-primary/20 text-primary-dark font-bold text-sm transition-colors border border-transparent flex items-center justify-center"
          >
            +10
          </button>
        </div>
      </div>
    </div>
  );
}
