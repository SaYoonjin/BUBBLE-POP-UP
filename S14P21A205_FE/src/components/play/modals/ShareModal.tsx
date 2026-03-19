import { useState } from "react";
import ModalWrapper from "./ModalWrapper";

interface ShareModalProps {
  currentStock: number;
  onClose: () => void;
  onSubmit: (quantity: number) => void;
}

export default function ShareModal({
  currentStock,
  onClose,
  onSubmit,
}: ShareModalProps) {
  const [quantity, setQuantity] = useState<number | "">("");
  const isOutOfStock = currentStock <= 0;

  const handleChange = (value: string) => {
    if (value === "") {
      setQuantity("");
      return;
    }

    if (isOutOfStock) {
      setQuantity("");
      return;
    }

    const normalizedValue = Math.max(
      1,
      Math.min(currentStock, Number(value)),
    );

    setQuantity(normalizedValue);
  };

  return (
    <ModalWrapper onClose={onClose}>
      <div className="p-8 pb-6 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-3xl">
          🎁
        </div>
        <h2 className="mb-2 text-xl font-bold text-slate-800">나눔 이벤트</h2>
        <p className="text-sm leading-relaxed text-slate-500">
          재고를 일부 나누며 주변 고객에게 나눔을 진행합니다.
          <br />
          가게의 <span className="font-bold text-primary">호감도</span>가 올라갑니다.
        </p>
      </div>

      <div className="space-y-6 px-8">
        <div className="flex items-center justify-between rounded-xl border border-slate-100 bg-slate-50 p-3">
          <span className="text-sm font-medium text-slate-600">현재 재고</span>
          <span className="text-sm font-bold text-slate-800">{currentStock}개</span>
        </div>

        <div className="space-y-2">
          <label className="ml-1 block text-xs font-bold uppercase tracking-wider text-slate-500">
            나눔 수량
          </label>
          <div className="relative">
            <input
              type="number"
              value={quantity}
              onChange={(event) => handleChange(event.target.value)}
              placeholder={
                isOutOfStock ? "나눌 재고가 없습니다" : "수량을 입력하세요"
              }
              min={isOutOfStock ? 0 : 1}
              max={currentStock}
              disabled={isOutOfStock}
              className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-lg font-medium text-slate-800 transition-all placeholder:text-slate-300 focus:border-primary focus:ring-2 focus:ring-primary disabled:cursor-not-allowed disabled:bg-slate-50"
            />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-sm font-medium text-slate-400">
              개
            </span>
          </div>
          <div className="mt-1 flex items-center gap-1.5 text-rose-soft">
            <span className="material-symbols-outlined text-[16px]">info</span>
            <span className="text-xs font-medium">1개부터 현재 재고까지 선택할 수 있습니다.</span>
          </div>
        </div>
      </div>

      <div className="p-8 pt-6">
        <button
          type="button"
          onClick={() => quantity && onSubmit(Number(quantity))}
          disabled={isOutOfStock || !quantity || Number(quantity) <= 0}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-4 font-bold text-white shadow-lg shadow-primary/30 transition-all active:scale-[0.98] hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-40"
        >
          <span className="material-symbols-outlined text-[20px]">volunteer_activism</span>
          <span>나눔 시작하기</span>
        </button>
      </div>
    </ModalWrapper>
  );
}
