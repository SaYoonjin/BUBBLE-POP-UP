import { useState } from "react";

interface District {
  id: number;
  name: string;
  x: string;
  y: string;
  rent: string;
  congestion: string;
  grade: string;
  tags: string[];
  description: string;
}

interface Connection {
  from: number;
  to: number;
}

interface DistrictMapProps {
  districts: District[];
  connections: Connection[];
  selectedId: number | null;
  onSelect: (id: number) => void;
}

const gradeStyle: Record<string, string> = {
  S: "bg-rose-500 text-white",
  A: "bg-amber-400 text-amber-900",
  B: "bg-slate-400 text-white",
};

export default function DistrictMap({ districts, connections, selectedId, onSelect }: DistrictMapProps) {
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  const getPos = (id: number) => {
    const d = districts.find((d) => d.id === id);
    return d ? { x: d.x, y: d.y } : { x: "0%", y: "0%" };
  };

  return (
    <div className="relative w-full h-full flex items-center justify-center overflow-hidden">
      {/* Subtle dot grid */}
      <div className="absolute inset-0 pointer-events-none opacity-[0.06]" style={{
        backgroundImage: "radial-gradient(#334155 1px, transparent 1px)",
        backgroundSize: "32px 32px",
      }} />

      <div className="relative w-[900px] h-[620px]">
        {/* Han River decoration */}
        <svg className="absolute inset-0 w-full h-full pointer-events-none z-0">
          <path
            d="M 0,320 Q 220,280 420,310 Q 620,345 900,295"
            fill="none"
            stroke="#60a5fa"
            strokeWidth="24"
            opacity="0.08"
            strokeLinecap="round"
          />
          <path
            d="M 0,320 Q 220,280 420,310 Q 620,345 900,295"
            fill="none"
            stroke="#60a5fa"
            strokeWidth="2"
            opacity="0.15"
            strokeDasharray="8 6"
            strokeLinecap="round"
          />
          <text x="140" y="335" fill="#60a5fa" opacity="0.2" fontSize="12" fontStyle="italic" fontWeight="500">
            Han River
          </text>
        </svg>

        {/* Connection lines */}
        <svg className="absolute inset-0 w-full h-full pointer-events-none z-[1]">
          {connections.map((c, i) => {
            const from = getPos(c.from);
            const to = getPos(c.to);
            return (
              <line
                key={i}
                x1={from.x} y1={from.y}
                x2={to.x} y2={to.y}
                stroke="#94a3b8"
                strokeDasharray="6 4"
                strokeWidth="1.5"
                opacity="0.25"
              />
            );
          })}
        </svg>

        {/* District nodes */}
        {districts.map((d) => {
          const isActive = d.id === selectedId;
          const isHovered = d.id === hoveredId;
          const grade = d.grade.charAt(0);

          return (
            <div
              key={d.id}
              className="absolute z-10 flex flex-col items-center"
              style={{ top: d.y, left: d.x, transform: "translate(-50%, -50%)" }}
              onMouseEnter={() => setHoveredId(d.id)}
              onMouseLeave={() => setHoveredId(null)}
            >
              {/* Node circle */}
              <button
                onClick={() => onSelect(d.id)}
                className={`
                  relative w-11 h-11 rounded-xl cursor-pointer
                  transition-all duration-300 ease-out
                  flex items-center justify-center text-lg
                  ${isActive
                    ? "bg-indigo-500 shadow-lg shadow-indigo-500/30 scale-110 -translate-y-1 text-white"
                    : "bg-slate-700 shadow-md hover:bg-indigo-500 hover:shadow-lg hover:shadow-indigo-500/20 hover:scale-110 hover:-translate-y-1 text-white"
                  }
                `}
              >
                🏪
                {isActive && (
                  <span className="absolute inset-0 rounded-xl border-2 border-indigo-400 animate-ping opacity-40" />
                )}
              </button>

              {/* Name label */}
              <div className={`
                mt-2 px-3 py-1 rounded-full text-[13px] font-semibold
                whitespace-nowrap border transition-all duration-300
                ${isActive
                  ? "bg-indigo-500 text-white border-indigo-600 shadow-md"
                  : "bg-white text-slate-700 border-slate-200 shadow-sm hover:border-indigo-400 hover:text-indigo-600"
                }
              `}>
                {d.name}
              </div>

              {/* Grade badge */}
              <span className={`
                absolute -top-1 -right-2 text-[9px] font-bold px-1.5 py-0.5 rounded-md
                ${gradeStyle[grade] || gradeStyle.B}
              `}>
                {grade}
              </span>

              {/* Hover tooltip */}
              <div className={`
                absolute bottom-full left-1/2 -translate-x-1/2 mb-3
                bg-white rounded-xl shadow-xl border border-slate-100
                px-4 py-3 min-w-[150px]
                transition-all duration-200 pointer-events-none
                ${isHovered && !isActive ? "opacity-100 visible translate-y-0" : "opacity-0 invisible translate-y-2"}
              `}>
                <div className="absolute -bottom-1.5 left-1/2 -translate-x-1/2 w-3 h-3 bg-white border-r border-b border-slate-100 rotate-45" />
                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-0.5">임대료</p>
                <p className="text-sm font-mono font-bold text-indigo-600">{d.rent}</p>
                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider mt-1.5 mb-0.5">혼잡도</p>
                <p className="text-sm font-bold text-slate-700">{d.congestion}</p>
              </div>
            </div>
          );
        })}

        {/* Map label */}
        <div className="absolute bottom-3 left-4 text-slate-300 text-sm italic select-none tracking-wide">
          Seoul District Map
        </div>

        {/* Compass */}
        <div className="absolute top-4 right-5 flex flex-col items-center text-slate-300 select-none">
          <span className="material-symbols-outlined text-2xl">explore</span>
          <span className="text-[9px] font-bold tracking-widest mt-0.5">N</span>
        </div>
      </div>
    </div>
  );
}
