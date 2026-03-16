interface CozySeasonRecordProps {
  season: string;
  rank: string;
  rankVariant: "gold" | "normal" | "bankrupt";
  location: string;
  storeName: string;
  revenue: string;
  rewardPoints: string;
  isBankrupt?: boolean;
}

export default function CozySeasonRecord({
  season, rank, rankVariant, location, storeName, revenue, rewardPoints, isBankrupt = false,
}: CozySeasonRecordProps) {
  return (
    <div
      className={`
        relative bg-cozy-paper p-6 rounded-sm transition-all hover:-translate-y-1 duration-300
        shadow-[0_6px_20px_-4px_rgba(0,0,0,0.15),0_3px_8px_-2px_rgba(0,0,0,0.1)]
        hover:shadow-[0_12px_30px_-6px_rgba(0,0,0,0.2),0_6px_12px_-3px_rgba(0,0,0,0.12)]
        ${isBankrupt ? "border-l-4 border-cozy-primary" : ""}
        ${Math.random() > 0.5 ? "rotate-[0.5deg]" : "-rotate-[0.5deg]"}
      `}
      style={{
        backgroundImage: "url('https://www.transparenttextures.com/patterns/cream-paper.png')",
        backgroundBlendMode: "multiply",
      }}
    >
      <div className="flex items-start gap-6">
        {/* Rank stamp */}
        <div className={`
          shrink-0 w-16 h-16 rounded-sm flex flex-col items-center justify-center -rotate-[5deg] shadow-md border-2
          ${rankVariant === "gold" ? "bg-amber-50 border-amber-300 text-amber-700" :
            rankVariant === "bankrupt" ? "bg-red-50 border-red-200 text-red-400" :
            "bg-gray-50 border-gray-200 text-gray-500"}
        `}>
          {rankVariant === "gold" && <span className="material-symbols-outlined text-lg">emoji_events</span>}
          <span className="font-cozy-hand text-lg font-bold">{rank}</span>
        </div>

        <div className="flex-1">
          <div className="flex items-baseline gap-2 mb-1">
            <span className="font-cozy-serif font-bold text-lg text-cozy-ink">{season}</span>
            <span className="font-cozy-hand text-sm text-cozy-ink/40">{location}</span>
          </div>
          <h3 className="font-cozy-serif italic text-xl text-cozy-ink mb-3">{storeName}</h3>
          <div className="flex gap-6">
            <div>
              <span className="font-cozy-hand text-xs text-cozy-ink/40 block">Revenue</span>
              <span className={`font-cozy-hand text-lg ${isBankrupt ? "text-cozy-ink/30 line-through" : "text-cozy-ink"}`}>{revenue}</span>
            </div>
            <div>
              <span className="font-cozy-hand text-xs text-cozy-ink/40 block">Points</span>
              <span className={`font-cozy-hand text-lg ${isBankrupt ? "text-cozy-ink/30 line-through" : "text-cozy-sage"}`}>{rewardPoints}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
