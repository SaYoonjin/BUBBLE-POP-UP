import Badge from "../common/Badge";
import Confetti from "./Confetti";
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

const config: Record<number, {
  color: string;
  badgeBg: string;
  avatarBorder: string;
  order: string;
  minH: number;
  badgeSize: string;
  delay: string;
  shimmerDelay: string;
}> = {
  1: {
    color: "#EBC86E",
    badgeBg: "bg-[#EBC86E]",
    avatarBorder: "border-[#EBC86E]/30",
    order: "md:order-2",
    minH: 400,
    badgeSize: "size-12 text-xl",
    delay: "400ms",
    shimmerDelay: "0s",
  },
  2: {
    color: "#C0C0C0",
    badgeBg: "bg-[#C0C0C0]",
    avatarBorder: "border-[#C0C0C0]/30",
    order: "md:order-1",
    minH: 360,
    badgeSize: "size-8",
    delay: "200ms",
    shimmerDelay: "1s",
  },
  3: {
    color: "#CD7F32",
    badgeBg: "bg-[#CD7F32]",
    avatarBorder: "border-[#CD7F32]/30",
    order: "md:order-3",
    minH: 320,
    badgeSize: "size-8",
    delay: "300ms",
    shimmerDelay: "2s",
  },
};

export default function Podium({ entries }: PodiumProps) {
  const top3 = entries.filter((e) => e.rank <= 3);

  return (
    <div className="flex flex-col md:flex-row items-end justify-center gap-6 md:gap-6 mb-8 px-4 mt-16 relative">
      <Confetti />

      <style>{`
        @keyframes cardShimmer {
          0% { transform: translateX(-100%) rotate(25deg); }
          40%, 100% { transform: translateX(250%) rotate(25deg); }
        }
      `}</style>

      {top3.map((entry) => {
        const c = config[entry.rank];
        const isFirst = entry.rank === 1;

        return (
          <div key={entry.rank} className={`${c.order} flex-1 max-w-[280px] w-full relative z-20`}>
            {/* Badge */}
            <div
              className="flex justify-center mb-[-1rem] relative z-30"
              style={{ animation: `fadeUp 0.5s ease-out ${c.delay} both` }}
            >
              <div className={`${c.badgeSize} ${c.badgeBg} text-white rounded-full flex items-center justify-center font-bold shadow-md`}>
                {isFirst
                  ? <span className="material-symbols-outlined text-[1.4rem]">emoji_events</span>
                  : entry.rank}
              </div>
            </div>

            {/* Card */}
            <div
              className="bg-primary/5 rounded-2xl shadow-soft relative flex flex-col items-center justify-end pt-8 pb-8 overflow-hidden"
              style={{
                border: `2px solid ${c.color}`,
                animation: `fadeUp 0.5s ease-out ${c.delay} both`,
                minHeight: c.minH,
              }}
            >
              {/* Shimmer sweep */}
              <div
                className="absolute inset-0 pointer-events-none"
                style={{ overflow: "hidden", borderRadius: "inherit" }}
              >
                <div
                  style={{
                    position: "absolute",
                    top: "-50%",
                    width: "45%",
                    height: "200%",
                    background: "linear-gradient(90deg, transparent 0%, transparent 25%, rgba(255,255,255,0.7) 40%, rgba(255,255,255,0.9) 50%, rgba(255,255,255,0.7) 60%, transparent 75%, transparent 100%)",
                    animation: `cardShimmer 3s ease-in-out ${c.shimmerDelay} infinite`,
                  }}
                />
              </div>

              {/* Avatar */}
              <div className={`${isFirst ? "size-24 border-4" : "size-20 border-2"} rounded-full bg-slate-100 mb-3 ${c.avatarBorder} flex items-center justify-center text-3xl text-slate-400 relative z-10`}>
                <span className="material-symbols-outlined text-4xl">person</span>
              </div>

              {/* Name */}
              <h3 className={`font-bold ${isFirst ? "text-xl" : "text-lg"} text-slate-900 mb-0.5 flex items-center gap-1 relative z-10`}>
                {entry.nickname}
                {entry.isMe && <Badge variant="green" size="sm">ME</Badge>}
              </h3>
              <p className="text-sm text-slate-500 mb-1 truncate max-w-[90%] relative z-10">{entry.storeName}</p>
              <p className="text-xs text-slate-400 mb-3 flex items-center gap-0.5 relative z-10">
                <span className="material-symbols-outlined text-sm">location_on</span>
                {entry.locationName} · {entry.menuName}
              </p>

              {/* Stats */}
              <div className="flex flex-col items-center gap-1 relative z-10">
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
          </div>
        );
      })}
    </div>
  );
}
