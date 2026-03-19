import { useEffect, useState } from "react";
import AppHeader from "../components/common/AppHeader";
import FloatingBubbles from "../components/common/FloatingBubbles";
import ItemSelector from "../components/common/ItemSelector";
import SeasonCTA from "../components/common/SeasonCTA";
import BankruptWarning from "../components/common/BankruptWarning";
import AnimatedNumber from "../components/common/AnimatedNumber";
import { DASHBOARD_SELECTED_ITEMS_STORAGE_KEY } from "../constants";

const dashBubbles = [
  { size: "w-64 h-64", position: "top-[5%] left-[-5%]", opacity: "opacity-40", delay: "0s", variant: "glass" as const },
  { size: "w-48 h-48", position: "bottom-10 right-[-2%]", opacity: "opacity-30", delay: "2s", variant: "glass" as const },
  { size: "w-24 h-24", position: "top-1/3 left-[40%]", opacity: "opacity-20", delay: "4s", variant: "glass" as const },
];

const TOTAL_POINTS = 100;

const itemGroups = [
  {
    group: "ingredient",
    label: "원재료 할인권",
    icon: "🥬",
    items: [
      { id: 1, name: "원재료 할인권", group: "ingredient", discount: "20% 할인", desc: "원재료 구매 비용 20% 감소", price: 30 },
      { id: 3, name: "원재료 할인권", group: "ingredient", discount: "5% 할인", desc: "원재료 구매 비용 5% 감소", price: 10 },
    ],
  },
  {
    group: "rent",
    label: "임대료 할인권",
    icon: "🏠",
    items: [
      { id: 2, name: "임대료 할인권", group: "rent", discount: "20% 할인", desc: "일일 임대료 20% 감소", price: 30 },
      { id: 4, name: "임대료 할인권", group: "rent", discount: "5% 할인", desc: "일일 임대료 5% 감소", price: 10 },
    ],
  },
];

const allItems = itemGroups.flatMap((g) => g.items);

function getInitialSelectedItemIds() {
  try {
    const stored = localStorage.getItem(DASHBOARD_SELECTED_ITEMS_STORAGE_KEY);
    if (!stored) return [];
    const parsed = JSON.parse(stored);
    return Array.isArray(parsed) ? parsed.filter((value): value is number => typeof value === "number") : [];
  } catch {
    return [];
  }
}

export default function DashboardPage() {
  const [selectedItemIds, setSelectedItemIds] = useState<number[]>(getInitialSelectedItemIds);
  const [deadline] = useState(() => {
    const d = new Date();
    d.setHours(23, 59, 59, 0);
    return d;
  });

  const handleToggle = (id: number) => {
    setSelectedItemIds((prev) => {
      if (prev.includes(id)) {
        return prev.filter((i) => i !== id);
      }
      const item = allItems.find((i) => i.id === id);
      if (!item) return prev;
      const filtered = prev.filter((prevId) => {
        const prevItem = allItems.find((i) => i.id === prevId);
        return prevItem?.group !== item.group;
      });
      if (filtered.length >= 2) return prev;
      return [...filtered, id];
    });
  };

  useEffect(() => {
    localStorage.setItem(DASHBOARD_SELECTED_ITEMS_STORAGE_KEY, JSON.stringify(selectedItemIds));
  }, [selectedItemIds]);

  const usedPoints = allItems
    .filter((i) => selectedItemIds.includes(i.id))
    .reduce((sum, i) => sum + i.price, 0);

  return (
    <div className="relative min-h-screen w-full flex flex-col bg-[#FDFDFB] text-slate-900 overflow-x-hidden font-display">
      <FloatingBubbles bubbles={dashBubbles} />
      <AppHeader />

      <main className="flex-1 flex flex-col items-center w-full px-6 md:px-12 pt-24 pb-12 z-10 max-w-[1100px] mx-auto">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 w-full items-start">
          {/* Left */}
          <div className="flex flex-col gap-6 lg:w-[95%]">
            {/* Points card */}
            <div className="bg-white rounded-[20px] shadow-soft p-6 w-full flex flex-col gap-2">
              <span className="text-sm font-medium text-gray-500">보유 포인트</span>
              <div className="flex items-baseline gap-2">
                <AnimatedNumber
                  value={TOTAL_POINTS - usedPoints}
                  suffix="P"
                  className="text-[40px] font-bold text-primary font-countdown leading-none tracking-tight"
                />
                {usedPoints > 0 && (
                  <span className="text-sm text-slate-400 font-medium">/ {TOTAL_POINTS}P</span>
                )}
              </div>
            </div>

            <ItemSelector
              groups={itemGroups}
              selectedIds={selectedItemIds}
              onToggle={handleToggle}
            />
          </div>

          {/* Right */}
          <div className="flex flex-col gap-6">
            <SeasonCTA seasonNumber={3} day={3} totalDays={7} deadlineTime={deadline} linkTo="/game/3/prep" status="joinable" />
            <BankruptWarning />
          </div>
        </div>
      </main>
    </div>
  );
}
