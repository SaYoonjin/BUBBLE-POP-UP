interface NewsItem {
  id: number;
  title: string;
  content?: string;
}

interface CozyNewspaperProps {
  items: NewsItem[];
  expandedId: number | null;
  onToggle: (id: number) => void;
  day?: number;
}

export default function CozyNewspaper({ items, expandedId, onToggle, day }: CozyNewspaperProps) {
  return (
    <div className="bg-cozy-paper rounded-sm relative overflow-hidden hover:-translate-y-0.5 transition-transform shadow-[0_10px_30px_-5px_rgba(0,0,0,0.18),0_4px_10px_-2px_rgba(0,0,0,0.1)]">
      {/* Paper texture */}
      <div
        className="absolute inset-0 opacity-40 mix-blend-multiply pointer-events-none"
        style={{ backgroundImage: "url('https://www.transparenttextures.com/patterns/cream-paper.png')" }}
      />
      {/* Fold line */}
      <div className="absolute top-1/2 left-0 w-full h-px bg-gradient-to-r from-transparent via-gray-300 to-transparent z-20" />

      <div className="p-8 relative z-10">
        {/* Masthead */}
        <div className="flex justify-between items-end border-b-4 border-cozy-ink mb-6 pb-2">
          <div>
            <h2 className="font-cozy-serif text-4xl md:text-5xl font-black text-cozy-ink tracking-tight uppercase">
              Bubble News
            </h2>
            <p className="font-cozy-serif italic text-cozy-ink/50 mt-1 text-sm">
              {day ? `DAY ${day} · ` : ""}Today's Market Headlines
            </p>
          </div>
        </div>

        {/* Headlines */}
        <div className="flex flex-col gap-0 divide-y divide-dashed divide-cozy-ink/10">
          {items.map((news, i) => {
            const isExpanded = news.id === expandedId;
            return (
              <div
                key={news.id}
                className="py-5 cursor-pointer group"
                onClick={() => onToggle(news.id)}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex gap-3">
                    {i === 0 && (
                      <span className="inline-block bg-cozy-primary text-white text-[9px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-sm mt-1 shrink-0">
                        HOT
                      </span>
                    )}
                    <h4 className={`leading-tight transition-colors ${
                      isExpanded
                        ? "font-cozy-serif text-xl font-bold text-cozy-ink"
                        : "font-cozy-serif text-lg text-cozy-ink/80 group-hover:text-cozy-primary italic"
                    }`}>
                      {news.title}
                    </h4>
                  </div>
                  <span className={`material-symbols-outlined text-cozy-ink/30 shrink-0 transition-transform ${isExpanded ? "rotate-180" : ""}`}>
                    expand_more
                  </span>
                </div>
                {isExpanded && news.content && (
                  <p className="mt-4 text-sm text-cozy-ink/60 leading-relaxed border-l-2 border-cozy-sage pl-4">
                    {news.content}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
