import { useEffect, useState } from "react";

interface DayProfit {
  day: number;
  value: number;
  isCurrent?: boolean;
  isFuture?: boolean;
}

interface ProfitChartProps {
  data: DayProfit[];
  isBankrupt?: boolean;
}

export default function ProfitChart({ data, isBankrupt = false }: ProfitChartProps) {
  const [animated, setAnimated] = useState(false);

  useEffect(() => {
    const raf = requestAnimationFrame(() => setAnimated(true));
    return () => cancelAnimationFrame(raf);
  }, []);

  const realData = data.filter((d) => !d.isFuture);
  const hasNegative = realData.some((d) => d.value < 0);
  const maxAbs = Math.max(...realData.map((d) => Math.abs(d.value)), 1);

  const fmt = (v: number) =>
    v >= 0 ? `₩${(v / 10000).toFixed(0)}만` : `-₩${(Math.abs(v) / 10000).toFixed(0)}만`;

  return (
    <div className={`lg:col-span-2 bg-white rounded-xl p-6 shadow-soft flex flex-col ${
      isBankrupt ? "border border-rose-soft" : "border border-slate-100"
    }`}>
      <div className="flex items-center justify-between mb-6">
        <h3 className={`text-lg font-bold flex items-center gap-2 ${isBankrupt ? "text-rose-dark" : "text-slate-800"}`}>
          <span className={`material-symbols-outlined ${isBankrupt ? "text-rose-soft" : "text-primary"}`}>show_chart</span>
          수익 그래프
        </h3>
      </div>

      {/* Chart */}
      <div className="flex-1 flex flex-col">
        {/* 양수 영역 */}
        <div className="flex items-end gap-3 px-1" style={{ height: hasNegative ? 100 : 160 }}>
          {data.map((d, idx) => {
            if (d.isFuture) {
              return (
                <div key={d.day} className="flex-1 flex flex-col items-center justify-end">
                  <div className="w-full max-w-10 mx-auto rounded-t-md bg-slate-50 border border-dashed border-slate-200" style={{ height: 12 }} />
                </div>
              );
            }
            if (d.value < 0) {
              return <div key={d.day} className="flex-1" />;
            }
            const heightPercent = (d.value / maxAbs) * 100;
            const barColor = d.isCurrent
              ? "bg-primary shadow-md shadow-primary/20"
              : "bg-primary/20";

            return (
              <div key={d.day} className="flex-1 flex flex-col items-center justify-end relative group h-full">
                {/* 값 라벨 */}
                <span className={`text-[11px] font-semibold mb-1 whitespace-nowrap transition-opacity ${
                  d.isCurrent ? "opacity-100" : "opacity-0 group-hover:opacity-100"
                } ${d.isCurrent ? "text-primary-dark" : "text-slate-500"}`}>
                  {fmt(d.value)}
                </span>
                <div
                  className={`w-full max-w-10 mx-auto rounded-t-md transition-all duration-700 ease-out ${barColor}`}
                  style={{
                    height: animated ? `${Math.max(heightPercent, 4)}%` : "0%",
                    transitionDelay: `${idx * 100}ms`,
                  }}
                />
              </div>
            );
          })}
        </div>

        {/* 0선 (음수가 있을 때만) */}
        {hasNegative && (
          <div className="flex items-center gap-3 px-1">
            <div className="flex-1 border-t border-slate-300" />
          </div>
        )}

        {/* 음수 영역 */}
        {hasNegative && (
          <div className="flex items-start gap-3 px-1" style={{ height: 100 }}>
            {data.map((d, idx) => {
              if (d.isFuture || d.value >= 0) {
                return <div key={d.day} className="flex-1" />;
              }
              const heightPercent = (Math.abs(d.value) / maxAbs) * 100;
              const barColor = d.isCurrent
                ? "bg-rose-soft shadow-md shadow-rose-soft/30"
                : "bg-rose-soft/40";

              return (
                <div key={d.day} className="flex-1 flex flex-col items-center justify-start relative group h-full">
                  <div
                    className={`w-full max-w-10 mx-auto rounded-b-md transition-all duration-700 ease-out ${barColor}`}
                    style={{
                      height: animated ? `${Math.max(heightPercent, 4)}%` : "0%",
                      transitionDelay: `${idx * 100}ms`,
                    }}
                  />
                  {/* 값 라벨 */}
                  <span className={`text-[11px] font-semibold mt-1 whitespace-nowrap transition-opacity ${
                    d.isCurrent ? "opacity-100" : "opacity-0 group-hover:opacity-100"
                  } text-rose-dark`}>
                    {fmt(d.value)}
                  </span>
                </div>
              );
            })}
          </div>
        )}

        {/* Day labels */}
        <div className="flex gap-3 px-1 mt-3 border-t border-slate-100 pt-2">
          {data.map((d) => (
            <span key={d.day} className={`flex-1 text-center text-xs ${
              d.isCurrent
                ? `font-bold ${isBankrupt ? "text-rose-dark" : "text-primary"}`
                : "text-slate-400"
            }`}>
              {d.isCurrent ? `Day ${d.day}` : `D${d.day}`}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}
