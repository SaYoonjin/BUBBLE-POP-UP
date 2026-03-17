interface CozyNewsCardProps {
  title: string;
  content?: string;
  isOpen: boolean;
  onToggle: () => void;
  variant?: "featured" | "default";
}

export default function CozyNewsCard({ title, content, isOpen, onToggle, variant = "default" }: CozyNewsCardProps) {
  return (
    <div className={`
      relative overflow-hidden transition-all font-cozy-display
      ${variant === "featured"
        ? "bg-cozy-paper shadow-cozy-paper border-l-4 border-cozy-primary"
        : "bg-cozy-paper shadow-cozy-paper"
      }
      ${variant === "featured" ? "rotate-[-0.5deg]" : "rotate-[0.3deg]"}
      hover:rotate-0 transition-transform duration-300
    `}>
      {/* Paper texture */}
      <div className="absolute inset-0 opacity-30 pointer-events-none"
        style={{ backgroundImage: "url('https://www.transparenttextures.com/patterns/cream-paper.png')" }}
      />
      <button className="relative z-10 w-full flex items-center justify-between px-5 py-4 text-left cursor-pointer" onClick={onToggle}>
        <div className="flex items-center gap-3">
          {variant === "featured" && (
            <span className="bg-cozy-primary text-white text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full">HOT</span>
          )}
          <span className="font-cozy-serif font-bold text-cozy-ink text-base italic">{title}</span>
        </div>
        <span className={`material-symbols-outlined text-cozy-wood-dark transition-transform ${isOpen ? "rotate-180" : ""}`}>
          expand_more
        </span>
      </button>
      {isOpen && content && (
        <div className="relative z-10 px-5 pb-4 text-sm text-cozy-ink/70 leading-relaxed border-t border-dashed border-cozy-wood-light pt-3 font-cozy-display">
          {content}
          <div className="mt-2 flex items-center gap-1 text-xs font-bold text-cozy-dusty-rose uppercase tracking-wider cursor-pointer hover:text-cozy-primary">
            Read more <span className="material-symbols-outlined text-sm">arrow_forward</span>
          </div>
        </div>
      )}
    </div>
  );
}
