interface NewsItem {
  id: number;
  title: string;
  content?: string;
}

interface RankingItem {
  rank: number;
  name: string;
  change: string;
  positive: boolean;
}

interface RankingSection {
  title: string;
  eyebrow: string;
  items?: RankingItem[];
  imageSrc?: string;
  imageAlt?: string;
  caption?: string;
  captionDetail?: string;
  meta?: string[];
}

interface CozyNewspaperProps {
  items: NewsItem[];
  expandedId: number | null;
  onToggle: (id: number) => void;
  day?: number;
  rankings?: RankingSection[];
}

export default function CozyNewspaper({ items, expandedId, onToggle, day, rankings = [] }: CozyNewspaperProps) {
  const [leadStory, ...otherStories] = items;

  return (
    <div className="bg-cozy-paper rounded-sm relative overflow-hidden hover:-translate-y-0.5 transition-transform shadow-[0_10px_30px_-5px_rgba(0,0,0,0.18),0_4px_10px_-2px_rgba(0,0,0,0.1)]">
      {/* Paper texture */}
      <div
        className="absolute inset-0 opacity-40 mix-blend-multiply pointer-events-none"
        style={{ backgroundImage: "url('https://www.transparenttextures.com/patterns/cream-paper.png')" }}
      />

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

        <div className={`grid gap-8 ${rankings.length > 0 ? "lg:grid-cols-12" : "grid-cols-1"}`}>
          <section className={rankings.length > 0 ? "lg:col-span-7" : ""}>
            {leadStory && (
              <article className="pb-6 border-b-2 border-cozy-ink/15">
                <div className="flex items-center gap-3 mb-4">
                  <span className="inline-block bg-red-600 text-white text-[10px] font-bold uppercase tracking-[0.25em] px-3 py-1 rounded-sm">
                    Top Story
                  </span>
                  <span className="text-[11px] uppercase tracking-[0.3em] text-cozy-ink/35 font-bold">
                    Business Desk
                  </span>
                </div>
                <h3 className="font-cozy-serif text-3xl md:text-4xl font-black leading-tight text-cozy-ink tracking-tight">
                  {leadStory.title}
                </h3>
                {leadStory.content && (
                  <p className="mt-5 text-[15px] leading-7 text-cozy-ink/70 border-l-2 border-cozy-sage pl-5">
                    {leadStory.content}
                  </p>
                )}
              </article>
            )}

            <div className="mt-4 flex flex-col divide-y divide-dashed divide-cozy-ink/10">
              {otherStories.map((news) => {
                const isExpanded = news.id === expandedId;
                return (
                  <article key={news.id} className="py-5 cursor-pointer group" onClick={() => onToggle(news.id)}>
                    <div className="flex items-start justify-between gap-4">
                      <h4 className={`leading-tight transition-colors ${
                        isExpanded
                          ? "font-cozy-serif text-xl font-bold text-cozy-ink"
                          : "font-cozy-serif text-lg text-cozy-ink/80 group-hover:text-[#8DA98EFF] italic"
                      }`}>
                        {news.title}
                      </h4>
                      <span className={`material-symbols-outlined text-cozy-ink/30 shrink-0 transition-transform ${isExpanded ? "rotate-180" : ""}`}>
                        expand_more
                      </span>
                    </div>
                    {isExpanded && news.content && (
                      <p className="mt-4 text-sm text-cozy-ink/60 leading-relaxed border-l-2 border-cozy-sage pl-4">
                        {news.content}
                      </p>
                    )}
                  </article>
                );
              })}
            </div>
          </section>

          {rankings.length > 0 && (
            <aside className="lg:col-span-5 lg:border-l border-cozy-ink/10 lg:pl-8 flex flex-col gap-8">
              {rankings.map((section, index) => {
                const isImageOnlySection = Boolean(section.imageSrc) && !(section.items?.length);

                return (
                <section
                  key={section.title || section.imageSrc || `section-${index}`}
                  className="pb-6 border-b border-dashed border-cozy-ink/10 last:border-b-0 last:pb-0"
                >
                  {!isImageOnlySection && (
                    <div className="flex items-end justify-between gap-4 mb-4">
                      <div>
                        <p className="text-[10px] uppercase tracking-[0.3em] text-cozy-ink/35 font-bold">
                          {section.eyebrow}
                        </p>
                        <h4 className="font-display text-[22px] font-bold tracking-tight text-slate-800">
                          {section.title}
                        </h4>
                      </div>
                      <span className="font-mono text-xs text-cozy-ink/35">0{index + 1}</span>
                    </div>
                  )}

                  {section.imageSrc ? (
                    <div className="flex flex-col gap-4">
                      <div className="overflow-hidden rounded-sm border border-cozy-ink/15 bg-[#e4d8c6] shadow-[inset_0_1px_0_rgba(255,255,255,0.6)]">
                        <img
                          src={section.imageSrc}
                          alt={section.imageAlt ?? section.title}
                          className="h-full w-full object-cover"
                        />
                      </div>
                      {(section.caption || section.captionDetail || (section.meta?.length ?? 0) > 0) && (
                        <div>
                          {section.caption && (
                            <p className="font-cozy-serif text-xl font-bold leading-tight text-cozy-ink tracking-tight">
                              {section.caption}
                            </p>
                          )}
                          {section.captionDetail && (
                            <p className="mt-3 border-l-2 border-cozy-sage pl-4 text-sm leading-relaxed text-cozy-ink/60">
                              {section.captionDetail}
                            </p>
                          )}
                          {section.meta && section.meta.length > 0 && (
                            <div className="mt-4 flex w-full flex-wrap items-center justify-end gap-x-3 gap-y-1 text-right text-[10px] font-bold uppercase tracking-[0.3em] text-cozy-ink/35">
                              {section.meta.map((metaItem, metaIndex) => (
                                <span
                                  key={`${section.imageSrc}-${metaItem}`}
                                  className="inline-flex items-center gap-3"
                                >
                                  {metaIndex > 0 && <span className="text-cozy-ink/20">/</span>}
                                  <span>{metaItem}</span>
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  ) : (
                    <ul className="flex flex-col gap-3">
                      {(section.items ?? []).map((item) => {
                        const isNeutral =
                          item.change === "-" || item.change === "0%" || item.change === "0.0%";

                        return (
                          <li
                            key={`${section.title}-${item.rank}-${item.name}`}
                            className="flex items-center gap-3 border-b border-cozy-ink/10 pb-3 last:border-b-0 last:pb-0"
                          >
                            <span className="font-mono text-lg font-bold text-slate-500 w-7 shrink-0">
                              {item.rank}.
                            </span>
                            <div className="flex min-w-0 flex-1 items-center justify-between gap-3">
                              <span className="font-body text-[15px] font-semibold text-slate-800 truncate">
                                {item.name}
                              </span>
                              <div
                                className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-bold ${
                                  isNeutral
                                    ? "border-slate-200 bg-slate-100 text-slate-500"
                                    : item.positive
                                      ? "border-primary/30 bg-primary/12 text-primary-dark"
                                      : "border-red-200 bg-red-50 text-red-500"
                                }`}
                              >
                                <span className="material-symbols-outlined text-[14px] leading-none">
                                  {isNeutral
                                    ? "trending_flat"
                                    : item.positive
                                      ? "north_east"
                                      : "south_east"}
                                </span>
                                <span className="font-mono">{item.change}</span>
                              </div>
                            </div>
                          </li>
                        );
                      })}
                    </ul>
                  )}
                </section>
              )})}
            </aside>
          )}
        </div>
      </div>
    </div>
  );
}
