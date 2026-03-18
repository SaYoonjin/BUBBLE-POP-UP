interface SeasonHistoryEmptyStateProps {
  nickname: string;
}

export default function SeasonHistoryEmptyState({
  nickname,
}: SeasonHistoryEmptyStateProps) {
  return (
    <div className="rounded-[28px] border border-dashed border-primary/30 bg-white/75 p-8 shadow-soft sm:p-10">
      <div className="flex size-14 items-center justify-center rounded-2xl bg-primary/10 text-primary">
        <span className="material-symbols-outlined text-3xl">history</span>
      </div>

      <div className="mt-6 space-y-3">
        <h3 className="text-2xl font-bold tracking-tight text-slate-900">
          아직 참여한 시즌이 없어요
        </h3>
        <p className="text-sm leading-7 text-slate-500">
          {nickname}님의 시즌 기록이 쌓이면 최근 10개 시즌 기준으로 이 영역에 정리해서 보여드릴게요.
        </p>
      </div>
    </div>
  );
}
