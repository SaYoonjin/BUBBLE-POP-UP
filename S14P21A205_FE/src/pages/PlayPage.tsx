import { useState } from "react";
import { useParams } from "react-router-dom";
import PlayHeader from "../components/play/PlayHeader";
import EventSidebar, { type GameAlert } from "../components/play/EventSidebar";
import RankingSidebar, { type RankEntry } from "../components/play/RankingSidebar";
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
  congestion: "crowded" as const,
  guests: 24,
  stock: 85,
  balance: 450,
  gameTime: "14:30",
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

const mockAlerts: GameAlert[] = [
  { id: 1, type: "event", title: "SNS에서 입소문 확산", description: "손님 유입률 +15% 증가", time: "방금 전" },
  { id: 2, type: "deadline", title: "마감 1분 전", description: "영업 종료가 곧 다가옵니다", time: "1분 전" },
  { id: 3, type: "stock", title: "재고 30개 이하", description: "긴급 발주를 고려해보세요", time: "3분 전" },
  { id: 4, type: "event", title: "주변 축제 개최", description: "유동인구 +20% 증가 예상", time: "5분 전" },
  { id: 5, type: "action", title: "할인 이벤트 적용됨", description: "20% 할인이 적용되었습니다", time: "8분 전" },
];

const mockRankings: RankEntry[] = [
  { rank: 1, name: "김사장", storeName: "홍대 쿠키팩토리", revenue: "₩12.4M" },
  { rank: 2, name: "이대표", storeName: "강남 버블티", revenue: "₩11.8M" },
  { rank: 3, name: "나", storeName: "성수 윤진이의 까까이", revenue: "₩10.2M", isMe: true },
  { rank: 4, name: "박점장", storeName: "명동 핫도그", revenue: "₩9.7M" },
  { rank: 5, name: "최사장", storeName: "잠실 붕어빵", revenue: "₩8.3M" },
];

export default function PlayPage() {
  const { day } = useParams<{ day: string }>();
  const [activeModal, setActiveModal] = useState<ActionType | null>(null);
  const [usedActions, setUsedActions] = useState<Set<ActionType>>(new Set());
  const [alerts] = useState<GameAlert[]>(mockAlerts);

  const handleAction = (action: ActionType) => {
    setActiveModal(action);
  };

  const closeModal = () => setActiveModal(null);

  const completeAction = (action: ActionType) => {
    setUsedActions((prev) => new Set(prev).add(action));
    closeModal();
  };

  return (
    <div className="h-screen w-full overflow-hidden flex flex-col font-display text-slate-900 selection:bg-primary selection:text-white">
      <PlayHeader
        location={MOCK.location}
        storeName={MOCK.storeName}
        menuName={MOCK.menuName}
        day={Number(day) || 1}
        remainingSeconds={153}
        gameTime={MOCK.gameTime}
        congestion={MOCK.congestion}
        guests={MOCK.guests}
        stock={MOCK.stock}
        balance={MOCK.balance}
      />

      <main className="flex-1 flex overflow-hidden relative">
        <div className="absolute inset-0 z-0 bg-transparent" />
        <div className="flex-1 relative z-0" />

        {/* Left: Ranking */}
        <RankingSidebar rankings={mockRankings} />

        {/* Right: Alerts */}
        <EventSidebar alerts={alerts} />

        {/* Bottom: Actions */}
        <ActionBar onAction={handleAction} usedActions={usedActions} />
      </main>

      {activeModal === "discount" && (
        <DiscountModal originalPrice={MOCK.originalPrice} onClose={closeModal}
          onSubmit={(rate) => { console.log("할인율:", rate); completeAction("discount"); }} />
      )}
      {activeModal === "emergency" && (
        <EmergencyOrderModal menuItems={MOCK.menuItems} onClose={closeModal}
          onSubmit={(mi, qty) => { console.log("긴급발주:", MOCK.menuItems[mi].name, qty); completeAction("emergency"); }} />
      )}
      {activeModal === "promotion" && (
        <PromotionModal onClose={closeModal}
          onSubmit={(id) => { console.log("홍보:", id); completeAction("promotion"); }} />
      )}
      {activeModal === "share" && (
        <ShareModal currentStock={MOCK.stock} onClose={closeModal}
          onSubmit={(qty) => { console.log("나눔:", qty); completeAction("share"); }} />
      )}
      {activeModal === "move" && (
        <MoveModal regions={MOCK.moveRegions} onClose={closeModal}
          onSubmit={(rid) => { console.log("이전:", rid); completeAction("move"); }} />
      )}
    </div>
  );
}
