interface CozyQuantityDialProps {
  quantity: number;
  min?: number;
  max?: number;
  onChange: (quantity: number) => void;
}

export default function CozyQuantityDial({ quantity, min = 1, max = 500, onChange }: CozyQuantityDialProps) {
  const adjust = (delta: number) => onChange(Math.max(min, Math.min(max, quantity + delta)));

  return (
    <div className="bg-[#263238] rounded-2xl p-6 border-b-8 border-[#1a2327] flex flex-col items-center relative overflow-hidden shadow-[0_20px_40px_-8px_rgba(0,0,0,0.35),0_8px_16px_-4px_rgba(0,0,0,0.2)]">
      {/* Metal reflection */}
      <div className="absolute top-0 left-0 w-full h-1/2 bg-gradient-to-b from-white/10 to-transparent pointer-events-none" />

      <h3 className="text-white/60 text-xs font-bold uppercase tracking-widest mb-4">수량 준비</h3>

      <div className="flex items-center gap-4">
        <div className="flex flex-col gap-2">
          <button
            onClick={() => adjust(-10)}
            className="size-10 rounded-lg bg-[#37474f] text-white shadow-lg active:scale-95 active:bg-[#263238] flex items-center justify-center border-b-4 border-[#1a2327] text-xs font-bold"
          >
            -10
          </button>
          <button
            onClick={() => adjust(-1)}
            className="size-10 rounded-lg bg-[#37474f] text-white shadow-lg active:scale-95 active:bg-[#263238] flex items-center justify-center border-b-4 border-[#1a2327] text-xs font-bold"
          >
            -1
          </button>
        </div>
        <div className="bg-black/40 p-4 rounded-lg border-2 border-[#37474f] shadow-inner min-w-[120px] text-center">
          <span className="font-mono text-5xl text-white font-bold tracking-widest tabular-nums leading-none">
            {String(quantity).padStart(3, "0")}
          </span>
          <p className="text-white/40 text-[10px] mt-2 uppercase tracking-widest">준비 수량 (개)</p>
        </div>
        <div className="flex flex-col gap-2">
          <button
            onClick={() => adjust(10)}
            className="size-10 rounded-lg bg-cozy-primary/80 text-white shadow-lg active:scale-95 flex items-center justify-center border-b-4 border-cozy-primary-dark text-xs font-bold"
          >
            +10
          </button>
          <button
            onClick={() => adjust(1)}
            className="size-10 rounded-lg bg-cozy-primary/80 text-white shadow-lg active:scale-95 flex items-center justify-center border-b-4 border-cozy-primary-dark text-xs font-bold"
          >
            +1
          </button>
        </div>
      </div>

      {/* Bar graph decoration */}
      <div className="flex gap-1 mt-5 h-4 items-end">
        {[3, 4, 6, 8, 5, 7, 9, 6, 4].map((h, i) => (
          <div key={i} className="w-3 rounded-sm bg-cozy-primary" style={{ height: `${h * 4}px`, opacity: 0.3 + h / 10 }} />
        ))}
      </div>
    </div>
  );
}
