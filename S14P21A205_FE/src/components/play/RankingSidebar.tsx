import { useState } from "react";

export interface RankEntry {
  rank: number;
  name: string;
  storeName: string;
  revenue: string;
  isMe?: boolean;
}

interface RankingSidebarProps {
  rankings: RankEntry[];
}

const medalMap: Record<number, string> = { 1: "🥇", 2: "🥈", 3: "🥉" };

export default function RankingSidebar({ rankings }: RankingSidebarProps) {
  const [expanded, setExpanded] = useState(true);

  return (
    <aside className="absolute top-20 left-4 z-10 w-64 pointer-events-none">
      <div className="glass-panel rounded-2xl shadow-lg pointer-events-auto overflow-hidden">
        {/* Header */}
        <button
          onClick={() => setExpanded(!expanded)}
          className="w-full px-4 py-3 flex items-center justify-between hover:bg-white/40 transition-colors"
        >
          <h2 className="font-bold text-slate-800 text-sm flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[18px]">leaderboard</span>
            실시간 랭킹
          </h2>
          <span className={`material-symbols-outlined text-slate-400 text-[18px] transition-transform duration-200 ${expanded ? "rotate-180" : ""}`}>
            expand_more
          </span>
        </button>

        {/* Ranking list */}
        {expanded && (
          <div className="px-3 pb-3 space-y-1">
            {rankings.map((entry) => (
              <div
                key={entry.rank}
                className={`flex items-center gap-2.5 px-3 py-2 rounded-lg transition-colors ${
                  entry.isMe ? "bg-primary/8" : "hover:bg-white/50"
                }`}
              >
                {/* Rank */}
                <div className="w-6 text-center shrink-0">
                  {medalMap[entry.rank]
                    ? <span className="text-sm">{medalMap[entry.rank]}</span>
                    : <span className="text-[12px] font-bold text-slate-400">{entry.rank}</span>
                  }
                </div>

                {/* Info */}
                <div className="flex-1 min-w-0">
                  <p className={`text-[12px] font-bold truncate ${entry.isMe ? "text-primary-dark" : "text-slate-700"}`}>
                    {entry.name}
                    {entry.isMe && <span className="text-[9px] text-primary ml-1">YOU</span>}
                  </p>
                  <p className="text-[10px] text-slate-400 truncate">{entry.storeName}</p>
                </div>

                {/* Revenue */}
                <span className="text-[12px] font-bold text-slate-600 font-countdown shrink-0">{entry.revenue}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </aside>
  );
}
