import axios from "axios";
import { useEffect, useMemo, useState } from "react";
import ActionBalanceSummary from "../../common/ActionBalanceSummary";
import ModalWrapper from "./ModalWrapper";

interface ApiErrorResponse {
  message?: string;
}

export interface PromotionOption {
  id: string;
  icon: string;
  name: string;
  price: number;
  multiplier: number;
}

interface PromotionModalProps {
  currentBalance: number;
  options: PromotionOption[];
  onClose: () => void;
  onSubmit: (payload: { promotionId: string; cost: number }) => Promise<void> | void;
}

function formatWon(amount: number) {
  return amount > 0 ? `₩${amount.toLocaleString()}` : "무료";
}

export default function PromotionModal({
  currentBalance,
  options,
  onClose,
  onSubmit,
}: PromotionModalProps) {
  const [selected, setSelected] = useState(options[0]?.id ?? "");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    setSelected((previous) =>
      options.some((option) => option.id === previous) ? previous : (options[0]?.id ?? ""),
    );
    setSubmitError(null);
  }, [options]);

  const selectedOption = useMemo(
    () => options.find((option) => option.id === selected) ?? options[0] ?? null,
    [options, selected],
  );
  const canAfford = selectedOption ? currentBalance >= selectedOption.price : false;

  const handleSubmit = async () => {
    if (!selectedOption || !canAfford || isSubmitting) {
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      await Promise.resolve(
        onSubmit({
          promotionId: selectedOption.id,
          cost: selectedOption.price,
        }),
      );
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setSubmitError(
          error.response?.data?.message ?? "홍보 실행에 실패했습니다. 잠시 후 다시 시도해주세요.",
        );
      } else {
        setSubmitError("홍보 실행에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

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
          <span className="text-3xl">📣</span>
          홍보하기
        </h3>
        <p className="mt-2 text-sm text-slate-500">
          홍보 채널별 비용을 비교해서 선택해보세요.
        </p>
      </div>

      <div className="space-y-4 px-6 py-2">
        {options.length === 0 ? (
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-center text-sm text-slate-500">
            홍보 옵션을 불러오지 못했습니다.
          </div>
        ) : (
          <div className="space-y-2">
            {options.map((option) => {
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

                  <div className="ml-4 flex shrink-0 items-center">
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
        )}

        <ActionBalanceSummary
          currentBalance={currentBalance}
          actionCost={selectedOption?.price ?? 0}
          costLabel="이번 홍보 비용"
        />

        {submitError && (
          <div className="rounded-2xl border border-red-200 bg-red-50/80 px-4 py-3 text-sm text-red-600">
            {submitError}
          </div>
        )}
      </div>

      <div className="px-6 pb-8 pt-2">
        <button
          type="button"
          onClick={() => void handleSubmit()}
          disabled={!selectedOption || !canAfford || isSubmitting}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-4 font-bold text-white shadow-lg shadow-primary/30 transition-all hover:bg-primary-dark active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40"
        >
          <span>{isSubmitting ? "홍보 실행중..." : "홍보 시작하기"}</span>
          <span className="material-symbols-outlined text-sm">arrow_forward</span>
        </button>
      </div>
    </ModalWrapper>
  );
}
