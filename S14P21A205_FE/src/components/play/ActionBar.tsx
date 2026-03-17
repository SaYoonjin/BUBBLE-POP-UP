export type ActionType = "discount" | "emergency" | "promotion" | "share" | "move";

interface ActionBarProps {
  onAction: (action: ActionType) => void;
}

const actions: { type: ActionType; icon: string; label: string; featured?: boolean }[] = [
  { type: "discount", icon: "🏷️", label: "할인" },
  { type: "emergency", icon: "🚚", label: "긴급발주" },
  { type: "promotion", icon: "📢", label: "홍보하기", featured: true },
  { type: "share", icon: "🤲", label: "나눔" },
  { type: "move", icon: "🚛", label: "팝업이전" },
];

export default function ActionBar({ onAction }: ActionBarProps) {
  return (
    <div className="absolute bottom-8 left-1/2 -translate-x-1/2 z-20 w-auto max-w-[90%]">
      <div className="glass-panel px-8 py-4 rounded-2xl shadow-xl border border-white/60 flex items-center gap-6">
        {actions.map((action) =>
          action.featured ? (
            <button
              key={action.type}
              onClick={() => onAction(action.type)}
              className="group flex flex-col items-center gap-1 min-w-[70px] transition-transform active:scale-95"
            >
              <div className="w-16 h-16 bg-primary text-white rounded-xl shadow-md border border-primary-dark flex items-center justify-center hover:bg-primary-dark transition-colors text-3xl -mt-6 ring-4 ring-white/50">
                {action.icon}
              </div>
              <span className="text-xs font-bold text-primary-dark">{action.label}</span>
            </button>
          ) : (
            <button
              key={action.type}
              onClick={() => onAction(action.type)}
              className="group flex flex-col items-center gap-1 min-w-[70px] transition-transform active:scale-95"
            >
              <div className="w-14 h-14 bg-white rounded-xl shadow-sm border border-slate-100 flex items-center justify-center group-hover:bg-primary group-hover:text-white transition-colors text-2xl">
                {action.icon}
              </div>
              <span className="text-xs font-bold text-slate-600 group-hover:text-primary transition-colors">
                {action.label}
              </span>
            </button>
          )
        )}
      </div>
    </div>
  );
}
