interface SeasonCardProps {
  season: string;
  rank: string;
  rankVariant: "gold" | "normal" | "bankrupt";
  location: string;
  storeName: string;
  revenue: string;
  rewardPoints: string;
  usedPoints: string;
  pointDetails: { label: string; value: string }[];
  isBankrupt?: boolean;
}

const rankStyles = {
  gold: "bg-yellow-50 border-yellow-200 text-amber-600",
  normal: "bg-gray-100 border-gray-200 text-gray-500",
  bankrupt: "bg-rose-50 border-rose-200 text-rose-400",
};

export default function SeasonCard({
  season, rank, rankVariant, location, storeName,
  revenue, rewardPoints, usedPoints, pointDetails, isBankrupt = false,
}: SeasonCardProps) {
  return (
    <article className={`
      bg-white rounded-xl shadow-soft p-6 flex flex-col md:flex-row gap-6 relative overflow-hidden
      transition-transform hover:-translate-y-1 duration-300 group
      ${isBankrupt ? "border-l-4 border-accent-rose" : ""}
    `}>
      {/* Season + Rank */}
      <div className="flex md:flex-col items-center justify-between md:justify-center md:w-32 shrink-0 border-b md:border-b-0 md:border-r border-gray-100 pb-4 md:pb-0 md:pr-4">
        <span className="text-lg font-bold text-slate-900">{season}</span>
        <div className={`mt-0 md:mt-2 px-3 py-1 border rounded-full flex items-center gap-1 ${rankStyles[rankVariant]}`}>
          {rankVariant === "gold" && <span className="material-symbols-outlined text-sm">emoji_events</span>}
          <span className="text-xs font-bold">{rank}</span>
        </div>
      </div>

      {/* Info */}
      <div className="flex-grow space-y-3">
        <div>
          <div className="flex items-center gap-2 text-xs text-gray-500 mb-1">
            <span className="material-symbols-outlined text-sm">location_on</span>
            <span>{location}</span>
          </div>
          <h3 className="text-lg font-bold text-slate-900 group-hover:text-primary transition-colors">{storeName}</h3>
        </div>
        <div className="flex flex-wrap gap-4 mt-2">
          <div className={`flex items-center gap-2 bg-primary/5 px-3 py-2 rounded-lg ${isBankrupt ? "opacity-60" : ""}`}>
            <span className="material-symbols-outlined text-primary text-sm">payments</span>
            <div className="flex flex-col">
              <span className="text-[10px] text-gray-500 uppercase tracking-wider">수익</span>
              <span className={`text-sm font-mono font-semibold text-slate-900 ${isBankrupt ? "line-through" : ""}`}>{revenue}</span>
            </div>
          </div>
          <div className={`flex items-center gap-2 bg-primary/5 px-3 py-2 rounded-lg ${isBankrupt ? "opacity-60" : ""}`}>
            <span className={`material-symbols-outlined text-sm ${isBankrupt ? "text-gray-400" : "text-primary"}`}>stars</span>
            <div className="flex flex-col">
              <span className="text-[10px] text-gray-500 uppercase tracking-wider">보상 포인트</span>
              <span className={`text-sm font-mono font-semibold ${isBankrupt ? "text-gray-400 line-through" : "text-primary"}`}>{rewardPoints}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Used Points */}
      <div className="md:w-56 shrink-0 flex flex-col justify-center items-end text-right border-t md:border-t-0 md:border-l border-gray-100 pt-4 md:pt-0 md:pl-4">
        <span className="block mb-1 text-xs text-gray-500">사용 포인트</span>
        <span className="block font-mono font-medium text-slate-900 text-lg mb-2">{usedPoints}</span>
        <div className="space-y-1">
          {pointDetails.map((d, i) => (
            <div key={i} className="flex justify-end items-center gap-2">
              <span className="text-[11px] text-gray-500">{d.label}</span>
              <span className="text-[11px] font-mono font-medium text-gray-700">{d.value}</span>
            </div>
          ))}
        </div>
      </div>
    </article>
  );
}
