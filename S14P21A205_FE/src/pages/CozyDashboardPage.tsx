import { useState } from "react";
import CozyWoodBackground from "../components/cozy/CozyWoodBackground";
import CozyAppHeader from "../components/cozy/CozyAppHeader";
import CozyItemChecklist from "../components/cozy/CozyItemChecklist";
import CozySeasonCalendar from "../components/cozy/CozySeasonCalendar";
import CozyStickyNote from "../components/cozy/CozyStickyNote";

const mockItems = [
  { id: 1, name: "🚚 긴급 발주 특권", desc: "주문 즉시 재고가 보충되어 판매 기회를 놓치지 않습니다.", price: "30P" },
  { id: 2, name: "📢 특별 홍보 패키지", desc: "소셜 미디어 홍보를 통해 방문객 유입이 20% 증가합니다.", price: "50P" },
  { id: 3, name: "☕ 성수 한정 쿠폰", desc: "단골 고객들에게 쿠폰을 발행하여 재방문율을 높입니다.", price: "20P" },
];

export default function CozyDashboardPage() {
  const [selectedItemIds, setSelectedItemIds] = useState<number[]>([1]);

  const handleToggle = (id: number) => {
    setSelectedItemIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    );
  };

  return (
    <CozyWoodBackground>
      <CozyAppHeader nickname="솔희" />

      <main className="relative z-10 flex-1 flex flex-col items-center w-full px-6 md:px-12 pt-28 pb-12 max-w-[1200px] mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 w-full items-start">
          {/* Left: Items (Paper checklist) */}
          <div className="lg:col-span-5 flex flex-col gap-8 order-2 lg:order-1">
            <CozyItemChecklist
              items={mockItems}
              selectedIds={selectedItemIds}
              onToggle={handleToggle}
              points="100P"
            />
            <CozyStickyNote
              icon="error"
              title="파산했지만 괜찮아요!"
              description="아직 포기하지 마세요. 새로운 전략으로 다시 시작해보세요."
              actionLabel="다시 도전하기"
              onAction={() => alert("다시 도전!")}
            />
          </div>

          {/* Right: Season Calendar + Pen */}
          <div className="lg:col-span-7 flex flex-col items-center justify-center order-1 lg:order-2">
            <div className="relative w-full max-w-md">
              <CozySeasonCalendar
                seasonNumber={3}
                day={3}
                countdown="05:42"
                bestRank={3}
                linkTo="/cozy/prep"
              />
              {/* Pen */}
              <div className="absolute -right-12 bottom-0 w-64 h-4 bg-gradient-to-r from-black to-gray-700 rounded-full shadow-lg rotate-[135deg] opacity-90 z-10 hidden lg:block">
                <div className="absolute right-0 top-0 h-full w-12 bg-cozy-primary rounded-r-full" />
                <div className="absolute right-12 top-0 h-full w-2 bg-gray-400" />
              </div>
            </div>
          </div>
        </div>
      </main>

      <footer className="mt-auto py-8 text-center text-white/30 font-cozy-serif italic text-xs z-10">
        © 2024 The Daily Bubble • All Rights Reserved
      </footer>
    </CozyWoodBackground>
  );
}
