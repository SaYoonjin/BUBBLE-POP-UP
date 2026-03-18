import { useState } from "react";

export type AlertType = "event" | "deadline" | "stock" | "action";

export interface GameAlert {
  id: number;
  type: AlertType;
  title: string;
  description: string;
  time: string;
}

interface EventSidebarProps {
  alerts: GameAlert[];
}

const typeStyles: Record<AlertType, { border: string; icon: string; iconColor: string }> = {
  event: { border: "border-primary", icon: "celebration", iconColor: "text-primary" },
  deadline: { border: "border-red-400", icon: "alarm", iconColor: "text-red-500" },
  stock: { border: "border-amber-400", icon: "inventory_2", iconColor: "text-amber-500" },
  action: { border: "border-blue-400", icon: "task_alt", iconColor: "text-blue-500" },
};

export default function EventSidebar({ alerts }: EventSidebarProps) {
  const [expanded, setExpanded] = useState(false);
  const latest = alerts[0];

  return (
    <aside className="absolute top-20 right-4 z-10 w-72 pointer-events-none">
      <div className="glass-panel rounded-2xl shadow-lg pointer-events-auto overflow-hidden">
        {/* Header - always visible */}
        <button
          onClick={() => setExpanded(!expanded)}
          className="w-full px-4 py-3 flex items-center justify-between hover:bg-white/40 transition-colors"
        >
          <h2 className="font-bold text-slate-800 text-sm flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[18px]">notifications_active</span>
            실시간 알림
            {alerts.length > 0 && (
              <span className="bg-primary text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full min-w-[18px] text-center">
                {alerts.length}
              </span>
            )}
          </h2>
          <span className={`material-symbols-outlined text-slate-400 text-[18px] transition-transform duration-200 ${expanded ? "rotate-180" : ""}`}>
            expand_more
          </span>
        </button>

        {/* Latest alert - always visible */}
        {latest && !expanded && (
          <div className="px-4 pb-3">
            <AlertCard alert={latest} />
          </div>
        )}

        {/* Expanded list */}
        {expanded && (
          <div className="px-4 pb-3 space-y-2 max-h-[400px] overflow-y-auto custom-scrollbar">
            {alerts.map((alert) => (
              <AlertCard key={alert.id} alert={alert} />
            ))}
          </div>
        )}
      </div>
    </aside>
  );
}

function AlertCard({ alert }: { alert: GameAlert }) {
  const style = typeStyles[alert.type];
  return (
    <div className={`bg-white p-3 rounded-xl border-l-[3px] ${style.border} shadow-sm`}>
      <div className="flex justify-between items-start mb-0.5">
        <div className="flex items-center gap-1.5">
          <span className={`material-symbols-outlined text-[14px] ${style.iconColor}`}>{style.icon}</span>
          <span className="text-[12px] font-bold text-slate-700">{alert.title}</span>
        </div>
        <span className="text-[10px] text-slate-400 shrink-0 ml-2">{alert.time}</span>
      </div>
      <p className="text-[11px] text-slate-500 mt-0.5 pl-5">{alert.description}</p>
    </div>
  );
}
