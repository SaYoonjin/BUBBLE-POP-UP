interface ShopItem {
  id: number;
  name: string;
  desc: string;
  price: string;
}

interface ItemSelectorProps {
  items: ShopItem[];
  selectedIds: number[];
  onToggle: (id: number) => void;
}

export default function ItemSelector({ items, selectedIds, onToggle }: ItemSelectorProps) {
  return (
    <div className="flex flex-col gap-4">
      <h3 className="text-lg font-bold px-1">선택 가능한 아이템</h3>
      <div className="grid grid-cols-1 gap-3">
        {items.map((item) => {
          const isSelected = selectedIds.includes(item.id);
          return (
            <div
              key={item.id}
              onClick={() => onToggle(item.id)}
              className={`bg-white rounded-[16px] p-5 flex flex-col gap-2 relative cursor-pointer transition-all ${
                isSelected
                  ? "border-2 border-primary shadow-md"
                  : "border border-gray-100 opacity-60 hover:opacity-100 hover:border-gray-200 hover:shadow-sm"
              }`}
            >
              {isSelected && (
                <div className="absolute -top-2 -right-2 bg-primary text-white rounded-full p-1 shadow-sm">
                  <span className="material-symbols-outlined text-sm font-bold block">check</span>
                </div>
              )}
              <div className="flex justify-between items-center">
                <span className={`font-bold text-base ${isSelected ? "text-slate-900" : "text-[#5c5446]"}`}>{item.name}</span>
                <span className={`px-2 py-0.5 text-[11px] font-bold rounded-full ${
                  isSelected ? "bg-primary text-white" : "bg-gray-200 text-gray-500"
                }`}>{item.price}</span>
              </div>
              <p className={`text-xs font-medium leading-relaxed ${isSelected ? "text-gray-500" : "text-[#8c8273]"}`}>
                {item.desc}
              </p>
            </div>
          );
        })}
      </div>
    </div>
  );
}
