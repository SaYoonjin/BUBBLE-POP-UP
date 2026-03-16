interface EventToastProps {
  type: "GOOD_NEWS" | "WARNING" | "SYSTEM";
  title: string;
  description: string;
  timeAgo: string;
}

const typeConfig: Record<string, { icon: string; bg: string; iconColor: string }> = {
  GOOD_NEWS: { icon: "trending_up", bg: "bg-green-50 border-green-200", iconColor: "text-green-600" },
  WARNING: { icon: "warning", bg: "bg-amber-50 border-amber-200", iconColor: "text-amber-600" },
  SYSTEM: { icon: "info", bg: "bg-blue-50 border-blue-200", iconColor: "text-blue-600" },
};

export default function EventToast({
  type,
  title,
  description,
  timeAgo,
}: EventToastProps) {
  const config = typeConfig[type];

  return (
    <div className={`flex gap-3 p-3 rounded-xl border ${config.bg} animate-[slideIn_0.3s_ease-out]`}>
      <span className={`material-symbols-outlined ${config.iconColor} text-xl mt-0.5`}>
        {config.icon}
      </span>
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-gray-900">{title}</span>
          <span className="text-xs text-gray-400 ml-2 shrink-0">{timeAgo}</span>
        </div>
        <p className="text-xs text-gray-600 mt-0.5 leading-relaxed">{description}</p>
      </div>
    </div>
  );
}
