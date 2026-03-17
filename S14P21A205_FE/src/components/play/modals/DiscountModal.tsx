import { useState } from "react";
import ModalWrapper from "./ModalWrapper";

interface DiscountModalProps {
  originalPrice: number;
  onClose: () => void;
  onSubmit: (discountRate: number) => void;
}

export default function DiscountModal({ originalPrice, onClose, onSubmit }: DiscountModalProps) {
  const [rate, setRate] = useState(20);
  const discountedPrice = Math.round(originalPrice * (1 - rate / 100));
  const demandMultiplier = (1 + rate * 0.0175).toFixed(2);

  return (
    <ModalWrapper onClose={onClose}>
      <div className="p-8 pb-4">
        <h2 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
          <span>🏷️</span> 할인 이벤트
        </h2>
        <p className="text-slate-500 text-sm mt-1">할인을 통해 손님을 더 많이 끌어모을 수 있어요.</p>
      </div>

      <div className="px-8 py-4 space-y-8">
        {/* Slider */}
        <div className="space-y-4">
          <div className="flex justify-between items-end">
            <span className="text-sm font-bold text-slate-600">할인율 설정</span>
            <span className="text-3xl font-bold text-primary-dark">
              {rate}<span className="text-lg text-slate-400">%</span>
            </span>
          </div>
          <input
            type="range"
            min={0}
            max={50}
            step={5}
            value={rate}
            onChange={(e) => setRate(Number(e.target.value))}
            className="w-full h-2 bg-slate-100 rounded-lg appearance-none cursor-pointer accent-primary"
          />
          <div className="flex justify-between text-xs text-slate-400 font-medium px-1">
            <span>0%</span>
            <span>50%</span>
          </div>
        </div>

        {/* Stats */}
        <div className="bg-slate-50 rounded-2xl p-5 border border-slate-100 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-slate-600">
              <span className="material-symbols-outlined text-rose-soft text-xl">trending_up</span>
              <span className="text-sm font-medium">수요 증가율</span>
            </div>
            <span className="text-lg font-bold text-rose-soft">{demandMultiplier}x</span>
          </div>
          <div className="w-full h-px bg-slate-200" />
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-slate-600">
              <span className="material-symbols-outlined text-primary text-xl">sell</span>
              <span className="text-sm font-medium">할인 적용가</span>
            </div>
            <div className="text-right">
              <span className="text-xs text-slate-400 line-through mr-1">₩{originalPrice.toLocaleString()}</span>
              <span className="text-lg font-bold text-slate-800">₩{discountedPrice.toLocaleString()}</span>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-center gap-2 text-xs text-slate-400">
          <span className="material-symbols-outlined text-[16px]">info</span>
          <span>1일 1회 사용 가능</span>
        </div>
      </div>

      <div className="p-8 pt-2">
        <button
          onClick={() => onSubmit(rate)}
          disabled={rate === 0}
          className="w-full bg-primary hover:bg-primary-dark text-white font-bold text-lg py-4 rounded-xl shadow-lg shadow-primary/30 transition-all active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed"
        >
          할인 시작하기
        </button>
        <button onClick={onClose} className="w-full mt-3 py-3 text-slate-400 text-sm font-medium hover:text-slate-600 transition-colors">
          취소
        </button>
      </div>
    </ModalWrapper>
  );
}
