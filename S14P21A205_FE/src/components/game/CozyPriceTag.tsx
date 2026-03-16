interface CozyPriceTagProps {
  price: number;
  min: number;
  max: number;
  step: number;
  costPrice: number;
  onChange: (price: number) => void;
}

export default function CozyPriceTag({ price, min, max, step, costPrice, onChange }: CozyPriceTagProps) {
  const margin = price - costPrice;

  return (
    <div className="bg-cozy-cream rounded-xl p-1 relative rotate-1 hover:rotate-0 transition-transform duration-300 origin-top-left shadow-[0_15px_35px_-8px_rgba(0,0,0,0.2),0_6px_12px_-3px_rgba(0,0,0,0.1)]">
      {/* Pin */}
      <div className="absolute -top-3 left-1/2 -translate-x-1/2 size-8 bg-red-500 rounded-full shadow-md z-20 border-2 border-red-700 flex items-center justify-center">
        <div className="size-2 bg-red-300 rounded-full blur-[1px]" />
      </div>

      <div
        className="p-6 rounded-lg border border-cozy-wood-light min-h-[200px] flex flex-col items-center justify-center gap-4 relative"
        style={{
          backgroundImage: "radial-gradient(#d4c5b0 0.5px, transparent 0.5px)",
          backgroundSize: "10px 10px",
          backgroundColor: "#fffdf5",
        }}
      >
        <h3 className="text-[#5D4037] font-bold uppercase tracking-widest text-sm border-b-2 border-[#5D4037]/20 pb-1 w-full text-center">
          판매 가격
        </h3>

        <div className="relative w-full flex flex-col items-center mt-2 gap-4">
          <p className="font-cozy-hand text-7xl font-bold text-[#5D4037] -rotate-1">
            ₩{price.toLocaleString()}
          </p>
          <input
            type="range"
            min={min} max={max} step={step}
            value={price}
            onChange={(e) => onChange(Number(e.target.value))}
            className="w-full h-2 bg-cozy-wood-light rounded-lg appearance-none cursor-pointer accent-cozy-primary"
          />
          <div className="flex justify-between w-full text-xs text-cozy-wood-dark">
            <span>₩{min.toLocaleString()}</span>
            <span>₩{max.toLocaleString()}</span>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 w-full mt-2">
          <div className="bg-white/80 rounded-xl p-3 text-center shadow-sm">
            <p className="text-[10px] text-cozy-wood-dark font-medium">원가</p>
            <p className="font-bold text-[#5D4037]">₩{costPrice.toLocaleString()}</p>
          </div>
          <div className={`rounded-xl p-3 text-center shadow-sm border ${
            margin >= 0
              ? "bg-cozy-sage-green/10 border-cozy-sage-green/20"
              : "bg-red-50 border-red-200"
          }`}>
            <p className={`text-[10px] font-medium ${margin >= 0 ? "text-cozy-sage-green" : "text-red-400"}`}>마진</p>
            <p className={`font-bold ${margin >= 0 ? "text-cozy-sage-green" : "text-red-500"}`}>
              {margin >= 0 ? "+" : ""}₩{margin.toLocaleString()}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
