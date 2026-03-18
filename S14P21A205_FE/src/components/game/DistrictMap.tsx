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

const MAP_WIDTH = 920;
const MAP_HEIGHT = 640;

const gradeStyle: Record<string, { token: string; halo: string; area: string; stroke: string }> = {
  S: {
    token: "border-rose-200 bg-accent-rose text-white",
    halo: "shadow-[0_18px_42px_rgba(212,165,165,0.28)]",
    area: "#F7E8E8",
    stroke: "#D4A5A5",
  },
  A: {
    token: "border-amber-200 bg-amber-100 text-amber-800",
    halo: "shadow-[0_18px_42px_rgba(251,191,36,0.2)]",
    area: "#FFF5DD",
    stroke: "#EAC56D",
  },
  B: {
    token: "border-primary/20 bg-primary/15 text-primary-dark",
    halo: "shadow-[0_18px_42px_rgba(168,191,169,0.24)]",
    area: "#ECF3EC",
    stroke: "#A8BFA9",
  },
};

const congestionStyle: Record<string, { chip: string; text: string; label: string }> = {
  "Very High": {
    chip: "border-rose-100 bg-rose-50 text-rose-500",
    text: "text-rose-500",
    label: "매우 붐빔",
  },
  High: {
    chip: "border-amber-100 bg-amber-50 text-amber-600",
    text: "text-amber-600",
    label: "붐빔",
  },
  Medium: {
    chip: "border-primary/20 bg-primary/10 text-primary-dark",
    text: "text-primary-dark",
    label: "보통",
  },
};

function percentToPixels(value: string, size: number) {
  return (Number.parseFloat(value) / 100) * size;
}

function buildConnectionPath(from: District, to: District) {
  const x1 = percentToPixels(from.x, MAP_WIDTH);
  const y1 = percentToPixels(from.y, MAP_HEIGHT);
  const x2 = percentToPixels(to.x, MAP_WIDTH);
  const y2 = percentToPixels(to.y, MAP_HEIGHT);
  const midpointX = (x1 + x2) / 2;
  const midpointY = (y1 + y2) / 2 - Math.max(22, Math.abs(x1 - x2) * 0.12);

  return `M ${x1} ${y1} Q ${midpointX} ${midpointY} ${x2} ${y2}`;
}

function getTooltipClasses(x: string, y: string) {
  const xValue = Number.parseFloat(x);
  const yValue = Number.parseFloat(y);

  if (xValue <= 28) {
    return "left-[calc(100%+16px)] top-1/2 -translate-y-1/2";
  }

  if (xValue >= 74) {
    return "right-[calc(100%+16px)] top-1/2 -translate-y-1/2";
  }

  if (yValue <= 34) {
    return "left-1/2 top-[calc(100%+16px)] -translate-x-1/2";
  }

  return "left-1/2 bottom-[calc(100%+18px)] -translate-x-1/2";
}

function getTooltipPlacement(district: District) {
  if (district.id === 3) {
    return "left-[calc(100%+16px)] top-1/2 -translate-y-1/2";
  }

  return getTooltipClasses(district.x, district.y);
}

export default function DistrictMap({
  districts,
  connections,
  selectedId,
  onSelect,
}: DistrictMapProps) {
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  return (
    <div className="relative flex h-full min-h-[620px] w-full items-center justify-center overflow-hidden">
      <div className="relative aspect-[23/16] w-[min(920px,100%)] overflow-visible rounded-[40px] border border-white/70 bg-white/55 shadow-premium backdrop-blur-sm">
        <div className="absolute inset-0 rounded-[40px] bg-gradient-to-br from-white/75 via-white/40 to-primary/10" />

        <svg
          viewBox={`0 0 ${MAP_WIDTH} ${MAP_HEIGHT}`}
          className="absolute inset-0 h-full w-full"
          aria-hidden="true"
        >
          <defs>
            <linearGradient id="riverFlow" x1="0%" x2="100%" y1="0%" y2="100%">
              <stop offset="0%" stopColor="#DCECF0" stopOpacity="0.96" />
              <stop offset="100%" stopColor="#BDD7DD" stopOpacity="0.98" />
            </linearGradient>
          </defs>

          <path
            d="M110 120C170 78 255 58 341 87C427 117 508 73 606 109C706 146 812 188 842 282C869 366 832 470 736 537C639 604 510 592 418 585C325 578 223 587 148 529C83 479 66 392 73 317C80 245 83 166 110 120Z"
            fill="#FFFFFF"
            fillOpacity="0.85"
            stroke="#8DA98E"
            strokeOpacity="0.24"
            strokeWidth="2.5"
          />

          <path
            d="M118 322C244 286 365 282 481 308C598 335 712 341 820 305"
            fill="none"
            stroke="url(#riverFlow)"
            strokeWidth="36"
            strokeLinecap="round"
            opacity="0.82"
          />
          <path
            d="M118 322C244 286 365 282 481 308C598 335 712 341 820 305"
            fill="none"
            stroke="#8EB4BC"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeDasharray="9 8"
            opacity="0.45"
          />

          <path d="M172 164L262 526" stroke="#DFE8DF" strokeWidth="1.6" opacity="0.85" />
          <path d="M358 110L418 576" stroke="#E5ECE5" strokeWidth="1.5" opacity="0.85" />
          <path d="M592 120L518 562" stroke="#E5ECE5" strokeWidth="1.5" opacity="0.78" />
          <path d="M162 236L774 204" stroke="#E3EBE3" strokeWidth="1.5" opacity="0.8" />
          <path d="M140 408L776 382" stroke="#E9EFE9" strokeWidth="1.5" opacity="0.72" />

          {connections.map((connection) => {
            const from = districts.find((district) => district.id === connection.from);
            const to = districts.find((district) => district.id === connection.to);

            if (!from || !to) {
              return null;
            }

            return (
              <path
                key={`${connection.from}-${connection.to}`}
                d={buildConnectionPath(from, to)}
                fill="none"
                stroke="#A8BFA9"
                strokeWidth="2"
                strokeDasharray="6 7"
                opacity="0.26"
                strokeLinecap="round"
              />
            );
          })}

          {districts.map((district) => {
            const grade = district.grade.charAt(0);
            const gradeMeta = gradeStyle[grade] ?? gradeStyle.B;
            const isActive = district.id === selectedId;
            const isHovered = district.id === hoveredId;
            const centerX = percentToPixels(district.x, MAP_WIDTH);
            const centerY = percentToPixels(district.y, MAP_HEIGHT);

            return (
              <ellipse
                key={`area-${district.id}`}
                cx={centerX}
                cy={centerY}
                rx={grade === "S" ? 64 : 58}
                ry={grade === "S" ? 44 : 40}
                fill={gradeMeta.area}
                stroke={gradeMeta.stroke}
                strokeWidth={isActive ? 2.3 : 1.4}
                opacity={isActive ? 0.92 : isHovered ? 0.68 : 0.4}
                style={{
                  transformOrigin: `${centerX}px ${centerY}px`,
                  transform: isActive || isHovered ? "scale(1.08)" : "scale(1)",
                  transition: "transform 260ms ease, opacity 260ms ease",
                }}
              />
            );
          })}

          <text x="116" y="344" fill="#7EA5AE" opacity="0.4" fontSize="12" fontStyle="italic" fontWeight="700">
            Han River
          </text>
        </svg>

        {districts.map((district) => {
          const isActive = district.id === selectedId;
          const isHovered = district.id === hoveredId;
          const grade = district.grade.charAt(0);
          const gradeMeta = gradeStyle[grade] ?? gradeStyle.B;
          const congestionMeta = congestionStyle[district.congestion] ?? congestionStyle.Medium;

          return (
            <div
              key={district.id}
              className={`absolute ${isActive || isHovered ? "z-30" : "z-10"}`}
              style={{ top: district.y, left: district.x, transform: "translate(-50%, -50%)" }}
              onMouseEnter={() => setHoveredId(district.id)}
              onMouseLeave={() => setHoveredId(null)}
            >
              <div
                className={`relative flex flex-col items-center transition-transform duration-300 ${
                  isActive || isHovered ? "scale-[1.04]" : "scale-100"
                }`}
              >
                <button
                  type="button"
                  onClick={() => onSelect(district.id)}
                  className="group relative flex flex-col items-center"
                >
                  <span
                    className={`relative flex h-6 w-6 items-center justify-center rounded-full border-4 transition-all duration-300 ${
                      isActive
                        ? `border-primary bg-primary-dark text-white ${gradeMeta.halo}`
                        : "border-white bg-primary-dark text-white shadow-soft hover:border-primary/60"
                    }`}
                  >
                    <span className="absolute inset-0 rounded-full border border-white/40" />
                    <span className="material-symbols-outlined text-[12px]">place</span>
                  </span>

                  <span
                    className={`mt-2 flex items-center gap-1.5 rounded-full border border-white/80 bg-white/95 px-3 py-1.5 shadow-soft transition-all duration-300 ${
                      isActive ? "shadow-premium" : ""
                    }`}
                  >
                    <span
                      className={`inline-flex rounded-full border px-2 py-0.5 text-[10px] font-black ${gradeMeta.token}`}
                    >
                      {district.grade}
                    </span>
                    <span className="text-[12px] font-bold text-slate-800">{district.name}</span>
                  </span>

                  <span
                    className={`mt-2 inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-semibold shadow-soft transition-all duration-300 ${
                      congestionMeta.chip
                    } ${isActive || isHovered ? "translate-y-0 opacity-100" : "translate-y-1 opacity-0"}`}
                  >
                    <span className="material-symbols-outlined text-[12px]">local_fire_department</span>
                    {congestionMeta.label}
                  </span>
                </button>

                <div
                  className={`pointer-events-none absolute z-40 min-w-[182px] rounded-[22px] border border-white/80 bg-white/96 px-4 py-3 shadow-premium backdrop-blur transition-all duration-300 ${
                    getTooltipPlacement(district)
                  } ${isActive || isHovered ? "translate-y-0 scale-100 opacity-100" : "translate-y-2 scale-95 opacity-0"}`}
                >
                  <p className="text-[10px] font-bold uppercase tracking-[0.24em] text-slate-400">일일 임대료</p>
                  <p className="mt-1 font-mono text-sm font-bold text-slate-900">{district.rent}</p>
                  <div className="mt-2 flex items-center gap-2">
                    <span className={`text-[11px] font-bold ${congestionMeta.text}`}>{congestionMeta.label}</span>
                    <span className="h-1 w-1 rounded-full bg-slate-300" />
                    <span className="text-[11px] font-semibold text-slate-500">{district.grade}</span>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
