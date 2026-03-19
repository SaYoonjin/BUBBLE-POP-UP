interface NewsItem {
  id: number;
  title: string;
  content?: string;
}

interface NewsAccordionProps {
  items: NewsItem[];
  expandedId: number | null;
  onToggle: (id: number) => void;
}

export default function NewsAccordion({ items, expandedId, onToggle }: NewsAccordionProps) {
  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-center justify-between mb-2 px-2">
        <h3 className="text-lg font-bold text-slate-900 flex items-center gap-3 tracking-tight">
          <span className="p-2 bg-slate-50 rounded-lg border border-slate-100">
            <span className="material-symbols-outlined text-primary text-xl">newspaper</span>
          </span>
          최신 뉴스 헤드라인
        </h3>
      </div>

      {items.map((item, i) => {
        const isExpanded = item.id === expandedId;
        const isFirst = i === 0;

        return (
          <div
            key={item.id}
            className={`bg-white rounded-2xl border p-${isExpanded ? "8" : "6"} transition-all duration-300 ${
              isFirst && isExpanded
                ? "border-primary shadow-premium"
                : "border-slate-100/50 shadow-soft hover:shadow-premium hover:-translate-y-0.5"
            } cursor-pointer group`}
            onClick={() => onToggle(item.id)}
          >
            <div className="flex items-start justify-between gap-6">
              <h4
                className={`font-semibold leading-tight tracking-tight transition-colors ${
                  isExpanded ? "text-xl font-bold text-slate-900" : "text-lg text-slate-800 group-hover:text-primary-dark"
                }`}
              >
                {item.title}
              </h4>
              <button className={`shrink-0 p-1 rounded-full transition-colors ${
                isExpanded ? "text-primary bg-primary/10" : "text-slate-300 group-hover:text-primary"
              }`}>
                <span className="material-symbols-outlined text-xl">
                  {isExpanded ? "expand_less" : "expand_more"}
                </span>
              </button>
            </div>

            {isExpanded && item.content && (
              <div className="mt-6 pt-6 border-t border-slate-100 text-slate-600 text-[15px] leading-[1.6] font-normal tracking-wide">
                <p>{item.content}</p>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
