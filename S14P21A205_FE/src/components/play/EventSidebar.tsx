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

const typeIcons: Record<AlertType, string> = {
  event: "celebration",
  deadline: "alarm",
  stock: "inventory_2",
  action: "task_alt",
};

const typeIconStyles: Record<AlertType, { chip: string; icon: string }> = {
  event: {
    chip: "bg-cozy-primary/10",
    icon: "text-cozy-primary",
  },
  deadline: {
    chip: "bg-accent-rose/15",
    icon: "text-rose-dark",
  },
  stock: {
    chip: "bg-amber-50",
    icon: "text-amber-600",
  },
  action: {
    chip: "bg-primary/15",
    icon: "text-primary-dark",
  },
};

export default function EventSidebar({ alerts }: EventSidebarProps) {
  const [expanded, setExpanded] = useState(false);
  const latest = alerts[0];

  return (
    <aside className="absolute top-20 right-4 z-10 w-72 pointer-events-none">
      <div className="glass-panel rounded-2xl shadow-lg pointer-events-auto overflow-hidden">
        <button
          type="button"
          onClick={() => setExpanded((prev) => !prev)}
          className="w-full px-4 py-3 flex items-center justify-between hover:bg-white/40 transition-colors"
        >
          <h2 className="font-bold text-slate-800 text-sm flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[18px]">notifications_active</span>
            실시간 알림
          </h2>
          <span className={`material-symbols-outlined text-slate-400 text-[18px] transition-transform duration-200 ${expanded ? "rotate-180" : ""}`}>
            expand_more
          </span>
        </button>

        {latest && !expanded && (
          <div className="px-4 pb-3">
            <AlertCard alert={latest} />
          </div>
        )}

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
  const iconStyle = typeIconStyles[alert.type];

  return (
    <div className="rounded-xl border border-slate-200 bg-white px-3 py-3 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-start gap-2.5">
          <div className={`mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-full ${iconStyle.chip}`}>
            <span className={`material-symbols-outlined text-[15px] ${iconStyle.icon}`}>{typeIcons[alert.type]}</span>
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-[12px] font-bold text-slate-700">{alert.title}</p>
            <p className="mt-0.5 truncate text-[11px] text-slate-500">{alert.description}</p>
          </div>
        </div>
        <span className="shrink-0 text-[10px] text-slate-400">{alert.time}</span>
      </div>
    </div>
  );
}
