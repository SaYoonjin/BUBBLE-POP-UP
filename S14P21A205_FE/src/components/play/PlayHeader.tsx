import CountdownTimer from "../common/CountdownTimer";

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
  location, storeName, menuName, day, remainingSeconds, gameTime, congestion, guests, stock, balance,
}: PlayHeaderProps) {
  const cong = congestionMap[congestion];

  return (
    <header className="h-16 px-5 flex items-center justify-between glass-panel sticky top-0 z-50 shadow-sm border-b border-white/40">
      {/* Left: Store info */}
      <div className="flex items-center gap-3 text-sm font-medium text-slate-600">
        <div className="flex items-center gap-1.5">
          <span className="material-symbols-outlined text-primary text-[18px]">location_on</span>
          <span className="font-bold text-slate-800">{location}</span>
        </div>
        <div className="w-px h-3.5 bg-slate-300" />
        <div className="flex items-center gap-1.5">
          <span className="material-symbols-outlined text-primary text-[18px]">storefront</span>
          <span className="font-bold text-slate-800">{storeName}</span>
        </div>
        <div className="w-px h-3.5 bg-slate-300" />
        <div className="flex items-center gap-1.5">
          <span className="material-symbols-outlined text-primary text-[18px]">restaurant_menu</span>
          <span className="font-bold text-slate-800">{menuName}</span>
        </div>
      </div>

      {/* Center: Day + Timer + Game Clock */}
      <div className="absolute left-1/2 -translate-x-1/2 flex items-center gap-2.5">
        <div className="bg-primary/15 px-3.5 py-1.5 rounded-full border border-primary/20">
          <span className="font-extrabold tracking-wider text-primary-dark text-sm">DAY {day}</span>
        </div>
        <CountdownTimer initialSeconds={remainingSeconds} label="영업 시간" />
        <div className="flex items-center gap-1.5 bg-white px-3 py-1.5 rounded-full shadow-sm border border-slate-100">
          <span className="material-symbols-outlined text-slate-400 text-[16px]">schedule</span>
          <span className="font-countdown font-bold text-sm text-slate-700 tabular-nums">{gameTime}</span>
        </div>
      </div>

      {/* Right: Stats */}
      <div className="flex items-center gap-4 bg-white/60 px-4 py-1.5 rounded-xl backdrop-blur-sm shadow-sm border border-white/50">
        <StatItem label="유동인구" icon="groups" value={cong.label} valueColor={cong.color} />
        <div className="w-px h-7 bg-slate-200" />
        <StatItem label="손님" icon="person" value={String(guests)} />
        <div className="w-px h-7 bg-slate-200" />
        <StatItem label="재고" icon="inventory_2" value={String(stock)} />
        <div className="w-px h-7 bg-slate-200" />
        <StatItem label="잔액" icon="account_balance_wallet" value={balance.toLocaleString()} />
      </div>
    </header>
  );
}

function StatItem({ label, icon, value, valueColor }: { label: string; icon: string; value: string; valueColor?: string }) {
  return (
    <div className="flex flex-col items-center min-w-[3rem]">
      <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wide">{label}</span>
      <div className="flex items-center gap-1">
        <span className="material-symbols-outlined text-slate-400 text-[14px]">{icon}</span>
        <span className={`font-bold text-sm ${valueColor || "text-slate-800"}`}>{value}</span>
      </div>
    </div>
  );
}
