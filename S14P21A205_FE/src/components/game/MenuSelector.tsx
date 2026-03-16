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
    <div className="bg-white rounded-3xl p-8 shadow-soft border border-transparent">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h3 className="text-xl font-bold text-slate-900">오늘의 메뉴</h3>
          <p className="text-slate-500 text-sm mt-1">오늘 판매할 메뉴를 선택해주세요.</p>
        </div>
        <span className="material-symbols-outlined text-slate-300 text-3xl">restaurant_menu</span>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-5 gap-4">
        {menus.map((menu) => {
          const isSelected = menu.id === selectedId;
          return (
            <button
              key={menu.id}
              onClick={() => onSelect(menu.id)}
              className={`
                group relative flex flex-col items-center justify-center p-4 rounded-2xl transition-all
                aspect-square sm:aspect-auto sm:h-36
                ${isSelected
                  ? "bg-white border border-primary shadow-md ring-2 ring-primary/10"
                  : "bg-slate-50 border border-transparent hover:bg-slate-100"
                }
              `}
            >
              {isSelected && (
                <div className="absolute top-3 right-3 size-6 bg-primary rounded-full flex items-center justify-center text-white shadow-sm">
                  <span className="material-symbols-outlined text-[14px] font-bold">check</span>
                </div>
              )}
              <span
                className={`text-5xl mb-3 transition-all ${
                  isSelected ? "" : "filter grayscale opacity-80 group-hover:grayscale-0 group-hover:opacity-100"
                }`}
              >
                {menu.emoji}
              </span>
              <span className={`text-sm font-medium ${isSelected ? "font-bold text-slate-900" : "text-slate-600"}`}>
                {menu.name}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
