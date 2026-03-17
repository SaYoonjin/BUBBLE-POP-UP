interface PlayHeaderProps {
  location: string;
  storeName: string;
  menuName: string;
  day: number;
  timeLeft: string;
  population: number;
  guests: number;
  stock: number;
  balance: number;
}

export default function PlayHeader({
  location,
  storeName,
  menuName,
  day,
  timeLeft,
  population,
  guests,
  stock,
  balance,
}: PlayHeaderProps) {
  return (
    <header className="h-20 px-6 flex items-center justify-between glass-panel sticky top-0 z-50 shadow-sm border-b border-white/40">
      {/* Left: Store info */}
      <div className="flex items-center gap-4 text-sm font-medium text-slate-600">
        <div className="flex items-center gap-2">
          <span className="material-symbols-outlined text-primary text-[20px]">location_on</span>
          <span className="font-bold text-slate-800">{location}</span>
        </div>
        <div className="w-px h-4 bg-slate-300" />
        <div className="flex items-center gap-2">
          <span className="material-symbols-outlined text-primary text-[20px]">storefront</span>
          <span className="font-bold text-slate-800">{storeName}</span>
        </div>
        <div className="w-px h-4 bg-slate-300" />
        <div className="flex items-center gap-2">
          <span className="material-symbols-outlined text-primary text-[20px]">restaurant_menu</span>
          <span className="font-bold text-slate-800">{menuName}</span>
        </div>
      </div>

      {/* Center: Day + Timer */}
      <div className="absolute left-1/2 -translate-x-1/2 flex items-center gap-3">
        <div className="flex items-center gap-2 bg-primary/20 px-5 py-2 rounded-full border border-primary/30 shadow-sm">
          <span className="font-extrabold tracking-wider text-primary-dark">DAY {day}</span>
        </div>
        <div className="flex items-center gap-2 bg-white px-5 py-2 rounded-full shadow-sm border border-slate-100">
          <span className="material-symbols-outlined text-primary text-[20px]">timer</span>
          <span className="font-mono font-bold text-lg text-slate-800">{timeLeft}</span>
        </div>
      </div>

      {/* Right: Stats */}
      <div className="flex items-center gap-6 bg-white/60 px-6 py-2 rounded-xl backdrop-blur-sm shadow-sm border border-white/50">
        <StatItem label="유동인구" icon="👥" value={population.toLocaleString()} />
        <div className="w-px h-8 bg-slate-200" />
        <StatItem label="손님" icon="👋" value={String(guests)} />
        <div className="w-px h-8 bg-slate-200" />
        <StatItem label="재고" icon="📦" value={String(stock)} />
        <div className="w-px h-8 bg-slate-200" />
        <StatItem label="잔액" icon="💰" value={balance.toLocaleString()} />
      </div>
    </header>
  );
}

function StatItem({ label, icon, value }: { label: string; icon: string; value: string }) {
  return (
    <div className="flex flex-col items-center min-w-[3rem]">
      <span className="text-xs text-slate-500 font-medium">{label}</span>
      <div className="flex items-center gap-1">
        <span className="text-sm">{icon}</span>
        <span className="font-bold text-slate-800">{value}</span>
      </div>
    </div>
  );
}
