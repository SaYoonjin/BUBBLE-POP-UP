import { useState } from "react";
import { useParams } from "react-router-dom";
import PlayHeader from "../components/play/PlayHeader";
import EventSidebar, { type GameEvent } from "../components/play/EventSidebar";
import ActionBar, { type ActionType } from "../components/play/ActionBar";
import DiscountModal from "../components/play/modals/DiscountModal";
import EmergencyOrderModal from "../components/play/modals/EmergencyOrderModal";
import PromotionModal from "../components/play/modals/PromotionModal";
import ShareModal from "../components/play/modals/ShareModal";
import MoveModal from "../components/play/modals/MoveModal";

// TODO: Replace with real data from API / game state
const MOCK = {
  location: "성수",
  storeName: "윤진이의 까까이",
  menuName: "쿠키",
  population: 1240,
  guests: 24,
  stock: 85,
  balance: 450,
  originalPrice: 10000,
  menuItems: [
    { name: "쿠키", price: 2000 },
    { name: "아메리카노", price: 1500 },
    { name: "라떼", price: 1800 },
  ],
  moveRegions: [
    { id: 1, name: "강남역", icon: "🏙️", iconBg: "bg-blue-100", population: "매우 많음", populationColor: "text-blue-500", moveCost: 2000 },
    { id: 2, name: "서울숲", icon: "🌲", iconBg: "bg-green-100", population: "보통", populationColor: "text-green-600", moveCost: 1200 },
    { id: 3, name: "홍대입구", icon: "🎓", iconBg: "bg-purple-100", population: "많음", populationColor: "text-purple-500", moveCost: 1800 },
  ],
};

const mockEvents: GameEvent[] = [
  { id: 1, type: "good", title: "SNS에서 입소문 타는 중! 🎉", description: "손님 유입률 +15% 증가", time: "방금 전" },
  { id: 2, type: "warning", title: "갑작스러운 소나기 ☔", description: "야외 손님 감소 예상", time: "2분 전" },
  { id: 3, type: "system", title: "재고 부족 경고 ⚠️", description: "인기 품목 재발주 필요", time: "5분 전" },
];

export default function PlayPage() {
  const { day } = useParams<{ day: string }>();
  const [activeModal, setActiveModal] = useState<ActionType | null>(null);
  const [events] = useState<GameEvent[]>(mockEvents);

  const handleAction = (action: ActionType) => {
    setActiveModal(action);
  };

  const closeModal = () => setActiveModal(null);

  return (
    <div className="h-screen w-full overflow-hidden flex flex-col font-display text-slate-900 selection:bg-primary selection:text-white">
      {/* Header */}
      <PlayHeader
        location={MOCK.location}
        storeName={MOCK.storeName}
        menuName={MOCK.menuName}
        day={Number(day) || 1}
        timeLeft="02:33"
        population={MOCK.population}
        guests={MOCK.guests}
        stock={MOCK.stock}
        balance={MOCK.balance}
      />

      {/* Main content */}
      <main className="flex-1 flex overflow-hidden relative">
        {/* Unity background placeholder - transparent so Unity shows through */}
        <div className="absolute inset-0 z-0 bg-transparent" />

        {/* Center area - empty, Unity renders here */}
        <div className="flex-1 relative z-0" />

        {/* Event sidebar */}
        <EventSidebar events={events} />

        {/* Action bar */}
        <ActionBar onAction={handleAction} />
      </main>

      {/* Modals */}
      {activeModal === "discount" && (
        <DiscountModal
          originalPrice={MOCK.originalPrice}
          onClose={closeModal}
          onSubmit={(rate) => {
            console.log("할인율:", rate);
            closeModal();
          }}
        />
      )}

      {activeModal === "emergency" && (
        <EmergencyOrderModal
          menuItems={MOCK.menuItems}
          onClose={closeModal}
          onSubmit={(menuIndex, qty) => {
            console.log("긴급발주:", MOCK.menuItems[menuIndex].name, qty);
            closeModal();
          }}
        />
      )}

      {activeModal === "promotion" && (
        <PromotionModal
          onClose={closeModal}
          onSubmit={(id) => {
            console.log("홍보:", id);
            closeModal();
          }}
        />
      )}

      {activeModal === "share" && (
        <ShareModal
          currentStock={MOCK.stock}
          onClose={closeModal}
          onSubmit={(qty) => {
            console.log("나눔:", qty);
            closeModal();
          }}
        />
      )}

      {activeModal === "move" && (
        <MoveModal
          regions={MOCK.moveRegions}
          onClose={closeModal}
          onSubmit={(regionId) => {
            console.log("이전:", regionId);
            closeModal();
          }}
        />
      )}
    </div>
  );
}
