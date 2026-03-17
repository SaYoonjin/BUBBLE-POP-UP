interface MenuItem {
  id: number;
  emoji: string;
  name: string;
}

interface MenuSelectorProps {
  menus: MenuItem[];
  selectedId: number | null;
  onSelect: (id: number) => void;
}

export default function MenuSelector({ menus, selectedId, onSelect }: MenuSelectorProps) {
  return (
    <div className="bg-white rounded-[1.5rem] p-6 md:p-7 shadow-soft border border-transparent">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-slate-900">오늘의 메뉴</h3>
          <p className="text-slate-500 text-sm mt-1">10개 메뉴 중 오늘의 대표 메뉴를 골라 가격과 수량을 준비하세요.</p>
        </div>
        <span className="material-symbols-outlined text-slate-300 text-[28px]">restaurant_menu</span>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        {menus.map((menu) => {
          const isSelected = menu.id === selectedId;
          return (
            <button
              key={menu.id}
              onClick={() => onSelect(menu.id)}
              className={`
                group relative flex flex-col items-center justify-center p-3 rounded-2xl transition-all
                aspect-square md:aspect-auto md:h-28
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
            </button>
          );
        })}
      </div>
    </div>
  );
}
