interface MenuItem {
  id: number;
  emoji: string;
  name: string;
  tag?: string | null;
}

interface CozyMenuSelectorProps {
  menus: MenuItem[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  dayLabel?: string;
}

export default function CozyMenuSelector({ menus, selectedId, onSelect, dayLabel }: CozyMenuSelectorProps) {
  return (
    <div className="bg-cozy-cream rounded-xl p-6 shadow-cozy-float border-4 border-white/50 relative">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold text-[#5D4037] flex items-center gap-2">
          <span className="material-symbols-outlined text-cozy-sage-green">bakery_dining</span>
          오늘의 메뉴 선택
        </h2>
        {dayLabel && (
          <span className="bg-cozy-sage-green/20 text-[#556B2F] px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider">
            {dayLabel}
          </span>
        )}
      </div>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-6">
        {menus.map((menu) => {
          const isSelected = menu.id === selectedId;
          return (
            <div key={menu.id} className="group cursor-pointer relative" onClick={() => onSelect(menu.id)}>
              {menu.tag && (
                <div className="absolute -top-3 -right-3 z-20 bg-cozy-primary text-white text-[10px] font-bold px-2 py-1 rounded-full shadow-lg rotate-12">
                  {menu.tag}
                </div>
              )}
              <div className={`
                bg-white rounded-2xl p-5 flex flex-col items-center gap-3 transition-all
                ${isSelected
                  ? "shadow-[8px_8px_16px_#d1c4b3,-8px_-8px_16px_#ffffff] border-4 border-cozy-sage-green ring-4 ring-cozy-sage-green/20"
                  : "shadow-[8px_8px_16px_#d1c4b3,-8px_-8px_16px_#ffffff] hover:-translate-y-1"
                }
              `}>
                <div className={`
                  w-full aspect-square rounded-xl flex items-center justify-center text-6xl transition-colors
                  ${isSelected ? "bg-[#fff8e1]" : "bg-slate-50 group-hover:bg-slate-100"}
                `}>
                  <span className={`transition-all ${isSelected ? "" : "filter grayscale opacity-70 group-hover:grayscale-0 group-hover:opacity-100"}`}>
                    {menu.emoji}
                  </span>
                </div>
                <span className={`text-sm ${isSelected ? "font-bold text-[#5D4037]" : "font-medium text-[#5D4037]/70"}`}>
                  {menu.name}
                </span>
              </div>
            </div>
          );
        })}

        <div className="group cursor-pointer opacity-60 hover:opacity-100 transition-opacity">
          <div className="bg-slate-50 rounded-2xl p-5 flex flex-col items-center gap-3 border-2 border-dashed border-slate-300 h-full justify-center">
            <span className="material-symbols-outlined text-5xl text-slate-400 group-hover:text-cozy-primary transition-colors">add_circle</span>
            <span className="font-bold text-lg text-slate-400">Add New</span>
          </div>
        </div>
      </div>
    </div>
  );
}
