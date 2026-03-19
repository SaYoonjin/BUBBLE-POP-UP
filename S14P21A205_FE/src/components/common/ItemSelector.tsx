import { useState } from "react";

interface ShopItem {
  id: number;
  name: string;
  group: string;
  desc: string;
  discount: string;
  price: number;
}

interface ItemGroup {
  group: string;
  label: string;
  icon: string;
  items: ShopItem[];
}

interface ItemSelectorProps {
  groups: ItemGroup[];
  selectedIds: number[];
  onToggle: (id: number) => void;
}

export default function ItemSelector({ groups, selectedIds, onToggle }: ItemSelectorProps) {
  const [showTooltip, setShowTooltip] = useState(false);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2 px-1">
        <h3 className="text-lg font-bold">아이템 선택</h3>
        <div className="relative">
          <button
            type="button"
            onClick={() => setShowTooltip((prev) => !prev)}
            className="size-7 rounded-full bg-slate-50 border border-slate-200 text-slate-400 hover:text-primary-dark hover:border-primary/20 hover:bg-primary/5 transition-colors flex items-center justify-center"
            aria-label="아이템 선택 안내 보기"
          >
            <span className="material-symbols-outlined text-[16px]">info</span>
          </button>
          {showTooltip && (
            <div className="absolute left-0 top-9 z-10 w-64 rounded-2xl border border-slate-200 bg-white px-3.5 py-3 text-[13px] leading-relaxed text-slate-600 shadow-lg">
              시즌 종료 시 순위에 따라 포인트를 획득하고, 이 포인트로 아이템을 구매할 수 있어요. 같은 종류는 하나만, 최대 2개까지 선택 가능해요.
            </div>
          )}
        </div>
      </div>

      <div className="flex flex-col gap-4">
        {groups.map((group) => {
          const selectedInGroup = group.items.find((i) => selectedIds.includes(i.id));

          return (
            <div key={group.group} className="bg-white rounded-[20px] shadow-soft p-5">
              {/* Group header */}
              <div className="flex items-center gap-2 mb-3">
                <span className="text-lg">{group.icon}</span>
                <span className="text-sm font-bold text-slate-700">{group.label}</span>
                {selectedInGroup && (
                  <span className="ml-auto text-xs font-bold text-primary bg-primary/10 px-2 py-0.5 rounded-full">
                    적용 중
                  </span>
                )}
              </div>

              {/* Items in group */}
              <div className="grid grid-cols-2 gap-2.5">
                {group.items.map((item) => {
                  const isSelected = selectedIds.includes(item.id);
                  const otherGroupCount = selectedIds.filter((sid) => {
                    return !group.items.some((gi) => gi.id === sid);
                  }).length;
                  const isDisabled = !isSelected && (!!selectedInGroup || otherGroupCount + (selectedInGroup ? 1 : 0) >= 2);

                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => !isDisabled && onToggle(item.id)}
                      className={`relative rounded-xl p-4 text-left transition-all ${
                        isDisabled
                          ? "opacity-35 cursor-not-allowed bg-slate-50 border border-slate-100"
                          : isSelected
                            ? "bg-primary/5 border-2 border-primary shadow-sm cursor-pointer"
                            : "bg-slate-50 border border-slate-100 hover:border-slate-200 hover:shadow-sm cursor-pointer"
                      }`}
                    >
                      {isSelected && (
                        <div className="absolute -top-1.5 -right-1.5 bg-primary text-white rounded-full p-0.5 shadow-sm">
                          <span className="material-symbols-outlined text-[14px] font-bold block">check</span>
                        </div>
                      )}
                      <div className="flex items-center justify-between mb-1.5">
                        <span className={`text-sm font-bold ${isSelected ? "text-primary-dark" : "text-slate-500"}`}>
                          {item.discount}
                        </span>
                        <span className={`text-[11px] font-bold px-2 py-0.5 rounded-full ${
                          isSelected ? "bg-primary text-white" : "bg-slate-200/70 text-slate-500"
                        }`}>
                          {item.price}P
                        </span>
                      </div>
                      <p className={`text-xs leading-relaxed ${isSelected ? "text-slate-600" : "text-slate-400"}`}>
                        {item.desc}
                      </p>
                    </button>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
