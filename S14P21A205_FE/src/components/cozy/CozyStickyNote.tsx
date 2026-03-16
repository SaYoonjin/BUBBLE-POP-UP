interface CozyStickyNoteProps {
  icon?: string;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  color?: "yellow" | "pink";
}

export default function CozyStickyNote({
  icon = "error",
  title,
  description,
  actionLabel,
  onAction,
  color = "yellow",
}: CozyStickyNoteProps) {
  const bgColor = color === "yellow" ? "bg-yellow-200/90" : "bg-pink-100/90";

  return (
    <div className={`${bgColor} p-6 rotate-[2deg] hover:rotate-0 transition-transform cursor-default relative shadow-[0_12px_30px_-6px_rgba(0,0,0,0.2),0_4px_8px_-2px_rgba(0,0,0,0.1)]`}>
      {/* Tape */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 w-16 h-6 bg-white/40 backdrop-blur-sm rounded-full border border-white/20" />
      <div className="flex items-start gap-4">
        <span className="material-symbols-outlined text-cozy-primary text-3xl">{icon}</span>
        <div>
          <h4 className="font-cozy-hand text-xl text-cozy-ink mb-1">{title}</h4>
          <p className="font-cozy-display text-xs text-cozy-ink/70 mb-4 leading-relaxed">{description}</p>
          {actionLabel && (
            <button
              onClick={onAction}
              className="bg-cozy-ink text-white px-4 py-2 text-xs font-bold uppercase tracking-widest hover:bg-cozy-primary transition-colors flex items-center gap-2"
            >
              {actionLabel}
              <span className="material-symbols-outlined text-sm">refresh</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
