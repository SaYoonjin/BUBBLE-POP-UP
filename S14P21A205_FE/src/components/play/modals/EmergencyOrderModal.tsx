import { useState } from "react";
import ModalWrapper from "./ModalWrapper";

interface MenuItem {
  name: string;
  price: number;
}

interface EmergencyOrderModalProps {
  menuItems: MenuItem[];
  onClose: () => void;
  onSubmit: (menuIndex: number, quantity: number) => void;
}

export default function EmergencyOrderModal({ menuItems, onClose, onSubmit }: EmergencyOrderModalProps) {
  const [selectedMenu, setSelectedMenu] = useState(0);
  const [quantity, setQuantity] = useState(50);
  const surchargeRate = 0.5;

  const item = menuItems[selectedMenu];
  const baseCost = item.price * quantity;
  const surcharge = Math.round(baseCost * surchargeRate);
  const totalCost = baseCost + surcharge;

  return (
    <ModalWrapper onClose={onClose}>
      <div className="px-6 py-5 border-b border-slate-100 flex items-center justify-between pr-12">
        <div className="flex items-center gap-2">
          <span className="text-2xl">🚚</span>
          <h2 className="text-lg font-bold text-slate-800">긴급 발주</h2>
        </div>
      </div>

      <div className="p-6">
        {/* Warning */}
        <div className="bg-rose-50 border border-rose-100 rounded-xl p-3 mb-6 flex items-start gap-3">
          <span className="material-symbols-outlined text-rose-500 mt-0.5">info</span>
          <div>
            <p className="text-sm font-bold text-rose-700">긴급 발주 수수료 안내</p>
            <p className="text-xs text-rose-600 mt-0.5">원가의 {surchargeRate * 100}% 추가 비용 발생</p>
          </div>
        </div>

        <div className="space-y-4">
          {/* Menu Select */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider pl-1">Menu Item</label>
            <div className="relative">
              <select
                value={selectedMenu}
                onChange={(e) => setSelectedMenu(Number(e.target.value))}
                className="w-full bg-slate-50 border-slate-200 rounded-xl py-3 pl-4 pr-10 text-slate-700 font-medium focus:ring-2 focus:ring-primary/50 focus:border-primary transition-all appearance-none cursor-pointer"
              >
                {menuItems.map((m, i) => (
                  <option key={i} value={i}>{m.name} - ₩{m.price.toLocaleString()}</option>
                ))}
              </select>
              <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">
                <span className="material-symbols-outlined">expand_more</span>
              </div>
            </div>
          </div>

          {/* Quantity */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider pl-1">Quantity</label>
            <div className="flex items-center gap-3">
              <input
                type="number"
                value={quantity}
                onChange={(e) => setQuantity(Math.max(1, Number(e.target.value)))}
                className="flex-1 bg-slate-50 border-slate-200 rounded-xl py-3 px-4 text-slate-700 font-bold focus:ring-2 focus:ring-primary/50 focus:border-primary transition-all text-center"
              />
              <span className="text-sm font-medium text-slate-400">개</span>
            </div>
          </div>

          {/* Cost summary */}
          <div className="pt-4 border-t border-slate-100 mt-5">
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider pl-1 mb-2 block">결제 요약</label>
            <div className="bg-slate-50/50 rounded-xl p-4 border border-slate-100">
              <div className="flex justify-between items-center text-sm text-slate-600 mb-1.5">
                <span>원재료 단가(₩{item.price.toLocaleString()}) x 수량({quantity})</span>
                <span className="font-medium">₩{baseCost.toLocaleString()}</span>
              </div>
              <div className="flex justify-between items-center text-sm text-slate-600 mb-3">
                <span>+ 긴급 발주 수수료 ({surchargeRate * 100}%)</span>
                <span className="font-medium">₩{surcharge.toLocaleString()}</span>
              </div>
              <div className="h-px bg-slate-200 w-full mb-3" />
              <div className="flex justify-between items-center">
                <span className="text-sm font-bold text-slate-700">총 결제 금액</span>
                <span className="text-xl font-bold text-primary">₩{totalCost.toLocaleString()}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Transport info */}
        <div className="mt-6 grid grid-cols-2 gap-3">
          <div className="bg-slate-50 rounded-xl p-3 border border-slate-100 flex flex-col items-center text-center">
            <span className="text-xs text-slate-500 mb-1">현재 교통 상태</span>
            <span className="text-sm font-bold text-green-600 bg-green-100 px-2 py-0.5 rounded-md">NORMAL</span>
          </div>
          <div className="bg-slate-50 rounded-xl p-3 border border-slate-100 flex flex-col items-center text-center">
            <span className="text-xs text-slate-500 mb-1">예상 도착 시각</span>
            <div className="flex items-center gap-1 text-slate-800">
              <span className="material-symbols-outlined text-sm text-slate-400">schedule</span>
              <span className="font-bold">5분</span>
            </div>
          </div>
        </div>

        <button
          onClick={() => onSubmit(selectedMenu, quantity)}
          className="w-full mt-6 bg-primary hover:bg-primary-dark text-white font-bold py-3.5 rounded-xl shadow-md hover:shadow-lg transition-all active:scale-[0.98] flex items-center justify-center gap-2"
        >
          <span>발주하기</span>
          <span className="material-symbols-outlined text-sm">arrow_forward</span>
        </button>
      </div>
    </ModalWrapper>
  );
}
