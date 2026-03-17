import { useState } from "react";
import ModalWrapper from "./ModalWrapper";

interface ShareModalProps {
  currentStock: number;
  onClose: () => void;
  onSubmit: (quantity: number) => void;
}

export default function ShareModal({ currentStock, onClose, onSubmit }: ShareModalProps) {
  const [quantity, setQuantity] = useState<number | "">("");

  return (
    <ModalWrapper onClose={onClose}>
      <div className="p-8 pb-6 text-center">
        <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4 text-3xl">
          🤲
        </div>
        <h2 className="text-xl font-bold text-slate-800 mb-2">나눔 이벤트</h2>
        <p className="text-slate-500 text-sm leading-relaxed">
          재고를 소모하여 주변 이웃들에게 나눔을 실천합니다.<br />
          가게의 <span className="text-primary font-bold">평판</span>이 올라갑니다.
        </p>
      </div>

      <div className="px-8 space-y-6">
        <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
          <span className="text-sm font-medium text-slate-600">현재 재고</span>
          <span className="text-sm font-bold text-slate-800">{currentStock}개</span>
        </div>

        <div className="space-y-2">
          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider ml-1">나눔 수량</label>
          <div className="relative">
            <input
              type="number"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value === "" ? "" : Math.max(1, Math.min(currentStock, Number(e.target.value))))}
              placeholder="수량을 입력하세요"
              className="w-full bg-white border border-slate-200 rounded-xl px-4 py-3 text-slate-800 focus:ring-2 focus:ring-primary focus:border-primary transition-all text-lg font-medium placeholder:text-slate-300"
            />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-sm text-slate-400 font-medium">개</span>
          </div>
          <div className="flex items-center gap-1.5 text-rose-soft mt-1">
            <span className="material-symbols-outlined text-[16px]">info</span>
            <span className="text-xs font-medium">1일 1회 제한</span>
          </div>
        </div>
      </div>

      <div className="p-8 pt-6">
        <button
          onClick={() => quantity && onSubmit(Number(quantity))}
          disabled={!quantity || Number(quantity) <= 0}
          className="w-full bg-primary hover:bg-primary-dark text-white font-bold py-4 rounded-xl shadow-lg shadow-primary/30 transition-all active:scale-[0.98] flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <span className="material-symbols-outlined text-[20px]">volunteer_activism</span>
          나눔 시작하기
        </button>
      </div>
    </ModalWrapper>
  );
}
