import { useEffect, useState } from "react";
import AppHeader from "../components/common/AppHeader";
import FloatingBubbles from "../components/common/FloatingBubbles";
import ItemSelector from "../components/common/ItemSelector";
import SeasonCTA from "../components/common/SeasonCTA";
import BankruptWarning from "../components/common/BankruptWarning";
import { DASHBOARD_SELECTED_ITEMS_STORAGE_KEY } from "../constants";

const dashBubbles = [
  { size: "w-64 h-64", position: "top-[5%] left-[-5%]", opacity: "opacity-40", delay: "0s", variant: "glass" as const },
  { size: "w-48 h-48", position: "bottom-10 right-[-2%]", opacity: "opacity-30", delay: "2s", variant: "glass" as const },
  { size: "w-24 h-24", position: "top-1/3 left-[40%]", opacity: "opacity-20", delay: "4s", variant: "glass" as const },
];

const mockItems = [
  { id: 1, name: "🚚 긴급 발주 특권", desc: "주문 즉시 재고가 보충되어 판매 기회를 놓치지 않습니다.", price: "30P" },
  { id: 2, name: "📢 특별 홍보 패키지", desc: "소셜 미디어 홍보를 통해 방문객 유입이 20% 증가합니다.", price: "50P" },
  { id: 3, name: "☕ 성수 한정 쿠폰", desc: "단골 고객들에게 쿠폰을 발행하여 재방문율을 높입니다.", price: "20P" },
];

function getInitialSelectedItemIds() {
  try {
    const stored = localStorage.getItem(DASHBOARD_SELECTED_ITEMS_STORAGE_KEY);
    if (!stored) return [1];
    const parsed = JSON.parse(stored);
    return Array.isArray(parsed) ? parsed.filter((value): value is number => typeof value === "number") : [1];
  } catch {
    return [1];
  }
}

export default function DashboardPage() {
  const [selectedItemIds, setSelectedItemIds] = useState<number[]>(getInitialSelectedItemIds);

  const handleToggle = (id: number) => {
    setSelectedItemIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    );
  };

  useEffect(() => {
    localStorage.setItem(DASHBOARD_SELECTED_ITEMS_STORAGE_KEY, JSON.stringify(selectedItemIds));
  }, [selectedItemIds]);

  return (
    <div className="relative min-h-screen w-full flex flex-col bg-[#FDFDFB] text-slate-900 overflow-x-hidden font-display">
      <FloatingBubbles bubbles={dashBubbles} />
      <AppHeader nickname="솔희" />

      <main className="flex-1 flex flex-col items-center w-full px-6 md:px-12 pt-24 pb-12 z-10 max-w-[1100px] mx-auto">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 w-full items-start">
          {/* Left */}
          <div className="flex flex-col gap-6 lg:w-[95%]">
            <div className="bg-white rounded-[20px] shadow-soft p-6 w-full flex flex-col gap-2">
              <span className="text-sm font-medium text-gray-500">보유 포인트</span>
              <div className="text-[40px] font-bold text-primary font-mono leading-none tracking-tight">100P</div>
            </div>
            <ItemSelector items={mockItems} selectedIds={selectedItemIds} onToggle={handleToggle} />
          </div>

          {/* Right */}
          <div className="flex flex-col gap-6">
            <SeasonCTA seasonNumber={3} day={3} countdown="05:42" bestRank={3} linkTo="/game/3/prep" />
            <BankruptWarning onRetry={() => alert("다시 도전!")} />
          </div>
        </div>
      </main>
    </div>
  );
}
