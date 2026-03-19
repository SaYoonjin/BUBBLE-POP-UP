interface RankCardProps {
  rank: 1 | 2 | 3;
  username: string;
  storeName: string;
  roi: string;
  revenue: string;
  isMe?: boolean;
}

const rankConfig: Record<number, { emoji: string; bg: string; border: string; height: string }> = {
  1: { emoji: "🥇", bg: "bg-amber-50", border: "border-amber-200", height: "h-48" },
  2: { emoji: "🥈", bg: "bg-gray-50", border: "border-gray-200", height: "h-40" },
  3: { emoji: "🥉", bg: "bg-orange-50", border: "border-orange-200", height: "h-36" },
};

export default function RankCard({
  rank,
  username,
  storeName,
  roi,
  revenue,
  isMe = false,
}: RankCardProps) {
  const config = rankConfig[rank];

  return (
    <div
      className={`
        flex flex-col items-center justify-end p-4 rounded-2xl border-2 transition-all
        ${config.bg} ${config.border} ${config.height}
        ${isMe ? "ring-2 ring-primary ring-offset-2" : ""}
      `}
    >
      <span className="text-3xl mb-2">{config.emoji}</span>
      <span className="text-sm font-bold text-gray-900">{username}</span>
      <span className="text-xs text-gray-500 mb-2">{storeName}</span>
      <div className="text-center">
        <div className="text-lg font-bold text-primary-dark">ROI {roi}</div>
        <div className="text-xs text-gray-500">매출 {revenue}</div>
      </div>
    </div>
  );
}
