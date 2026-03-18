interface PodiumEntry {
  rank: number;
  name: string;
  storeName: string;
  roi: string;
  revenue: string;
  isMe?: boolean;
}

interface PodiumProps {
  entries: PodiumEntry[];
}

const medalConfig: Record<number, { bg: string; border: string; size: string; icon?: string }> = {
  1: { bg: "bg-[#EBC86E]", border: "border-[#EBC86E]/20", size: "size-12", icon: "emoji_events" },
  2: { bg: "bg-[#C0C0C0]", border: "border-[#C0C0C0]/20", size: "size-8" },
  3: { bg: "bg-[#CD7F32]", border: "border-[#CD7F32]/20", size: "size-8" },
};

const podiumHeight: Record<number, string> = { 1: "pt-14 pb-12", 2: "pt-12 pb-8", 3: "pt-12 pb-8" };
const podiumOrder: Record<number, string> = { 1: "order-2", 2: "order-1", 3: "order-3" };

export default function Podium({ entries }: PodiumProps) {
  const top3 = entries.filter((e) => e.rank <= 3);

  return (
    <div className="flex flex-col md:flex-row items-end justify-center gap-4 md:gap-6 mb-8 px-4 min-h-[380px]">
      {top3.map((entry) => {
        const medal = medalConfig[entry.rank];
        const isFirst = entry.rank === 1;

        return (
          <div key={entry.rank} className={`${podiumOrder[entry.rank]} flex-1 max-w-[280px] w-full ${
            entry.isMe ? "bg-primary/5 ring-1 ring-primary" : "bg-white"
          } rounded-t-2xl rounded-b-lg shadow-soft relative flex flex-col items-center ${podiumHeight[entry.rank]} border-t-4`}
            style={{ borderTopColor: medal.bg.replace("bg-[", "").replace("]", "") }}>

            {/* Medal badge */}
            <div className={`absolute -top-${isFirst ? "8" : "6"} left-1/2 -translate-x-1/2 ${medal.size} ${medal.bg} text-white rounded-full flex items-center justify-center font-bold shadow-md z-10 ${isFirst ? "text-xl" : ""}`}>
              {medal.icon
                ? <span className="material-symbols-outlined text-[1.5rem]">{medal.icon}</span>
                : entry.rank}
            </div>

            {/* Avatar placeholder */}
            <div className={`${isFirst ? "size-24 border-4" : "size-20 border-2"} rounded-full bg-slate-100 overflow-hidden mb-3 ${medal.border} flex items-center justify-center text-3xl`}>
              👤
            </div>

            {/* Name */}
            <h3 className={`font-bold ${isFirst ? "text-xl" : "text-lg"} text-slate-900 mb-0.5 flex items-center gap-1`}>
              {entry.name}
              {entry.isMe && <span className="text-[10px] bg-primary text-white px-1.5 py-0.5 rounded-full font-bold">ME</span>}
            </h3>
            <p className="text-sm text-slate-500 mb-2 truncate max-w-[90%]">{entry.storeName}</p>

            {/* Stats */}
            <div className="flex flex-col items-center gap-0.5">
              <span className={`text-xs font-bold px-2 py-0.5 rounded-md ${
                isFirst ? "bg-yellow-50 text-yellow-700 border border-yellow-100" : "bg-slate-100 text-slate-600"
              }`}>
                ROI {entry.roi}
              </span>
              <p className={`text-primary font-mono font-bold ${isFirst ? "text-xl" : ""} mt-1`}>{entry.revenue}</p>
            </div>
          </div>
        );
      })}
    </div>
  );
}
