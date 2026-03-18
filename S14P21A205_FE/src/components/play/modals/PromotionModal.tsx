import { useMemo, useState } from "react";
import ActionBalanceSummary from "../../common/ActionBalanceSummary";
import ModalWrapper from "./ModalWrapper";

interface PromotionOption {
  id: string;
  icon: string;
  name: string;
  price: number;
  multiplier: number;
}

interface PromotionModalProps {
  currentBalance: number;
  onClose: () => void;
  onSubmit: (payload: { promotionId: string; cost: number }) => void;
}

const promotionOptions: PromotionOption[] = [
  { id: "influencer", icon: "📣", name: "인플루언서 홍보", price: 50_000, multiplier: 1.2 },
  { id: "sns", icon: "📱", name: "SNS 홍보", price: 30_000, multiplier: 1.15 },
  { id: "flyer", icon: "📰", name: "전단지 배포", price: 10_000, multiplier: 1.1 },
  { id: "referral", icon: "🤝", name: "지인 추천", price: 0, multiplier: 1.05 },
];

function formatWon(amount: number) {
  return amount > 0 ? `₩${amount.toLocaleString()}` : "무료";
}

export default function PromotionModal({
  currentBalance,
  onClose,
  onSubmit,
}: PromotionModalProps) {
  const [selected, setSelected] = useState<string>("influencer");
  const selectedOption = useMemo(
    () => promotionOptions.find((option) => option.id === selected) ?? promotionOptions[0],
    [selected],
  );
  const canAfford = currentBalance >= selectedOption.price;

  return (
    <ModalWrapper onClose={onClose}>
      <style>{`
        @keyframes promotionSelect {
          0% {
            transform: translateY(4px) scale(0.985);
            opacity: 0.9;
          }
          100% {
            transform: translateY(0) scale(1);
            opacity: 1;
          }
        }

        @keyframes promotionCheck {
          0% {
            transform: scale(0.7);
            opacity: 0;
          }
          100% {
            transform: scale(1);
            opacity: 1;
          }
        }
      `}</style>
      <div className="px-8 pb-4 pt-8 text-center">
        <h3 className="flex items-center justify-center gap-2 text-2xl font-bold text-slate-800">
          <span className="text-3xl">📢</span>
          홍보하기
        </h3>
        <p className="mt-2 text-sm text-slate-500">
          홍보 채널별 비용과 유입 효과를 비교해서 선택해보세요.
        </p>
      </div>

      <div className="space-y-4 px-6 py-2">
        <div className="space-y-2">
          {promotionOptions.map((option) => {
            const isSelected = selected === option.id;

            return (
              <button
                key={option.id}
                type="button"
                onClick={() => setSelected(option.id)}
                className={`relative flex w-full items-center justify-between rounded-2xl border px-4 py-4 text-left transition-all ${
                  isSelected
                    ? "border-primary bg-primary/5 shadow-md ring-1 ring-primary/10"
                    : "border-slate-200 bg-white hover:border-primary/40 hover:bg-slate-50"
                }`}
                style={
                  isSelected
                    ? { animation: "promotionSelect 0.22s ease-out" }
                    : undefined
                }
              >
                <div className="flex min-w-0 items-center gap-3">
                  <div
                    className={`flex size-12 shrink-0 items-center justify-center rounded-2xl text-2xl ${
                      isSelected ? "bg-white shadow-sm" : "bg-slate-50"
                    }`}
                  >
                    {option.icon}
                  </div>
                  <div className="min-w-0">
                    <p
                      className={`truncate text-sm ${
                        isSelected ? "font-bold text-slate-900" : "font-medium text-slate-700"
                      }`}
                    >
                      {option.name}
                    </p>
                    <p className="mt-1 text-xs text-slate-400">
                      {formatWon(option.price)}
                    </p>
                  </div>
                </div>

                <div className="ml-4 flex shrink-0 items-center gap-3">
                  <div className="text-right">
                    <p className="text-[11px] font-medium text-slate-400">유입 배수</p>
                    <p
                      className={`mt-1 text-sm font-bold ${
                        isSelected ? "text-primary-dark" : "text-slate-700"
                      }`}
                    >
                      x{option.multiplier.toFixed(2)}
                    </p>
                  </div>
                  {isSelected && (
                    <span
                      className="material-symbols-outlined text-[20px] text-primary"
                      style={{ animation: "promotionCheck 0.2s ease-out" }}
                    >
                      check_circle
                    </span>
                  )}
                </div>
              </button>
            );
          })}
        </div>

        <ActionBalanceSummary
          currentBalance={currentBalance}
          actionCost={selectedOption.price}
          costLabel="이번 홍보 비용"
        />
      </div>

      <div className="px-6 pb-8 pt-2">
        <button
          type="button"
          onClick={() =>
            onSubmit({
              promotionId: selected,
              cost: selectedOption.price,
            })
          }
          disabled={!canAfford}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-4 font-bold text-white shadow-lg shadow-primary/30 transition-all active:scale-[0.98] hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-40"
        >
          <span>홍보 시작하기</span>
          <span className="material-symbols-outlined text-sm">arrow_forward</span>
        </button>
      </div>
    </ModalWrapper>
  );
}
