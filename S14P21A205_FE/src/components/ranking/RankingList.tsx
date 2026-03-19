interface RankEntry {
  rank: number;
  name: string;
  storeName: string;
  location: string;
  roi: string;
  revenue: string;
  reward: string;
  isMe?: boolean;
}

interface RankingListProps {
  entries: RankEntry[];
}

export default function RankingList({ entries }: RankingListProps) {
  return (
    <div className="flex flex-col gap-3 w-full pb-12">
      {/* Header */}
      <div className="flex px-6 pb-2 text-xs font-bold text-slate-400 uppercase tracking-wider hidden md:flex">
        <div className="w-16">Rank</div>
        <div className="w-16 mr-4" />
        <div className="flex-1">User Info</div>
        <div className="w-24 text-center">ROI</div>
        <div className="w-[140px] text-right">Revenue</div>
        <div className="w-16 text-right ml-4">Reward</div>
      </div>

      {entries.map((entry) => (
        <div key={entry.rank} className={`flex items-center rounded-2xl p-4 shadow-soft border transition-shadow hover:shadow-md group relative overflow-hidden ${
          entry.isMe ? "bg-primary/5 border-primary" : "bg-white border-slate-50"
        }`}>
          {entry.isMe && <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-primary" />}

          {/* Rank */}
          <div className="w-12 flex justify-center mr-4 ml-2">
            <span className={`text-xs font-bold px-2.5 py-1 rounded-full ${
              entry.isMe ? "bg-primary text-white" : "bg-slate-100 text-slate-500 group-hover:bg-primary group-hover:text-white transition-colors"
            }`}>
              {entry.rank}
            </span>
          </div>

          {/* Avatar */}
          <div className={`size-12 rounded-full bg-slate-100 overflow-hidden mr-4 flex-shrink-0 flex items-center justify-center text-xl ${
            entry.isMe ? "border-2 border-white shadow-sm ring-2 ring-primary/30" : ""
          }`}>
            👤
          </div>

          {/* Info */}
          <div className="flex flex-col md:flex-row md:items-center flex-1 gap-1 md:gap-4 overflow-hidden">
            <div className="flex flex-col min-w-[140px]">
              <span className="font-bold text-slate-900 text-lg truncate">{entry.name}</span>
              {entry.isMe && <span className="text-xs text-primary font-bold">나 (Player)</span>}
            </div>
            <div className="flex flex-col text-sm text-slate-500">
              <span className="flex items-center gap-1 font-medium text-slate-700 truncate">
                <span className="material-symbols-outlined text-base">store</span>
                {entry.storeName}
              </span>
              <span className="flex items-center gap-1 text-xs truncate">
                <span className="material-symbols-outlined text-sm">location_on</span>
                {entry.location}
              </span>
            </div>
          </div>

          {/* Stats */}
          <div className="flex items-center gap-2 md:gap-6 ml-2">
            <div className="hidden sm:flex flex-col items-end justify-center w-24">
              <span className="text-xs text-slate-400 font-medium mb-0.5">ROI</span>
              <span className="text-slate-700 font-bold">{entry.roi}</span>
            </div>
            <div className="flex flex-col items-end w-[140px]">
              <span className="font-mono font-bold text-primary text-lg">{entry.revenue}</span>
            </div>
            <div className="w-16 flex justify-end">
              <span className="bg-primary/20 text-primary-dark text-xs font-bold px-2.5 py-1 rounded-full">{entry.reward}</span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
