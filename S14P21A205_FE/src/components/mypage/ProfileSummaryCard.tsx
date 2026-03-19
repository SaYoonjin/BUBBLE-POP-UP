import Badge from "../common/Badge";

interface ProfileSummaryCardProps {
  nickname: string;
  email: string;
  summaryBadges: string[];
  onEditNickname: () => void;
}

export default function ProfileSummaryCard({
  nickname,
  email,
  summaryBadges,
  onEditNickname,
}: ProfileSummaryCardProps) {
  return (
    <aside className="relative overflow-hidden rounded-[28px] border border-primary/20 bg-gradient-to-br from-white via-white to-primary/10 p-6 shadow-premium sm:p-7">
      <div className="absolute -right-10 -top-10 size-28 rounded-full bg-primary/10 blur-3xl" />
      <div className="absolute -left-6 bottom-20 size-24 rounded-full bg-accent-rose/10 blur-3xl" />

      <div className="relative flex flex-col gap-6">
        <div className="flex items-start gap-4">
          <div className="flex size-16 shrink-0 items-center justify-center rounded-3xl bg-primary/15 text-primary shadow-soft">
            <span className="material-symbols-outlined text-[30px]">person</span>
          </div>

          <div className="min-w-0 flex-1 space-y-2">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-primary-dark/75">
              My Page
            </p>

            <div className="flex items-center gap-2">
              <h1 className="truncate text-2xl font-bold text-slate-900">{nickname}</h1>
              <button
                type="button"
                onClick={onEditNickname}
                className="rounded-full p-1 text-slate-400 transition-colors hover:bg-white/80 hover:text-primary"
                title="닉네임 수정"
              >
                <span className="material-symbols-outlined text-lg">edit</span>
              </button>
            </div>

            <p className="break-all font-mono text-sm text-slate-500">{email}</p>
          </div>
        </div>

        <div className="rounded-3xl border border-white/70 bg-white/80 p-4 backdrop-blur">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">
            활동 요약
          </p>

          <div className="mt-3 flex flex-wrap gap-2">
            {summaryBadges.map((badge, index) => (
              <Badge key={badge} size="md" variant={index === 1 ? "gold" : "green"}>
                {badge}
              </Badge>
            ))}
          </div>
        </div>
      </div>
    </aside>
  );
}
