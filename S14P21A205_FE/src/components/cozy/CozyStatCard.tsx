interface CozyStatCardProps {
  label: string;
  value: string | number;
  change?: { value: string; positive: boolean };
  icon?: string;
  highlight?: boolean;
}

export default function CozyStatCard({ label, value, change, icon, highlight = false }: CozyStatCardProps) {
  return (
    <div className={`
      relative p-5 rounded-2xl transition-all font-cozy-display
      ${highlight
        ? "bg-[#fff3e0] border-2 border-amber-300 shadow-cozy-clay"
        : "bg-cozy-cream shadow-cozy-clay hover:-translate-y-0.5"
      }
    `}>
      {/* Paper texture */}
      <div className="absolute inset-0 rounded-2xl opacity-20 pointer-events-none"
        style={{ backgroundImage: "radial-gradient(#d4c5b0 0.5px, transparent 0.5px)", backgroundSize: "8px 8px" }}
      />
      <div className="relative z-10">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm text-cozy-wood-dark font-medium">{label}</span>
          {icon && <span className="material-symbols-outlined text-cozy-sage-green text-xl">{icon}</span>}
        </div>
        <div className="text-3xl font-black text-cozy-ink font-cozy-hand">{value}</div>
        {change && (
          <div className={`text-sm mt-1 font-bold ${change.positive ? "text-cozy-sage-green" : "text-cozy-primary"}`}>
            {change.positive ? "▲" : "▼"} {change.value}
          </div>
        )}
      </div>
    </div>
  );
}
