import { useState } from "react";
import ModalWrapper from "./ModalWrapper";

interface DiscountModalProps {
  currentPrice: number;
  onClose: () => void;
  onSubmit: (discountRate: number) => void;
}

export default function DiscountModal({ currentPrice, onClose, onSubmit }: DiscountModalProps) {
  const [rate, setRate] = useState(20);
  const discountedPrice = Math.round(currentPrice * (1 - rate / 100));

  return (
    <ModalWrapper onClose={onClose}>
      <div className="p-8 pb-4">
        <h2 className="flex items-center gap-2 text-2xl font-bold text-slate-800">
          <span>🏷️</span>
          할인 이벤트
        </h2>
        <p className="mt-1 text-sm text-slate-500">현재 판매가를 기준으로 할인율을 적용합니다.</p>
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
            max={50}
            step={5}
            value={rate}
            onChange={(event) => setRate(Number(event.target.value))}
            className="h-2 w-full cursor-pointer appearance-none rounded-lg bg-slate-100 accent-primary"
          />
          <div className="flex justify-between px-1 text-xs font-medium text-slate-400">
            <span>0%</span>
            <span>50%</span>
          </div>
        </div>

        <div className="space-y-3 rounded-2xl border border-slate-100 bg-slate-50 p-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-slate-600">
              <span className="material-symbols-outlined text-xl text-primary">local_offer</span>
              <span className="text-sm font-medium">할인 적용가</span>
            </div>
            <div className="text-right">
              <span className="mr-2 text-xs text-slate-400 line-through">₩{currentPrice.toLocaleString()}</span>
              <span className="text-lg font-bold text-slate-800">₩{discountedPrice.toLocaleString()}</span>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-center gap-2 text-xs text-slate-400">
          <span className="material-symbols-outlined text-[16px]">info</span>
          <span>1일 1회만 사용할 수 있습니다.</span>
        </div>
      </div>

      <div className="p-8 pt-2">
        <button
          type="button"
          onClick={() => onSubmit(rate)}
          disabled={rate === 0}
          className="w-full rounded-xl bg-primary py-3 text-base font-bold text-white shadow-md shadow-primary/25 transition-all active:scale-[0.98] hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-40"
        >
          할인 시작하기
        </button>
        <button
          type="button"
          onClick={onClose}
          className="mt-2.5 w-full py-2.5 text-sm font-medium text-slate-400 transition-colors hover:text-slate-600"
        >
          취소
        </button>
      </div>
    </ModalWrapper>
  );
}
