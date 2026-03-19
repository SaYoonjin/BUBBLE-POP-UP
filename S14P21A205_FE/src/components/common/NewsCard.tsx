interface NewsCardProps {
  title: string;
  content?: string;
  isOpen: boolean;
  onToggle: () => void;
  variant?: "featured" | "default";
}

export default function NewsCard({
  title,
  content,
  isOpen,
  onToggle,
  variant = "default",
}: NewsCardProps) {
  return (
    <div
      className={`
        rounded-xl border overflow-hidden transition-all
        ${variant === "featured"
          ? "border-primary/30 bg-primary/5"
          : "border-gray-100 bg-card-light"
        }
      `}
    >
      <button
        className="w-full flex items-center justify-between px-4 py-3 text-left cursor-pointer"
        onClick={onToggle}
      >
        <div className="flex items-center gap-2">
          {variant === "featured" && (
            <span className="material-symbols-outlined text-primary text-lg">breaking_news</span>
          )}
          <span className="font-medium text-gray-900 text-sm">{title}</span>
        </div>
        <span
          className={`material-symbols-outlined text-gray-400 text-lg transition-transform ${isOpen ? "rotate-180" : ""}`}
        >
          expand_more
        </span>
      </button>

      {isOpen && content && (
        <div className="px-4 pb-4 text-sm text-gray-600 leading-relaxed border-t border-gray-50 pt-3">
          {content}
        </div>
      )}
    </div>
  );
}
