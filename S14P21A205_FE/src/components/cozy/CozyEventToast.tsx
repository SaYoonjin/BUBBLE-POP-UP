interface CozyEventToastProps {
  type: "GOOD_NEWS" | "WARNING" | "SYSTEM";
  title: string;
  description: string;
  timeAgo: string;
}

const typeConfig: Record<string, { icon: string; bg: string; iconBg: string; iconColor: string }> = {
  GOOD_NEWS: { icon: "trending_up", bg: "bg-[#e8f5e9] border-cozy-sage-green/30", iconBg: "bg-cozy-sage-green", iconColor: "text-white" },
  WARNING: { icon: "warning", bg: "bg-[#fff3e0] border-amber-200", iconBg: "bg-amber-500", iconColor: "text-white" },
  SYSTEM: { icon: "info", bg: "bg-[#e3f2fd] border-blue-200", iconBg: "bg-blue-500", iconColor: "text-white" },
};

export default function CozyEventToast({ type, title, description, timeAgo }: CozyEventToastProps) {
  const config = typeConfig[type];
  return (
    <div className={`
      flex gap-3 p-4 rounded-2xl border-2 font-cozy-display shadow-cozy-clay
      ${config.bg}
      transform rotate-[0.5deg] hover:rotate-0 transition-transform
    `}>
      <div className={`size-10 rounded-xl ${config.iconBg} ${config.iconColor} flex items-center justify-center shrink-0 shadow-md`}>
        <span className="material-symbols-outlined text-xl">{config.icon}</span>
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between">
          <span className="text-sm font-bold text-cozy-ink">{title}</span>
          <span className="text-[10px] text-cozy-wood-dark ml-2 shrink-0 font-cozy-hand text-base">{timeAgo}</span>
        </div>
        <p className="text-xs text-cozy-ink/70 mt-0.5 leading-relaxed">{description}</p>
      </div>
    </div>
  );
}
