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
  storeName: "까매진 솔히",
  menuName: "쿠키",
  congestion: "crowded" as const,
  guests: 24,
  stock: 85,
  balance: 4_500_000,
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
  { id: "kim-boss", name: "김사장", storeName: "쿠키팩토리", revenue: 12_400_000, roi: 42.8 },
  { id: "lee-ceo", name: "이대표", storeName: "버블티하우스", revenue: 11_800_000, roi: 38.4 },
  { id: "me", name: "나", storeName: "까매진 솔히", revenue: 10_200_000, roi: 34.2, isMe: true },
  { id: "park-manager", name: "박점장", storeName: "핫도그랩", revenue: 9_700_000, roi: 27.5 },
  { id: "choi-owner", name: "최사장", storeName: "붕어빵연구소", revenue: 830_000, roi: 18.7 },
];

const promotionLabels: Record<string, string> = {
  influencer: "인플루언서 홍보",
  sns: "SNS 광고",
  flyer: "전단지 배포",
  referral: "지인 추천",
};

export default function PlayPage() {
  const { day } = useParams<{ day: string }>();
  const [activeModal, setActiveModal] = useState<ActionType | null>(null);
  const [usedActions, setUsedActions] = useState<Set<ActionType>>(new Set());
  const [alerts, setAlerts] = useState<GameAlert[]>(mockAlerts);

  const handleAction = (action: ActionType) => {
    setActiveModal(action);
  };

  const closeModal = () => setActiveModal(null);

  const pushActionAlert = (title: string, description: string) => {
    setAlerts((prev) => [
      {
        id: Date.now(),
        type: "action",
        title,
        description,
        time: "방금 전",
      },
      ...prev,
    ]);
  };

  const completeAction = (action: ActionType, alert?: { title: string; description: string }) => {
    setUsedActions((prev) => new Set(prev).add(action));
    if (alert) {
      pushActionAlert(alert.title, alert.description);
    }
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

        <RankingSidebar rankings={mockRankings} />
        <EventSidebar alerts={alerts} />
        <ActionBar onAction={handleAction} usedActions={usedActions} />
      </main>

      {activeModal === "discount" && (
        <DiscountModal
          currentPrice={MOCK.originalPrice}
          onClose={closeModal}
          onSubmit={(rate) => {
            console.log("할인율:", rate);
            completeAction("discount", {
              title: "할인 이벤트 적용됨",
              description: `${rate}% 할인이 적용되었습니다.`,
            });
          }}
        />
      )}
      {activeModal === "emergency" && (
        <EmergencyOrderModal
          menuItems={MOCK.menuItems}
          onClose={closeModal}
          onSubmit={(menuIndex, quantity) => {
            console.log("긴급발주:", MOCK.menuItems[menuIndex].name, quantity);
            completeAction("emergency", {
              title: "긴급 발주 완료",
              description: `${MOCK.menuItems[menuIndex].name} ${quantity}개를 긴급 발주했습니다.`,
            });
          }}
        />
      )}
      {activeModal === "promotion" && (
        <PromotionModal
          onClose={closeModal}
          onSubmit={(promotionId) => {
            console.log("홍보:", promotionId);
            completeAction("promotion", {
              title: "홍보 시작됨",
              description: `${promotionLabels[promotionId] ?? "홍보"}를 시작했습니다.`,
            });
          }}
        />
      )}
      {activeModal === "share" && (
        <ShareModal
          currentStock={MOCK.stock}
          onClose={closeModal}
          onSubmit={(quantity) => {
            console.log("나눔:", quantity);
            completeAction("share", {
              title: "나눔 이벤트 진행",
              description: `재고 ${quantity}개 나눔을 시작했습니다.`,
            });
          }}
        />
      )}
      {activeModal === "move" && (
        <MoveModal
          regions={MOCK.moveRegions}
          onClose={closeModal}
          onSubmit={(regionId) => {
            console.log("이전:", regionId);
            const destination = MOCK.moveRegions.find((region) => region.id === regionId);

            completeAction("move", {
              title: "영업 지역 이전 예약",
              description: `${destination?.name ?? "선택한 지역"}으로 다음 영업부터 이동합니다.`,
            });
          }}
        />
      )}
    </div>
  );
}
