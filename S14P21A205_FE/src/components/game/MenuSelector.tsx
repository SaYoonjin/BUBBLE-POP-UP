import { useState } from "react";

interface MenuItem {
  id: number;
  emoji: string;
  name: string;
  isCurrentSellingMenu?: boolean;
  isSelectedNewMenu?: boolean;
}

interface MenuSelectorProps {
  menus: MenuItem[];
  selectedId: number | null;
  onSelect: (id: number) => void;
}

export default function MenuSelector({ menus, selectedId, onSelect }: MenuSelectorProps) {
  const [showTooltip, setShowTooltip] = useState(false);

  return (
    <div className="bg-white rounded-[1.5rem] p-6 md:p-7 shadow-soft border border-transparent">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-slate-900">오늘의 메뉴</h3>
          <p className="text-slate-500 text-sm mt-1">10개 메뉴 중 오늘의 대표 메뉴를 골라 가격과 수량을 준비하세요.</p>
        </div>
        <div className="relative">
          <button
            type="button"
            onClick={() => setShowTooltip((prev) => !prev)}
            className="size-9 rounded-full bg-slate-50 border border-slate-200 text-slate-400 hover:text-primary-dark hover:border-primary/20 hover:bg-primary/5 transition-colors flex items-center justify-center"
            aria-label="오늘의 메뉴 안내 보기"
          >
            <span className="material-symbols-outlined text-[20px]">restaurant_menu</span>
          </button>
          {showTooltip && (
            <div className="absolute right-0 top-11 z-10 w-60 rounded-2xl border border-slate-200 bg-white px-3.5 py-3 text-[13px] leading-relaxed text-slate-600 shadow-lg">
              오늘은 대표 메뉴 1개만 선택할 수 있습니다. 선택한 메뉴를 기준으로 판매 가격과 준비 수량이 설정됩니다.
            </div>
          )}
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        {menus.map((menu) => {
          const isSelected = menu.id === selectedId;
          const statusBadge = menu.isCurrentSellingMenu ? (
            <span className="rounded-full bg-primary/15 px-2 py-1 text-[10px] font-bold text-primary-dark">
              현재 판매 중
            </span>
          ) : menu.isSelectedNewMenu ? (
            <span className="rounded-full bg-accent-rose/15 px-2 py-1 text-[10px] font-bold text-rose-dark">
              새 메뉴
            </span>
          ) : null;
          const shouldUseTopLayout = Boolean(statusBadge) && (isSelected || menu.isCurrentSellingMenu);

          return (
            <button
              key={menu.id}
              type="button"
              onClick={() => onSelect(menu.id)}
              className={`
                group relative flex flex-col items-center rounded-2xl transition-all
                aspect-square md:aspect-auto md:h-32
                ${shouldUseTopLayout ? "justify-start px-3 pt-5 pb-4" : "justify-center p-3"}
                ${isSelected
                  ? "bg-white border border-primary shadow-md ring-2 ring-primary/10"
                  : "bg-slate-50 border border-transparent hover:bg-slate-100"
                }
              `}
              >
              {isSelected && (
                <div className="absolute top-2.5 right-2.5 size-[22px] bg-primary rounded-full flex items-center justify-center text-white shadow-sm">
                  <span className="material-symbols-outlined text-[13px] font-bold">check</span>
                </div>
              )}
              <div className="flex flex-col items-center">
                <span
                  className={`text-3xl md:text-[2.5rem] mb-2.5 transition-all ${
                    isSelected ? "" : "filter grayscale opacity-80 group-hover:grayscale-0 group-hover:opacity-100"
                  }`}
                >
                  {menu.emoji}
                </span>
                <span className={`text-sm md:text-[14px] font-medium text-center leading-tight ${isSelected ? "font-bold text-slate-900" : "text-slate-600"}`}>
                  {menu.name}
                </span>
                {shouldUseTopLayout && statusBadge && (
                  <div className="mt-2 flex min-h-5 flex-wrap items-center justify-center gap-1.5">
                    {statusBadge}
                  </div>
                )}
              </div>
              {!shouldUseTopLayout && statusBadge && (
                <div className="pointer-events-none absolute bottom-5 left-1/2 -translate-x-1/2">
                  {statusBadge}
                </div>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
