import Badge from "../common/Badge";
import { formatCurrency } from "./utils";

interface PodiumEntry {
  rank: number;
  nickname: string;
  storeName: string;
  locationName: string;
  menuName: string;
  roi: number;
  totalRevenue: number;
  rewardPoints: number;
  isBankrupt: boolean;
  isMe?: boolean;
}

interface PodiumProps {
  entries: PodiumEntry[];
}

const config: Record<number, { color: string; badgeBg: string; avatarBorder: string; order: string; mt: string; badgeSize: string; delay: string }> = {
  1: { color: "#EBC86E", badgeBg: "bg-[#EBC86E]", avatarBorder: "border-[#EBC86E]/30", order: "md:order-2", mt: "md:-mt-8", badgeSize: "size-12 -top-8 text-xl", delay: "400ms" },
  2: { color: "#C0C0C0", badgeBg: "bg-[#C0C0C0]", avatarBorder: "border-[#C0C0C0]/30", order: "md:order-1", mt: "mt-0", badgeSize: "size-8 -top-6", delay: "200ms" },
  3: { color: "#CD7F32", badgeBg: "bg-[#CD7F32]", avatarBorder: "border-[#CD7F32]/30", order: "md:order-3", mt: "mt-0", badgeSize: "size-8 -top-6", delay: "300ms" },
};

export default function Podium({ entries }: PodiumProps) {
  const top3 = entries.filter((e) => e.rank <= 3);

  return (
    <div className="flex flex-col md:flex-row items-end justify-center gap-4 md:gap-6 mb-8 px-4 mt-12">
      {top3.map((entry) => {
        const c = config[entry.rank];
        const isFirst = entry.rank === 1;

        return (
          <div key={entry.rank} className={`${c.order} ${c.mt} flex-1 max-w-[280px] w-full ${
            entry.isMe ? "bg-primary/5 ring-1 ring-primary" : "bg-white"
          } rounded-2xl shadow-soft relative flex flex-col items-center ${isFirst ? "pt-16 pb-12" : "pt-14 pb-8"} animate-fade-up`}
            style={{ borderTop: `4px solid ${c.color}`, animationDelay: c.delay }}>

            {/* Badge */}
            <div className={`absolute left-1/2 -translate-x-1/2 ${c.badgeSize} ${c.badgeBg} text-white rounded-full flex items-center justify-center font-bold shadow-md z-10`}>
              {isFirst
                ? <span className="material-symbols-outlined text-[1.4rem]">emoji_events</span>
                : entry.rank}
            </div>

            {/* Avatar */}
            <div className={`${isFirst ? "size-24 border-4" : "size-20 border-2"} rounded-full bg-slate-100 overflow-hidden mb-3 ${c.avatarBorder} flex items-center justify-center text-3xl text-slate-400`}>
              <span className="material-symbols-outlined text-4xl">person</span>
            </div>

            {/* Name */}
            <h3 className={`font-bold ${isFirst ? "text-xl" : "text-lg"} text-slate-900 mb-0.5 flex items-center gap-1`}>
              {entry.nickname}
              {entry.isMe && <Badge variant="green" size="sm">ME</Badge>}
            </h3>
            <p className="text-sm text-slate-500 mb-1 truncate max-w-[90%]">{entry.storeName}</p>
            <p className="text-xs text-slate-400 mb-3 flex items-center gap-0.5">
              <span className="material-symbols-outlined text-sm">location_on</span>
              {entry.locationName} · {entry.menuName}
            </p>

            {/* Stats */}
            <div className="flex flex-col items-center gap-1">
              <span className={`text-xs font-bold px-2.5 py-0.5 rounded-md ${
                isFirst ? "bg-yellow-50 text-yellow-700 border border-yellow-100" : "bg-slate-100 text-slate-600"
              }`}>
                ROI {entry.roi.toFixed(1)}%
              </span>
              <p className={`text-primary font-mono font-bold ${isFirst ? "text-xl" : "text-base"} mt-1`}>
                {formatCurrency(entry.totalRevenue)}
              </p>
              <span className="text-xs text-primary-dark font-bold mt-0.5">{entry.rewardPoints}P</span>
            </div>
          </div>
        );
      })}
    </div>
  );
}
