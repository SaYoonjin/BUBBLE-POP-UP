export interface GameEvent {
  id: number;
  type: "good" | "warning" | "system";
  title: string;
  description: string;
  time: string;
}

interface EventSidebarProps {
  events: GameEvent[];
}

const typeStyles: Record<GameEvent["type"], { border: string; label: string; labelColor: string }> = {
  good: { border: "border-primary", label: "Good News", labelColor: "text-primary" },
  warning: { border: "border-rose-soft", label: "Warning", labelColor: "text-rose-soft" },
  system: { border: "border-blue-400", label: "System", labelColor: "text-blue-500" },
};

export default function EventSidebar({ events }: EventSidebarProps) {
  return (
    <aside className="relative z-10 w-80 h-full p-4 pointer-events-none flex flex-col items-end shrink-0">
      <div className="glass-panel w-full rounded-2xl p-4 shadow-lg pointer-events-auto flex flex-col h-full max-h-[600px]">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-bold text-slate-800 flex items-center gap-2">
            <span className="material-symbols-outlined text-primary">notifications_active</span>
            실시간 이벤트
          </h2>
        </div>

        <div className="flex-1 overflow-y-auto space-y-3 pr-1 custom-scrollbar">
          {events.map((event) => {
            const style = typeStyles[event.type];
            return (
              <div key={event.id} className={`bg-white p-3 rounded-xl border-l-4 ${style.border} shadow-sm`}>
                <div className="flex justify-between items-start mb-1">
                  <span className={`text-xs font-bold ${style.labelColor} uppercase`}>{style.label}</span>
                  <span className="text-[10px] text-slate-400">{event.time}</span>
                </div>
                <p className="text-sm text-slate-800 font-medium">{event.title}</p>
                <p className="text-xs text-slate-500 mt-1">{event.description}</p>
              </div>
            );
          })}
        </div>
      </div>
    </aside>
  );
}
