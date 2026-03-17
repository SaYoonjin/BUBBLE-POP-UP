interface StatCardProps {
  label: string;
  value: string | number;
  change?: {
    value: string;
    positive: boolean;
  };
  icon?: string;
  highlight?: boolean;
}

export default function StatCard({
  label,
  value,
  change,
  icon,
  highlight = false,
}: StatCardProps) {
  return (
    <div
      className={`
        p-4 rounded-xl border transition-all
        ${highlight
          ? "bg-accent-rose/5 border-accent-rose/20"
          : "bg-card-light border-gray-100 shadow-soft"
        }
      `}
    >
      <div className="flex items-center justify-between mb-2">
        <span className="text-sm text-gray-500">{label}</span>
        {icon && (
          <span className="material-symbols-outlined text-gray-400 text-xl">
            {icon}
          </span>
        )}
      </div>
      <div className="text-2xl font-bold text-gray-900">{value}</div>
      {change && (
        <div className={`text-sm mt-1 ${change.positive ? "text-green-600" : "text-red-500"}`}>
          {change.positive ? "▲" : "▼"} {change.value}
        </div>
      )}
    </div>
  );
}
