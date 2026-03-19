import RankingRow from "./RankingRow";
import type { RankingRowEntry } from "./RankingRow";

interface RankingListProps {
  entries: RankingRowEntry[];
}

export default function RankingList({ entries }: RankingListProps) {
  return (
    <div className="flex flex-col gap-3 w-full">
      {/* Header */}
      <div className="hidden md:flex px-6 pb-2 text-xs font-bold text-slate-400 uppercase tracking-wider">
        <div className="w-16">Rank</div>
        <div className="w-16 mr-4" />
        <div className="flex-1">User Info</div>
        <div className="w-24 text-center">ROI</div>
        <div className="w-[140px] text-right">Revenue</div>
        <div className="w-16 text-right ml-4">Reward</div>
      </div>

      {entries.map((entry, idx) => (
        <RankingRow key={`${entry.rank}-${idx}`} entry={entry} animationDelay={500 + idx * 80} />
      ))}
    </div>
  );
}
