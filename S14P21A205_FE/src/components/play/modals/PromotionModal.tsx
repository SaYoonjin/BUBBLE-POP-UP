import { useState } from "react";
import ModalWrapper from "./ModalWrapper";

interface PromotionOption {
  id: string;
  icon: string;
  name: string;
  price: number;
}

const promotionOptions: PromotionOption[] = [
  { id: "influencer", icon: "🤳", name: "인플루언서", price: 50000 },
  { id: "sns", icon: "📱", name: "SNS 광고", price: 30000 },
  { id: "flyer", icon: "📰", name: "전단지 배포", price: 10000 },
  { id: "referral", icon: "🗣️", name: "지인 추천", price: 0 },
];

interface PromotionModalProps {
  onClose: () => void;
  onSubmit: (promotionId: string) => void;
}

export default function PromotionModal({ onClose, onSubmit }: PromotionModalProps) {
  const [selected, setSelected] = useState<string>("influencer");

  return (
    <ModalWrapper onClose={onClose}>
      <div className="px-8 pt-8 pb-4 text-center">
        <h3 className="text-2xl font-bold text-slate-800 flex items-center justify-center gap-2">
          <span className="text-3xl">📢</span> 홍보하기
        </h3>
        <p className="text-sm text-slate-500 mt-2">다양한 채널을 통해 가게를 알리세요.</p>
      </div>

      <div className="px-6 py-2">
        <div className="grid grid-cols-2 gap-3 mb-6">
          {promotionOptions.map((opt) => (
            <button
              key={opt.id}
              onClick={() => setSelected(opt.id)}
              className={`relative p-4 rounded-2xl border-2 text-left transition-all hover:shadow-md ${
                selected === opt.id
                  ? "border-primary bg-primary/5"
                  : "border-slate-200 bg-white hover:border-primary/50"
              }`}
            >
              {selected === opt.id && (
                <div className="absolute top-3 right-3 text-primary">
                  <span className="material-symbols-outlined text-[20px]">check_circle</span>
                </div>
              )}
              <div className="text-2xl mb-2">{opt.icon}</div>
              <div className="font-bold text-slate-800 text-sm">{opt.name}</div>
              <div className={`text-xs font-semibold mt-1 ${selected === opt.id ? "text-primary" : "text-slate-500"}`}>
                {opt.price > 0 ? `₩${opt.price.toLocaleString()}` : "무료"}
              </div>
            </button>
          ))}
        </div>

        <div className="bg-slate-50 rounded-xl p-4 mb-6 border border-slate-100">
          <div className="flex items-center gap-2 mb-2">
            <span className="material-symbols-outlined text-primary text-sm">auto_awesome</span>
            <span className="text-xs font-bold text-slate-700">기대 효과</span>
          </div>
          <p className="text-sm text-slate-600 leading-relaxed">
            평판 상승 및 <span className="text-primary font-bold">유동인구 보너스 +15%</span>
          </p>
        </div>
      </div>

      <div className="px-6 pb-8">
        <button
          onClick={() => onSubmit(selected)}
          className="w-full bg-primary hover:bg-primary-dark text-white font-bold py-4 rounded-xl shadow-lg shadow-primary/30 transition-all active:scale-[0.98] flex items-center justify-center gap-2"
        >
          <span>홍보 시작하기</span>
          <span className="material-symbols-outlined text-sm">arrow_forward</span>
        </button>
      </div>
    </ModalWrapper>
  );
}
