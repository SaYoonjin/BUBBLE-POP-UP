interface StatCardProps {
  label: string;
  value: string | number;
  change?: {
    value: string;
    positive: boolean;
  };
  subtext?: string;
  icon?: string;
  iconBg?: string;
  iconColor?: string;
  highlight?: boolean;
  highlightColor?: string;
}

export default function StatCard({
  label, value, change, subtext, icon, iconBg, iconColor, highlight = false, highlightColor,
}: StatCardProps) {
  const bgClass = highlight
    ? highlightColor || "bg-rose-50 border-rose-200/50"
    : "bg-white border-slate-100 shadow-soft";

  const valueColor = highlight
    ? highlightColor?.includes("rose") ? "text-rose-600" : "text-slate-900"
    : "text-slate-900";

  return (
    <div className={`flex flex-col gap-3 rounded-xl p-6 border transition-transform hover:scale-[1.02] relative overflow-hidden ${bgClass}`}>
      {highlight && (
        <div className="absolute top-0 right-0 w-24 h-24 bg-current opacity-[0.06] rounded-bl-full -mr-4 -mt-4" />
      )}
      <div className="flex items-center justify-between relative z-10">
        <p className="text-slate-500 text-sm font-medium">{label}</p>
        {icon && (
          <div className={`size-8 rounded-full ${iconBg || "bg-slate-100"} flex items-center justify-center ${iconColor || "text-slate-500"}`}>
            <span className="material-symbols-outlined text-lg">{icon}</span>
          </div>
        )}
      </div>
      <div className="relative z-10">
        <p className={`tracking-tight text-2xl font-bold ${valueColor}`}>{value}</p>
        {change && (
          <div className={`flex items-center gap-1 mt-1 text-sm font-medium ${change.positive ? "text-green-600" : "text-red-500"}`}>
            <span className="material-symbols-outlined text-sm">{change.positive ? "trending_up" : "trending_down"}</span>
            <span>{change.value}</span>
          </div>
        )}
        {subtext && (
          <p className="text-sm text-slate-500 mt-1">{subtext}</p>
        )}
      </div>
    </div>
  );
}
