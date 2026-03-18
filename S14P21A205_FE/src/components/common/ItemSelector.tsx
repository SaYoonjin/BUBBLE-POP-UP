interface ShopItem {
  id: number;
  name: string;
  group: string;
  discount: string;
  price: number;
}

interface ItemSelectorProps {
  items: ShopItem[];
  selectedIds: number[];
  onToggle: (id: number) => void;
  totalPoints: number;
}

export default function ItemSelector({ items, selectedIds, onToggle, totalPoints }: ItemSelectorProps) {
  const usedPoints = items.filter((i) => selectedIds.includes(i.id)).reduce((sum, i) => sum + i.price, 0);
  const remainingPoints = totalPoints - usedPoints;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between px-1">
        <h3 className="text-lg font-bold">아이템 선택</h3>
        <span className="text-sm text-slate-500">최대 2개</span>
      </div>

      <div className="grid grid-cols-1 gap-3">
        {items.map((item) => {
          const isSelected = selectedIds.includes(item.id);
          const sameGroupSelected = items.find(
            (other) => other.group === item.group && other.id !== item.id && selectedIds.includes(other.id)
          );
          const isDisabled = !isSelected && (!!sameGroupSelected || (selectedIds.length >= 2));
          const canAfford = remainingPoints >= item.price || isSelected;

          return (
            <div
              key={item.id}
              onClick={() => !isDisabled && canAfford && onToggle(item.id)}
              className={`bg-white rounded-[16px] p-5 relative transition-all ${
                isDisabled || !canAfford
                  ? "opacity-40 cursor-not-allowed border border-gray-100"
                  : isSelected
                    ? "border-2 border-primary shadow-md cursor-pointer"
                    : "border border-gray-100 hover:border-gray-200 hover:shadow-sm cursor-pointer"
              }`}
            >
              {isSelected && (
                <div className="absolute -top-2 -right-2 bg-primary text-white rounded-full p-1 shadow-sm">
                  <span className="material-symbols-outlined text-sm font-bold block">check</span>
                </div>
              )}

              <div className="flex items-center justify-between">
                <span className={`font-bold text-base ${isSelected ? "text-slate-900" : "text-slate-600"}`}>
                  {item.name}
                </span>
                <div className="flex items-center gap-2">
                  <span className={`text-sm font-mono font-bold ${isSelected ? "text-primary-dark" : "text-slate-400"}`}>
                    {item.discount}
                  </span>
                  <span className={`px-2.5 py-0.5 text-[11px] font-bold rounded-full ${
                    isSelected ? "bg-primary text-white" : "bg-gray-100 text-gray-500"
                  }`}>
                    {item.price}P
                  </span>
                </div>
              </div>

              {sameGroupSelected && !isSelected && (
                <p className="text-[11px] text-slate-400 mt-2">같은 종류의 다른 할인권이 선택되어 있습니다</p>
              )}
            </div>
          );
        })}
      </div>

      {/* Point summary */}
      <div className="bg-slate-50 rounded-xl px-4 py-3 flex items-center justify-between border border-slate-100">
        <span className="text-sm text-slate-500">사용 포인트</span>
        <div className="flex items-center gap-3">
          {usedPoints > 0 && (
            <span className="text-sm font-bold text-rose-soft">-{usedPoints}P</span>
          )}
          <span className={`text-lg font-bold font-mono ${remainingPoints < 0 ? "text-red-500" : "text-primary"}`}>
            {remainingPoints}P
          </span>
        </div>
      </div>
    </div>
  );
}
