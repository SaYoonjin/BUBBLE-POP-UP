interface CozyRankCardProps {
  rank: 1 | 2 | 3;
  username: string;
  storeName: string;
  roi: string;
  revenue: string;
  isMe?: boolean;
}

const rankConfig: Record<number, { bg: string; height: string; numberColor: string }> = {
  1: { bg: "bg-[#fff9c4]", height: "h-56", numberColor: "text-amber-600" },
  2: { bg: "bg-cozy-cream", height: "h-48", numberColor: "text-gray-500" },
  3: { bg: "bg-[#fff3e0]", height: "h-40", numberColor: "text-orange-400" },
};

export default function CozyRankCard({ rank, username, storeName, roi, revenue, isMe = false }: CozyRankCardProps) {
  const config = rankConfig[rank];

  return (
    <div className={`
      relative flex flex-col items-center justify-end p-5 rounded-2xl transition-all font-cozy-display
      ${config.bg} ${config.height}
      shadow-cozy-clay hover:-translate-y-1
      ${isMe ? "ring-4 ring-cozy-primary ring-offset-2 ring-offset-cozy-cream" : ""}
    `}>
      {/* Handwritten rank number */}
      <span className={`font-cozy-hand text-6xl font-bold ${config.numberColor} absolute top-3 opacity-30`}>
        {rank}
      </span>
      <div className="relative z-10 flex flex-col items-center">
        <div className="size-14 rounded-full bg-white shadow-md border-2 border-cozy-wood-light flex items-center justify-center mb-2">
          <span className="material-symbols-outlined text-cozy-primary text-2xl">person</span>
        </div>
        <span className="font-bold text-cozy-ink">{username}</span>
        <span className="text-xs text-cozy-wood-dark">{storeName}</span>
        <div className="mt-2 bg-white/80 rounded-xl px-3 py-1.5 text-center shadow-sm">
          <div className="font-cozy-hand text-2xl font-bold text-cozy-primary">{roi}</div>
          <div className="text-[10px] text-cozy-wood-dark uppercase tracking-wider">매출 {revenue}</div>
        </div>
      </div>
    </div>
  );
}
