interface ActionBalanceSummaryProps {
  currentBalance: number;
  actionCost: number;
  costLabel?: string;
}

const formatWon = (amount: number) => {
  const prefix = amount < 0 ? "-" : "";
  return `${prefix}₩${Math.abs(amount).toLocaleString()}`;
};

export default function ActionBalanceSummary({
  currentBalance,
  actionCost,
  costLabel = "이번 액션 비용",
}: ActionBalanceSummaryProps) {
  const remainingBalance = currentBalance - actionCost;
  const isInsufficient = remainingBalance < 0;

  return (
    <div
      className={`rounded-2xl border p-4 ${
        isInsufficient
          ? "border-rose-200 bg-rose-50/70"
          : "border-slate-100 bg-slate-50/80"
      }`}
    >
      <div className="mb-3 flex items-center gap-2">
        <span
          className={`material-symbols-outlined text-[18px] ${
            isInsufficient ? "text-rose-500" : "text-primary-dark"
          }`}
        >
          account_balance_wallet
        </span>
        <span className="text-xs font-bold uppercase tracking-[0.18em] text-slate-600">
          잔액 요약
        </span>
      </div>

      <div className="space-y-2.5">
        <div className="flex items-center justify-between text-sm text-slate-600">
          <span>현재 잔액</span>
          <span className="font-medium text-slate-800">{formatWon(currentBalance)}</span>
        </div>
        <div className="flex items-center justify-between text-sm text-slate-600">
          <span>{costLabel}</span>
          <span className="font-medium text-slate-800">{formatWon(actionCost)}</span>
        </div>
        <div className="h-px w-full bg-slate-200" />
        <div className="flex items-center justify-between">
          <span className="text-sm font-bold text-slate-700">실행 후 잔액</span>
          <span
            className={`text-lg font-bold ${
              isInsufficient ? "text-rose-500" : "text-primary-dark"
            }`}
          >
            {formatWon(remainingBalance)}
          </span>
        </div>
      </div>

      {isInsufficient && (
        <p className="mt-3 text-xs font-medium text-rose-500">
          잔액이 부족해 이 액션은 실행할 수 없습니다.
        </p>
      )}
    </div>
  );
}
