import { useState } from "react";
import ModalWrapper from "./ModalWrapper";
import {
  applyDiscount,
  getSelectedDiscountMultiplier,
  getStoredSelectedDashboardItems,
} from "../../../utils/dashboardItems";

interface MenuItem {
  name: string;
  price: number;
  emoji: string;
}

export type DeliveryTrafficLevel =
  | "very_relaxed"
  | "relaxed"
  | "normal"
  | "crowded"
  | "very_crowded";

interface EmergencyOrderModalProps {
  currentBalance: number;
  menuItems: MenuItem[];
  currentMenuName: string;
  deliveryTraffic: DeliveryTrafficLevel;
  onClose: () => void;
  onSubmit: (payload: {
    menuIndex: number;
    quantity: number;
    totalCost: number;
  }) => void;
}

const SURCHARGE_RATE = 0.5;

const deliveryTrafficMap: Record<
  DeliveryTrafficLevel,
  { label: string; color: string; etaMinutes: number; iconBg: string }
> = {
  very_relaxed: {
    label: "매우 원활",
    color: "text-sky-500",
    etaMinutes: 8,
    iconBg: "bg-sky-50",
  },
  relaxed: {
    label: "원활",
    color: "text-emerald-500",
    etaMinutes: 12,
    iconBg: "bg-emerald-50",
  },
  normal: {
    label: "보통",
    color: "text-slate-600",
    etaMinutes: 18,
    iconBg: "bg-slate-100",
  },
  crowded: {
    label: "혼잡",
    color: "text-amber-500",
    etaMinutes: 26,
    iconBg: "bg-amber-50",
  },
  very_crowded: {
    label: "매우 혼잡",
    color: "text-rose-500",
    etaMinutes: 35,
    iconBg: "bg-rose-50",
  },
};

function formatWon(amount: number) {
  return `₩${amount.toLocaleString()}`;
}

function formatExpenseWon(amount: number) {
  return `- ${formatWon(amount)}`;
}

export default function EmergencyOrderModal({
  currentBalance,
  menuItems,
  currentMenuName,
  deliveryTraffic,
  onClose,
  onSubmit,
}: EmergencyOrderModalProps) {
  const initialMenuIndex = Math.max(
    0,
    menuItems.findIndex((menu) => menu.name === currentMenuName),
  );
  const [selectedMenuIndex, setSelectedMenuIndex] = useState(initialMenuIndex);
  const [selectedDashboardItems] = useState(
    getStoredSelectedDashboardItems,
  );
  const [quantity, setQuantity] = useState(120);
  const [showWarningTooltip, setShowWarningTooltip] = useState(false);

  const selectedMenu = menuItems[selectedMenuIndex];
  const ingredientDiscountMultiplier = getSelectedDiscountMultiplier(
    selectedDashboardItems,
    "INGREDIENT",
  );
  const hasItemDiscount = ingredientDiscountMultiplier < 1;
  const discountedUnitPrice = applyDiscount(
    selectedMenu.price,
    ingredientDiscountMultiplier,
  );
  const materialsCost = discountedUnitPrice * quantity;
  const surcharge = Math.round(materialsCost * SURCHARGE_RATE);
  const totalCost = materialsCost + surcharge;
  const remainingBalance = currentBalance - totalCost;
  const canAfford = currentBalance >= totalCost;
  const trafficInfo = deliveryTrafficMap[deliveryTraffic];

  return (
    <ModalWrapper onClose={onClose}>
      <div className="border-b border-slate-100 px-6 py-5 pr-12">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-2">
            <span className="text-2xl">🚚</span>
            <div>
              <h2 className="text-lg font-bold text-slate-800">긴급 발주</h2>
              <p className="mt-1 text-sm text-slate-500">
                필요한 메뉴를 바로 추가 주문할 수 있습니다.
              </p>
            </div>
          </div>

          <div className="relative shrink-0">
            <button
              type="button"
              onClick={() => setShowWarningTooltip((prev) => !prev)}
              className="flex size-8 items-center justify-center rounded-full border border-rose-200 bg-rose-50 text-rose-600 transition-colors hover:bg-rose-100"
              aria-label="긴급 발주 수수료 안내 보기"
            >
              <span className="material-symbols-outlined text-[16px]">info</span>
            </button>
            {showWarningTooltip && (
              <div className="absolute right-0 top-[calc(100%+0.5rem)] z-10 w-64 rounded-2xl border border-rose-200 bg-white px-3.5 py-3 text-xs leading-relaxed text-slate-600 shadow-lg">
                긴급 발주에는 기본 재료비 외에 50% 수수료가 추가되므로 일반 발주보다 비용이 높아 필요한 수량만 주문하는 편이 좋습니다.
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="space-y-6 p-6">
        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-slate-800">메뉴 선택</h3>
              <p className="mt-1 text-xs text-slate-400">
                긴급 주문할 메뉴를 골라주세요.
              </p>
            </div>
            <span className="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-semibold text-slate-500">
              총 {menuItems.length}개
            </span>
          </div>

          <div className="max-h-72 space-y-2 overflow-y-auto rounded-2xl border border-slate-100 bg-white p-2 custom-scrollbar">
            {menuItems.map((menu, index) => {
              const isSelected = selectedMenuIndex === index;
              const isCurrentMenu = menu.name === currentMenuName;
              const listDiscountedPrice = applyDiscount(
                menu.price,
                ingredientDiscountMultiplier,
              );
              const isSelectedNewMenu = isSelected && !isCurrentMenu;

              return (
                <button
                  key={`${menu.name}-${index}`}
                  type="button"
                  onClick={() => setSelectedMenuIndex(index)}
                  className={`flex w-full items-center justify-between rounded-xl border px-3.5 py-3 text-left transition-all ${
                    isSelected
                      ? "border-primary bg-primary/5 shadow-sm ring-1 ring-primary/10"
                      : "border-transparent bg-slate-50 hover:border-slate-200 hover:bg-white"
                  }`}
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <div
                      className={`flex size-11 shrink-0 items-center justify-center rounded-2xl text-2xl ${
                        isSelected ? "bg-white" : "bg-white/80"
                      }`}
                    >
                      {menu.emoji}
                    </div>
                    <div className="min-w-0">
                      <p
                        className={`truncate text-sm ${
                          isSelected
                            ? "font-bold text-slate-900"
                            : "font-medium text-slate-700"
                        }`}
                      >
                        {menu.name}
                      </p>
                      <div className="mt-0.5 flex items-center gap-1.5 text-[11px]">
                        {hasItemDiscount && (
                          <span className="text-rose-300 line-through decoration-2">
                            {formatWon(menu.price)}
                          </span>
                        )}
                        <span
                          className={
                            hasItemDiscount
                              ? "font-semibold text-rose-500"
                              : "text-slate-400"
                          }
                        >
                          {formatWon(listDiscountedPrice)}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="ml-3 flex shrink-0 items-center gap-2">
                    {isCurrentMenu && (
                      <span className="rounded-full bg-primary/15 px-2 py-1 text-[10px] font-bold text-primary-dark">
                        현재 판매 중
                      </span>
                    )}
                    {isSelectedNewMenu && (
                      <span className="rounded-full bg-accent-rose/15 px-2 py-1 text-[10px] font-bold text-rose-dark">
                        새 메뉴
                      </span>
                    )}
                    {isSelected && (
                      <span className="material-symbols-outlined text-[18px] text-primary">
                        check_circle
                      </span>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        </section>

        <section className="space-y-4 rounded-2xl border border-slate-100 bg-white p-5 shadow-soft">
          <div>
            <h3 className="text-sm font-bold text-slate-800">수량 설정</h3>
            <p className="mt-1 text-xs text-slate-400">
              50개부터 500개까지 조정할 수 있습니다.
            </p>
          </div>

          <div className="py-1 text-center">
            <p className="mb-2 text-[13px] font-medium text-slate-400">주문 수량</p>
            <p className="text-[2.4rem] font-black tracking-tight text-slate-900">
              {quantity}개
            </p>
          </div>

          <div className="px-1">
            <input
              type="range"
              min={50}
              max={500}
              step={10}
              value={quantity}
              onChange={(event) => setQuantity(Number(event.target.value))}
              className="h-2 w-full cursor-pointer appearance-none rounded-lg bg-slate-100 accent-primary transition-all"
            />
            <div className="mt-2.5 flex justify-between text-xs font-medium text-slate-400">
              <span>50개</span>
              <span>500개</span>
            </div>
          </div>
        </section>

        <section className="space-y-3 rounded-2xl border border-slate-100 bg-white p-5 shadow-soft">
          <div>
            <h3 className="text-sm font-bold text-slate-800">배송 정보</h3>
            <p className="mt-1 text-xs text-slate-400">
              현재 교통 상황을 기준으로 예상 도착 시간을 안내합니다.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="flex min-h-[144px] flex-col items-center justify-center rounded-2xl border border-slate-100 bg-slate-50/80 p-4 text-center">
              <div className="mb-3 flex items-center justify-center gap-2">
                <div
                  className={`flex size-8 items-center justify-center rounded-full ${trafficInfo.iconBg}`}
                >
                  <span className="material-symbols-outlined text-[16px] text-slate-500">
                    traffic
                  </span>
                </div>
                <span className="text-xs font-bold uppercase tracking-[0.18em] text-slate-500">
                  현재 교통량
                </span>
              </div>
              <p className={`text-[1.65rem] font-black tracking-tight ${trafficInfo.color}`}>
                {trafficInfo.label}
              </p>
            </div>

            <div className="flex min-h-[144px] flex-col items-center justify-center rounded-2xl border border-slate-100 bg-slate-50/80 p-4 text-center">
              <div className="mb-3 flex items-center justify-center gap-2">
                <div className="flex size-8 items-center justify-center rounded-full bg-primary/10">
                  <span className="material-symbols-outlined text-[16px] text-primary-dark">
                    schedule
                  </span>
                </div>
                <span className="shrink-0 whitespace-nowrap text-[11px] font-bold tracking-[0.1em] text-slate-500">
                  예상 도착시간
                </span>
              </div>
              <p className="whitespace-nowrap text-[1.65rem] font-black tracking-tight text-slate-800">
                약 {trafficInfo.etaMinutes}분
              </p>
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-slate-100 bg-slate-50/70 p-4">
          <p className="mb-3 text-xs font-bold uppercase tracking-[0.18em] text-slate-500">
            결제 요약
          </p>
          <div className="space-y-2.5">
            <div className="flex items-center justify-between text-sm text-slate-600">
              <span>현재 잔액</span>
              <span className="font-medium text-slate-800">
                {formatWon(currentBalance)}
              </span>
            </div>
            <div className="flex items-center justify-between text-sm text-slate-600">
              <span className="text-rose-400">원가 x 수량</span>
              <span className="font-semibold text-rose-500">
                {formatExpenseWon(materialsCost)}
              </span>
            </div>
            <div className="flex items-center justify-between text-sm text-slate-600">
              <span className="text-rose-400">
                긴급 발주 수수료 ({SURCHARGE_RATE * 100}%)
              </span>
              <span className="font-semibold text-rose-500">
                {formatExpenseWon(surcharge)}
              </span>
            </div>
            <div className="h-px w-full bg-slate-200" />
            <div className="flex items-center justify-between">
              <span className="text-sm font-bold text-slate-700">총 결제 비용</span>
              <span className="text-xl font-black text-slate-900">
                {formatWon(totalCost)}
              </span>
            </div>
            <div className="h-px w-full bg-slate-200" />
            <div className="flex items-center justify-between">
              <span className="text-sm font-bold text-slate-700">결제 후 잔액</span>
              <span
                className={`text-lg font-bold ${
                  remainingBalance < 0 ? "text-rose-500" : "text-primary-dark"
                }`}
              >
                {formatWon(remainingBalance)}
              </span>
            </div>
          </div>

          {!canAfford && (
            <p className="mt-3 text-xs font-medium text-rose-500">
              잔액이 부족해 긴급 발주를 실행할 수 없습니다.
            </p>
          )}
        </section>

        <button
          type="button"
          onClick={() =>
            onSubmit({
              menuIndex: selectedMenuIndex,
              quantity,
              totalCost,
            })
          }
          disabled={!canAfford}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-3.5 font-bold text-white shadow-md transition-all hover:bg-primary-dark hover:shadow-lg active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40"
        >
          <span>발주하기</span>
          <span className="material-symbols-outlined text-sm">arrow_forward</span>
        </button>
      </div>
    </ModalWrapper>
  );
}
