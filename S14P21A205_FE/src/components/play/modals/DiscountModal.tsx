import axios from "axios";
import { useEffect, useState } from "react";
import ModalWrapper from "./ModalWrapper";

interface ApiErrorResponse {
  message?: string;
}

interface DiscountModalProps {
  currentPrice: number;
  minimumPrice: number;
  onClose: () => void;
  onSubmit: (discountRate: number) => Promise<void> | void;
}

function formatWon(value: number) {
  return `₩${value.toLocaleString()}`;
}

function getMaxDiscountRate(currentPrice: number, minimumPrice: number) {
  if (currentPrice <= 0 || minimumPrice >= currentPrice) {
    return 0;
  }

  const rawRate = ((currentPrice - minimumPrice) / currentPrice) * 100;
  return Math.max(0, Math.floor(rawRate / 5) * 5);
}

export default function DiscountModal({
  currentPrice,
  minimumPrice,
  onClose,
  onSubmit,
}: DiscountModalProps) {
  const maxRate = getMaxDiscountRate(currentPrice, minimumPrice);
  const [rate, setRate] = useState(() => Math.min(20, maxRate));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const discountedPrice = Math.max(minimumPrice, Math.round(currentPrice * (1 - rate / 100)));

  useEffect(() => {
    setRate(Math.min(20, maxRate));
    setSubmitError(null);
  }, [maxRate, currentPrice, minimumPrice]);

  const handleSubmit = async () => {
    if (rate === 0 || isSubmitting || maxRate === 0) {
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      await Promise.resolve(onSubmit(rate));
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setSubmitError(error.response?.data?.message ?? "할인 적용에 실패했습니다. 잠시 후 다시 시도해주세요.");
      } else {
        setSubmitError("할인 적용에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ModalWrapper onClose={onClose}>
      <div className="p-8 pb-4">
        <h2 className="flex items-center gap-2 text-2xl font-bold text-slate-800">
          <span>🏷️</span>
          할인 이벤트
        </h2>
        <p className="mt-1 text-sm text-slate-500">
          현재 판매가를 기준으로 할인율을 적용합니다.
        </p>
      </div>

      <div className="space-y-7 px-8 py-4">
        <div className="space-y-4">
          <div className="flex items-end justify-between">
            <span className="text-sm font-bold text-slate-600">할인율 설정</span>
            <span className="text-3xl font-bold text-primary-dark">
              {rate}
              <span className="text-lg text-slate-400">%</span>
            </span>
          </div>
          <input
            type="range"
            min={0}
            max={maxRate}
            step={5}
            value={rate}
            onChange={(event) => setRate(Number(event.target.value))}
            className="h-2 w-full cursor-pointer appearance-none rounded-lg bg-slate-100 accent-primary"
          />
          <div className="flex justify-between px-1 text-xs font-medium text-slate-400">
            <span>0%</span>
            <span>최대 {maxRate}%</span>
          </div>
        </div>

        <div className="space-y-3 rounded-2xl border border-slate-100 bg-slate-50 p-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-slate-600">
              <span className="material-symbols-outlined text-xl text-primary">
                local_offer
              </span>
              <span className="text-sm font-medium">할인 적용가</span>
            </div>
            <div className="text-right">
              <span className="mr-2 text-xs text-slate-400 line-through">
                {formatWon(currentPrice)}
              </span>
              <span className="text-lg font-bold text-slate-800">
                {formatWon(discountedPrice)}
              </span>
            </div>
          </div>
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>최소 허용 판매가</span>
            <span>{formatWon(minimumPrice)}</span>
          </div>
        </div>

        {maxRate === 0 && (
          <div className="rounded-2xl border border-amber-200 bg-amber-50/80 px-4 py-3 text-sm text-amber-700">
            현재 판매가는 이미 최소 허용가에 가까워서 추가 할인을 적용할 수 없습니다.
          </div>
        )}

        {submitError && (
          <div className="rounded-2xl border border-red-200 bg-red-50/80 px-4 py-3 text-sm text-red-600">
            {submitError}
          </div>
        )}

        <div className="flex items-center justify-center gap-2 text-xs text-slate-400">
          <span className="material-symbols-outlined text-[16px]">info</span>
          <span>할인 액션은 하루에 한 번만 사용할 수 있습니다.</span>
        </div>
      </div>

      <div className="p-8 pt-2">
        <button
          type="button"
          onClick={() => void handleSubmit()}
          disabled={rate === 0 || isSubmitting || maxRate === 0}
          className="w-full rounded-xl bg-primary py-3 text-base font-bold text-white shadow-md shadow-primary/25 transition-all hover:bg-primary-dark active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40"
        >
          {isSubmitting ? "할인 적용중..." : "할인 시작하기"}
        </button>
      </div>
    </ModalWrapper>
  );
}
