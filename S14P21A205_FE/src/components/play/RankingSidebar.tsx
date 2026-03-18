import { useMemo, useState } from "react";
import { formatMoneyUnit } from "../../utils/formatMoneyUnit";

export interface RankEntry {
  id: string;
  name: string;
  storeName: string;
  revenue: number;
  roi: number;
  isMe?: boolean;
}

interface RankingSidebarProps {
  rankings: RankEntry[];
}

const medalMap: Record<number, string> = {
  1: "🥇",
  2: "🥈",
  3: "🥉",
};

export default function RankingSidebar({ rankings }: RankingSidebarProps) {
  const [expanded, setExpanded] = useState(true);
  const [showTooltip, setShowTooltip] = useState(false);

  const sortedRankings = useMemo(
    () => [...rankings].sort((left, right) => right.roi - left.roi),
    [rankings],
  );

  return (
    <aside className="absolute top-20 left-4 z-10 w-72 pointer-events-none">
      <div className="glass-panel rounded-2xl shadow-lg pointer-events-auto overflow-visible">
        <div className="px-4 py-3 flex items-start justify-between gap-3">
          <div className="min-w-0">
            <div className="flex items-center gap-1.5">
              <h2 className="font-bold text-slate-800 text-sm flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[18px]">leaderboard</span>
                실시간 랭킹
              </h2>
              <div className="relative">
                <button
                  type="button"
                  onClick={() => setShowTooltip((prev) => !prev)}
                  className="size-5 rounded-full border border-slate-200 bg-white/80 text-slate-400 hover:border-primary/20 hover:bg-primary/5 hover:text-primary-dark transition-colors flex items-center justify-center"
                  aria-label="ROI 기준 안내 보기"
                >
                  <span className="material-symbols-outlined text-[12px]">info</span>
                </button>
                {showTooltip && (
                  <div className="absolute left-0 top-8 z-20 w-56 rounded-2xl border border-slate-200 bg-white px-3.5 py-3 text-[12px] leading-relaxed text-slate-600 shadow-lg">
                    실제 순위는 ROI 기준으로 산정됩니다.
                    <br />
                    ROI는 투자 대비 수익률로, 수익을 투자금으로 나눈 값입니다.
                  </div>
                )}
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setExpanded((prev) => !prev)}
            className="shrink-0 text-slate-400 hover:text-slate-600 transition-colors"
            aria-label={expanded ? "실시간 랭킹 접기" : "실시간 랭킹 펼치기"}
          >
            <span className={`material-symbols-outlined text-[18px] transition-transform duration-200 ${expanded ? "rotate-180" : ""}`}>
              expand_more
            </span>
          </button>
        </div>

        {expanded && (
          <div className="px-3 pb-3 space-y-1">
            {sortedRankings.map((entry, index) => {
              const displayRank = index + 1;

              return (
                <div
                  key={entry.id}
                  className={`flex items-center gap-2.5 px-3 py-2.5 rounded-lg transition-colors ${
                    entry.isMe ? "bg-primary/8 ring-1 ring-primary/10" : "hover:bg-white/50"
                  }`}
                >
                  <div className="w-7 text-center shrink-0">
                    {medalMap[displayRank] ? (
                      <span className="text-base">{medalMap[displayRank]}</span>
                    ) : (
                      <span className="text-[12px] font-bold text-slate-400">{displayRank}</span>
                    )}
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1">
                      <p className={`text-[12px] font-bold truncate ${entry.isMe ? "text-primary-dark" : "text-slate-700"}`}>
                        {entry.name}
                      </p>
                      {entry.isMe && (
                        <span className="rounded-full bg-primary/10 px-1.5 py-0.5 text-[9px] font-bold text-primary">
                          YOU
                        </span>
                      )}
                    </div>
                    <p className="text-[10px] text-slate-400 truncate">{entry.storeName}</p>
                  </div>

                  <div className="shrink-0 text-right">
                    <p className="text-[9px] font-bold tracking-[0.08em] text-slate-400">수익</p>
                    <span className="text-[12px] font-bold text-slate-700 font-countdown">
                      {formatMoneyUnit(entry.revenue)}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </aside>
  );
}
