export type ActionType = "discount" | "emergency" | "promotion" | "share" | "move";

interface ActionBarProps {
  onAction: (action: ActionType) => void;
  usedActions: Set<ActionType>;
}

const actions: { type: ActionType; icon: string; label: string }[] = [
  { type: "discount", icon: "sell", label: "할인" },
  { type: "emergency", icon: "local_shipping", label: "긴급발주" },
  { type: "promotion", icon: "campaign", label: "홍보하기" },
  { type: "share", icon: "volunteer_activism", label: "나눔" },
  { type: "move", icon: "move_location", label: "팝업이전" },
];

export default function ActionBar({ onAction, usedActions }: ActionBarProps) {
  return (
    <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-20 w-auto max-w-[90%]">
      <div className="glass-panel px-6 py-3 rounded-2xl shadow-xl border border-white/60 flex items-center gap-4">
        {actions.map((action) => {
          const isUsed = usedActions.has(action.type);
          return (
            <button
              key={action.type}
              onClick={() => !isUsed && onAction(action.type)}
              disabled={isUsed}
              className={`group flex flex-col items-center gap-1 min-w-[60px] transition-all active:scale-95 ${
                isUsed ? "opacity-40 cursor-not-allowed" : ""
              }`}
            >
              <div className={`w-12 h-12 rounded-xl shadow-sm border flex items-center justify-center transition-colors ${
                isUsed
                  ? "bg-slate-100 border-slate-200 text-slate-300"
                  : "bg-white border-slate-100 text-slate-600 group-hover:bg-primary group-hover:text-white group-hover:border-primary"
              }`}>
                <span className="material-symbols-outlined text-[22px]">{action.icon}</span>
              </div>
              <span className={`text-[11px] font-bold transition-colors ${
                isUsed ? "text-slate-300" : "text-slate-600 group-hover:text-primary"
              }`}>
                {isUsed ? "사용완료" : action.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
