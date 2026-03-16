interface RankItem {
  rank: number;
  name: string;
  change: string;
  positive: boolean;
}

interface CozyNotebookProps {
  title: string;
  items: RankItem[];
  memo?: string;
  compact?: boolean;
}

export default function CozyNotebook({ title, items, memo, compact = false }: CozyNotebookProps) {
  return (
    <div className="relative bg-white rounded-lg rotate-1 hover:rotate-0 transition-transform shadow-[0_15px_35px_-8px_rgba(0,0,0,0.2),0_6px_12px_-3px_rgba(0,0,0,0.1)]">
      {/* Spiral binding */}
      <div className="absolute -top-3 left-0 w-full h-8 flex justify-evenly z-20">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="w-2 h-6 bg-zinc-400 rounded-full shadow-md" />
        ))}
      </div>

      <div
        className={`p-5 pt-9 ${compact ? "min-h-0" : "min-h-[300px]"}`}
        style={{
          backgroundImage: "linear-gradient(#e5e7eb 1px, transparent 1px)",
          backgroundSize: compact ? "100% 1.6rem" : "100% 2rem",
        }}
      >
        <h3 className={`font-cozy-hand text-cozy-primary text-center -rotate-2 ${compact ? "text-2xl mb-4" : "text-3xl mb-6"}`}>
          {title}
        </h3>

        <ul className={`flex flex-col ${compact ? "gap-2.5" : "gap-4"} pl-2`}>
          {items.map((item) => (
            <li key={item.rank} className="flex items-center gap-2">
              <span className={`font-cozy-hand text-cozy-dusty-rose w-5 ${compact ? "text-lg" : "text-xl"}`}>{item.rank}.</span>
              <div className="flex-1 border-b border-cozy-ink/10 pb-0.5 flex justify-between items-baseline">
                <span className={`font-cozy-hand text-cozy-ink/80 ${compact ? "text-base" : "text-lg"}`}>{item.name}</span>
                <span
                  className={`text-xs font-mono px-1 rounded font-bold ${
                    item.positive ? "bg-cozy-sage/20 text-cozy-sage" : "bg-red-100 text-red-400"
                  }`}
                >
                  {item.change}
                </span>
              </div>
            </li>
          ))}
        </ul>

        {memo && (
          <div className={`-rotate-[3deg] ${compact ? "mt-4" : "mt-8"}`}>
            <div className="bg-yellow-200/80 p-2.5 shadow-sm inline-block max-w-[85%]">
              <p className="font-cozy-hand text-sm text-cozy-ink/70">{memo}</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
