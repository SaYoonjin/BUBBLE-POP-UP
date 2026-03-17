interface ShopItem {
  id: number;
  name: string;
  desc: string;
  price: string;
}

interface CozyItemChecklistProps {
  items: ShopItem[];
  selectedIds: number[];
  onToggle: (id: number) => void;
  points: string;
}

export default function CozyItemChecklist({ items, selectedIds, onToggle, points }: CozyItemChecklistProps) {
  return (
    <div
      className="relative bg-cozy-paper p-8 -rotate-1 rounded-sm shadow-[0_8px_30px_-4px_rgba(0,0,0,0.2),0_4px_10px_-2px_rgba(0,0,0,0.1)]"
      style={{
        backgroundImage: "url('https://www.transparenttextures.com/patterns/cream-paper.png')",
        backgroundBlendMode: "multiply",
      }}
    >
      <div className="flex justify-between items-center mb-6 border-b-2 border-cozy-ink pb-2">
        <h3 className="font-cozy-hand text-3xl text-cozy-primary -rotate-1">선택 가능한 아이템</h3>
        <div className="text-right">
          <span className="block font-cozy-hand text-xs text-cozy-ink/40">MY POINTS</span>
          <span className="font-cozy-hand text-2xl text-cozy-sage">{points}</span>
        </div>
      </div>

      <div className="space-y-6">
        {items.map((item) => {
          const isSelected = selectedIds.includes(item.id);
          return (
            <div key={item.id} className="group cursor-pointer relative" onClick={() => onToggle(item.id)}>
              <div className={`flex gap-4 items-start ${isSelected ? "" : "opacity-80 hover:opacity-100"} transition-opacity`}>
                <div className={`mt-1 size-6 border-2 rounded-sm flex items-center justify-center ${
                  isSelected ? "border-cozy-primary bg-cozy-primary text-white" : "border-cozy-ink/20"
                }`}>
                  {isSelected && <span className="material-symbols-outlined text-sm font-bold">check</span>}
                </div>
                <div className="flex-1">
                  <div className="flex justify-between items-center mb-1">
                    <span className={`font-cozy-hand text-xl ${isSelected ? "text-cozy-ink" : "text-cozy-ink group-hover:text-cozy-primary"} transition-colors`}>
                      {item.name}
                    </span>
                    <span className={`font-cozy-hand text-lg ${isSelected ? "text-cozy-sage" : "text-cozy-ink/40 group-hover:text-cozy-sage"} transition-colors`}>
                      {item.price}
                    </span>
                  </div>
                  <p className="font-cozy-display text-sm text-cozy-ink/60 italic">{item.desc}</p>
                </div>
              </div>
              <div className="absolute -bottom-3 left-10 right-0 h-px border-b border-dashed border-cozy-ink/10" />
            </div>
          );
        })}
      </div>
    </div>
  );
}
