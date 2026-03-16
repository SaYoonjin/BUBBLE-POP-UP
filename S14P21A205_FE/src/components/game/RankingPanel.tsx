interface RankItem {
  rank: number;
  name: string;
  change: string;
  positive: boolean;
  barWidth: string;
}

interface RankingPanelProps {
  title: string;
  icon: string;
  items: RankItem[];
}

export default function RankingPanel({ title, icon, items }: RankingPanelProps) {
  return (
    <div className="bg-white rounded-2xl p-7 shadow-premium border border-white/50 backdrop-blur-sm">
      <div className="flex items-center justify-between mb-8">
        <h3 className="text-lg font-bold text-slate-800 flex items-center gap-3 tracking-tight">
          <span className="p-2 bg-slate-50 rounded-lg border border-slate-100">
            <span className="material-symbols-outlined text-primary text-xl">{icon}</span>
          </span>
          {title}
        </h3>
      </div>

      <div className="flex flex-col gap-5">
        {items.map((item) => (
          <div key={item.rank} className="group relative">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-4">
                <span className={`font-display w-5 ${
                  item.rank === 1 ? "font-black text-xl text-slate-900" : "font-bold text-lg text-slate-400"
                }`}>
                  {item.rank}
                </span>
                <span className={`font-semibold text-base ${
                  item.rank === 1 ? "text-slate-800" : "text-slate-700"
                }`}>
                  {item.name}
                </span>
              </div>
              {item.change !== "-" ? (
                <div className={`flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full ${
                  item.positive ? "text-primary bg-primary/10" : "text-accent-rose bg-accent-rose/10"
                }`}>
                  <span className="material-symbols-outlined text-[14px]">
                    {item.positive ? "trending_up" : "trending_down"}
                  </span>
                  <span>{item.change}</span>
                </div>
              ) : (
                <div className="flex items-center gap-1 text-slate-400 text-xs font-medium">
                  <span className="material-symbols-outlined text-[14px]">remove</span>
                  <span>-</span>
                </div>
              )}
            </div>
            <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full ${item.rank === 1 ? "bg-primary" : "bg-slate-300"}`}
                style={{ width: item.barWidth }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
