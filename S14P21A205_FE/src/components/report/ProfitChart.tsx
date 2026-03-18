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
  const maxAbs = Math.max(...data.filter((d) => !d.isFuture).map((d) => Math.abs(d.value)), 1);

  return (
    <div className={`lg:col-span-2 bg-white rounded-xl p-6 shadow-soft flex flex-col ${isBankrupt ? "border border-rose-soft" : "border border-slate-100"}`}>
      <div className="flex items-center justify-between mb-4">
        <h3 className={`text-lg font-bold flex items-center gap-2 ${isBankrupt ? "text-rose-dark" : "text-slate-800"}`}>
          <span className={`material-symbols-outlined ${isBankrupt ? "text-rose-soft" : "text-primary"}`}>show_chart</span>
          수익 그래프
        </h3>
      </div>

      <div className="flex-1 flex items-end justify-between gap-2 h-32 px-2 pb-2">
        {data.map((d) => {
          const height = d.isFuture ? 10 : Math.max((Math.abs(d.value) / maxAbs) * 100, 5);
          const isNegative = d.value < 0;

          return (
            <div key={d.day} className="w-full relative group" style={{ height: `${height}%` }}>
              <div className={`w-full h-full rounded-t-sm ${
                d.isFuture ? "bg-slate-50 border border-dashed border-slate-300 opacity-50"
                : d.isCurrent
                  ? isNegative ? "bg-rose-soft shadow-lg shadow-rose-soft/30" : "bg-primary/80 shadow-lg shadow-primary/20"
                  : isNegative ? "bg-rose-soft/40" : "bg-slate-100"
              }`} />
              {!d.isFuture && (
                <div className={`absolute -top-6 left-1/2 -translate-x-1/2 text-xs font-medium whitespace-nowrap ${
                  d.isCurrent ? "opacity-100 font-bold" : "opacity-0 group-hover:opacity-100"
                } transition-opacity ${isNegative ? "text-rose-dark" : d.isCurrent ? "text-primary" : "text-slate-500"}`}>
                  {d.value >= 0 ? `₩${(d.value / 10000).toFixed(0)}만` : `-₩${(Math.abs(d.value) / 10000).toFixed(0)}만`}
                </div>
              )}
            </div>
          );
        })}
      </div>

      <div className="flex justify-between text-xs text-slate-400 mt-2 px-1">
        {data.map((d) => (
          <span key={d.day} className={d.isCurrent ? `font-bold ${isBankrupt ? "text-rose-dark" : "text-primary"}` : ""}>
            {d.isCurrent ? `Day ${d.day}` : `D${d.day}`}
          </span>
        ))}
      </div>
    </div>
  );
}
