import Badge from "../common/Badge";

type RankVariant = "gold" | "gray" | "rose";
type SeasonStatus = "default" | "bankrupt" | "comeback";

interface SeasonHistoryCardProps {
  season: number;
  location: string;
  storeName: string;
  revenue: string;
  rewardPoints: string;
  rank: string;
  rankVariant?: RankVariant;
  status?: SeasonStatus;
}

const badgeVariantByRank: Record<RankVariant, "gold" | "gray" | "rose"> = {
  gold: "gold",
  gray: "gray",
  rose: "rose",
};

const statusStyles: Record<SeasonStatus, { card: string; accent: string; value: string }> = {
  default: {
    card: "border-slate-200 bg-white",
    accent: "from-primary/70 via-primary/25 to-accent-rose/40",
    value: "text-slate-900",
  },
  bankrupt: {
    card: "border-rose-200 bg-gradient-to-br from-white via-rose-50/60 to-white",
    accent: "from-accent-rose/80 via-accent-rose/35 to-rose-100",
    value: "text-rose-500",
  },
  comeback: {
    card: "border-primary/25 bg-gradient-to-br from-white via-primary/5 to-white",
    accent: "from-primary/80 via-primary/35 to-primary-light/80",
    value: "text-slate-900",
  },
};

export default function SeasonHistoryCard({
  season,
  location,
  storeName,
  revenue,
  rewardPoints,
  rank,
  rankVariant = "gray",
  status = "default",
}: SeasonHistoryCardProps) {
  const infoItems = [
    { label: "수익", value: revenue, icon: "payments" },
    { label: "보상 포인트", value: rewardPoints, icon: "stars" },
  ];

  const currentStyle = statusStyles[status];

  return (
    <article
      className={`group relative overflow-hidden rounded-[24px] border p-5 shadow-soft transition-all duration-300 hover:-translate-y-0.5 hover:shadow-premium sm:p-6 ${currentStyle.card}`}
    >
      <div className={`absolute inset-x-0 top-0 h-1 bg-gradient-to-r ${currentStyle.accent}`} />

      <div className="flex flex-col gap-5 lg:flex-row lg:items-stretch">
        <div className="flex items-center justify-between gap-4 border-b border-slate-100 pb-4 lg:w-44 lg:flex-col lg:items-start lg:justify-between lg:border-b-0 lg:border-r lg:pb-0 lg:pr-5">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">
              Season
            </p>
            <h3 className="mt-2 text-2xl font-bold tracking-tight text-slate-900">
              Season {season}
            </h3>
          </div>

          <div className="flex flex-wrap items-center justify-end gap-2 lg:justify-start">
            <Badge size="md" variant={badgeVariantByRank[rankVariant]}>
              {rank}
            </Badge>
            {status === "comeback" && (
              <Badge size="md" variant="green">
                재참여
              </Badge>
            )}
          </div>
        </div>

        <div className="flex-1 space-y-4">
          <div>
            <div className="flex items-center gap-2 text-xs font-medium text-slate-400">
              <span className="material-symbols-outlined text-sm">location_on</span>
              <span>{location}</span>
            </div>

            <h4
              className="mt-2 text-xl font-bold leading-snug text-slate-900"
              style={{
                display: "-webkit-box",
                WebkitBoxOrient: "vertical",
                WebkitLineClamp: 2,
                overflow: "hidden",
                wordBreak: "keep-all",
              }}
            >
              {storeName}
            </h4>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {infoItems.map((item) => (
              <div
                key={item.label}
                className="rounded-2xl border border-slate-100 bg-slate-50/80 px-4 py-3"
              >
                <div className="flex items-center gap-2 text-xs font-medium text-slate-400">
                  <span className="material-symbols-outlined text-sm">{item.icon}</span>
                  <span>{item.label}</span>
                </div>
                <p className={`mt-2 text-sm font-semibold leading-6 ${currentStyle.value}`}>
                  {item.value}
                </p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </article>
  );
}
