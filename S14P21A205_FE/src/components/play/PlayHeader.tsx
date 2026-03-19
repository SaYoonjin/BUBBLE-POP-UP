import CountdownTimer from "../common/CountdownTimer";
import { formatMoneyUnit } from "../../utils/formatMoneyUnit";

type CongestionLevel = "very_crowded" | "crowded" | "normal" | "relaxed" | "very_relaxed";

interface PlayHeaderProps {
  location: string;
  storeName: string;
  menuName: string;
  day: number;
  remainingSeconds: number;
  gameTime: string;
  congestion: CongestionLevel;
  guests: number;
  stock: number;
  balance: number;
}

const congestionMap: Record<CongestionLevel, { label: string; color: string }> = {
  very_crowded: { label: "매우 혼잡", color: "text-red-500" },
  crowded: { label: "혼잡", color: "text-amber-500" },
  normal: { label: "보통", color: "text-slate-600" },
  relaxed: { label: "여유", color: "text-blue-500" },
  very_relaxed: { label: "매우 여유", color: "text-blue-400" },
};

export default function PlayHeader({
  location,
  storeName,
  menuName,
  day,
  remainingSeconds,
  gameTime,
  congestion,
  guests,
  stock,
  balance,
}: PlayHeaderProps) {
  const congestionInfo = congestionMap[congestion];
  const formattedBalance = formatMoneyUnit(balance);

  return (
    <header className="sticky top-0 z-50 flex h-16 items-center justify-between border-b border-white/40 px-5 shadow-sm glass-panel">
      <div className="flex items-center gap-3 text-sm font-medium text-slate-600">
        <div className="flex items-center gap-1.5">
          <span className="material-symbols-outlined text-[18px] text-primary">location_on</span>
          <span className="font-bold text-slate-800">{location}</span>
        </div>
        <div className="h-3.5 w-px bg-slate-300" />
        <div className="flex items-center gap-1.5">
          <span className="material-symbols-outlined text-[18px] text-primary">storefront</span>
          <span className="font-bold text-slate-800">{storeName}</span>
        </div>
        <div className="h-3.5 w-px bg-slate-300" />
        <div className="flex items-center gap-1.5">
          <span className="material-symbols-outlined text-[18px] text-primary">restaurant_menu</span>
          <span className="font-bold text-slate-800">{menuName}</span>
        </div>
      </div>

      <div className="absolute left-1/2 flex -translate-x-1/2 items-center gap-2.5">
        <div className="rounded-full border border-primary/20 bg-primary/15 px-3.5 py-1.5">
          <span className="text-sm font-extrabold tracking-wider text-primary-dark">DAY {day}</span>
        </div>
        <CountdownTimer initialSeconds={remainingSeconds} label="영업 시간" />
        <div className="flex items-center gap-1.5 rounded-full border border-slate-100 bg-white px-3 py-1.5 shadow-sm">
          <span className="material-symbols-outlined text-[16px] text-slate-400">schedule</span>
          <span className="font-countdown text-sm font-bold tabular-nums text-slate-700">{gameTime}</span>
        </div>
      </div>

      <div className="flex items-center gap-4 rounded-xl border border-white/50 bg-white/60 px-4 py-1.5 shadow-sm backdrop-blur-sm">
        <StatItem label="유동인구" icon="groups" value={congestionInfo.label} valueColor={congestionInfo.color} />
        <div className="h-7 w-px bg-slate-200" />
        <StatItem label="손님" icon="person" value={String(guests)} />
        <div className="h-7 w-px bg-slate-200" />
        <StatItem label="재고" icon="inventory_2" value={String(stock)} />
        <div className="h-7 w-px bg-slate-200" />
        <StatItem label="잔액" icon="account_balance_wallet" value={formattedBalance} />
      </div>
    </header>
  );
}

function StatItem({
  label,
  icon,
  value,
  valueColor,
}: {
  label: string;
  icon: string;
  value: string;
  valueColor?: string;
}) {
  return (
    <div className="flex min-w-[3rem] flex-col items-center">
      <span className="text-[10px] font-bold uppercase tracking-wide text-slate-400">{label}</span>
      <div className="flex items-center gap-1">
        <span className="material-symbols-outlined text-[14px] text-slate-400">{icon}</span>
        <span className={`text-sm font-bold ${valueColor || "text-slate-800"}`}>{value}</span>
      </div>
    </div>
  );
}
