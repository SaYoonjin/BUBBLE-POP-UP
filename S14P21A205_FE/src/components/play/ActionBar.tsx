export type ActionType = "discount" | "emergency" | "promotion" | "share" | "move";

interface ActionBarProps {
  onAction: (action: ActionType) => void;
  usedActions: Set<ActionType>;
  activeEffects: Set<ActionType>;
}

const actions: { type: ActionType; icon: string; label: string }[] = [
  { type: "discount", icon: "sell", label: "할인" },
  { type: "emergency", icon: "local_shipping", label: "긴급발주" },
  { type: "promotion", icon: "campaign", label: "홍보하기" },
  { type: "share", icon: "volunteer_activism", label: "나눔" },
  { type: "move", icon: "move_location", label: "팝업이전" },
];

export default function ActionBar({ onAction, usedActions, activeEffects }: ActionBarProps) {
  return (
    <div className="absolute bottom-6 left-1/2 z-20 w-auto max-w-[90%] -translate-x-1/2">
      <div className="glass-panel flex items-center gap-4 rounded-2xl border border-white/60 px-6 py-3 shadow-xl">
        {actions.map((action) => {
          const isUsed = usedActions.has(action.type);
          const isActiveEffect = activeEffects.has(action.type);

          return (
            <button
              key={action.type}
              onClick={() => !isUsed && onAction(action.type)}
              disabled={isUsed}
              className={`group flex min-w-[74px] flex-col items-center gap-1.5 transition-all active:scale-95 ${
                isUsed ? "cursor-not-allowed" : ""
              }`}
            >
              <div
                className={`flex h-12 w-12 items-center justify-center rounded-xl border shadow-sm transition-colors ${
                  isActiveEffect
                    ? "border-emerald-200 bg-emerald-50 text-emerald-600"
                    : isUsed
                      ? "border-slate-200 bg-slate-100 text-slate-300"
                      : "border-slate-100 bg-white text-slate-600 group-hover:border-primary group-hover:bg-primary group-hover:text-white"
                }`}
              >
                <span className="material-symbols-outlined text-[22px]">{action.icon}</span>
              </div>
              <span
                className={`text-[11px] font-bold transition-colors ${
                  isActiveEffect
                    ? "text-slate-700"
                    : isUsed
                      ? "text-slate-400"
                      : "text-slate-600 group-hover:text-primary"
                }`}
              >
                {action.label}
              </span>
              {isActiveEffect && (
                <span className="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-[10px] font-semibold text-emerald-600 shadow-sm">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                  진행중
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
