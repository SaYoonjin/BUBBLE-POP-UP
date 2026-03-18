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
        <div className="inline-flex w-fit items-center gap-2 rounded-full border border-white/80 bg-white/85 px-3 py-1.5 text-sm font-semibold text-primary-dark shadow-soft backdrop-blur">
          <span className="material-symbols-outlined text-base text-primary">shield_person</span>
          <span>계정</span>
        </div>

        <div className="space-y-3">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <h1 className="truncate text-3xl font-bold tracking-tight text-slate-900">
                {nickname}
              </h1>
              <p className="mt-2 break-all font-mono text-sm text-slate-500">{email}</p>
            </div>

            <button
              type="button"
              onClick={onEditNickname}
              className="rounded-full border border-slate-200 bg-white/90 p-2 text-slate-400 shadow-soft transition-colors hover:border-primary/40 hover:text-primary"
              title="닉네임 수정"
            >
              <span className="material-symbols-outlined text-lg">edit</span>
            </button>
          </div>

          <p className="text-sm leading-6 text-slate-500">
            닉네임은 언제든지 수정할 수 있고, 시즌 기록은 최근 10개 시즌 기준으로 정리돼요.
          </p>
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
