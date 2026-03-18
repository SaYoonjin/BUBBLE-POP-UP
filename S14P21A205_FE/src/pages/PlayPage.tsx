import { useMemo, useState } from "react";
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

const MOCK = {
  location: "성수",
  storeName: "버블스토리",
  menuName: "버블티",
  congestion: "crowded" as const,
  guests: 24,
  stock: 85,
  balance: 4_500_000,
  gameTime: "14:30",
  currentPrice: 4_100,
  menuItems: [
    { name: "빵", price: 1_800, emoji: "🍞" },
    { name: "마라꼬치", price: 2_200, emoji: "🍢" },
    { name: "젤리", price: 900, emoji: "🍬" },
    { name: "떡볶이", price: 2_500, emoji: "🍽️" },
    { name: "햄버거", price: 3_100, emoji: "🍔" },
    { name: "아이스크림", price: 1_400, emoji: "🍦" },
    { name: "닭강정", price: 2_800, emoji: "🍗" },
    { name: "타코", price: 2_600, emoji: "🌮" },
    { name: "핫도그", price: 1_700, emoji: "🌭" },
    { name: "버블티", price: 2_300, emoji: "🧋" },
  ],
  moveRegions: [
    {
      id: 1,
      name: "강남역",
      icon: "🏙️",
      iconBg: "bg-blue-100",
      population: "매우 많음",
      populationColor: "text-blue-500",
      moveCost: 2_000,
    },
    {
      id: 2,
      name: "서울숲",
      icon: "🌲",
      iconBg: "bg-green-100",
      population: "보통",
      populationColor: "text-green-600",
      moveCost: 1_200,
    },
    {
      id: 3,
      name: "홍대입구",
      icon: "🎓",
      iconBg: "bg-purple-100",
      population: "많음",
      populationColor: "text-purple-500",
      moveCost: 1_800,
    },
  ],
};

const mockAlerts: GameAlert[] = [
  {
    id: 1,
    type: "event",
    title: "SNS에서 입소문 확산",
    description: "손님 유입률이 15% 증가했습니다.",
    time: "방금 전",
  },
  {
    id: 2,
    type: "deadline",
    title: "마감 1분 전",
    description: "영업 종료가 곧 다가옵니다.",
    time: "1분 전",
  },
  {
    id: 3,
    type: "stock",
    title: "재고 30개 이하",
    description: "긴급 발주를 고려해보세요.",
    time: "3분 전",
  },
  {
    id: 4,
    type: "event",
    title: "주말 축제 개막",
    description: "유동인구가 20% 증가할 예정입니다.",
    time: "5분 전",
  },
  {
    id: 5,
    type: "action",
    title: "할인 이벤트 적용됨",
    description: "20% 할인이 적용되었습니다.",
    time: "8분 전",
  },
];

const mockRankings: RankEntry[] = [
  { id: "kim-boss", name: "김사장", storeName: "쿠키팩토리", revenue: 12_400_000, roi: 42.8 },
  { id: "lee-ceo", name: "이대표", storeName: "버블티하우스", revenue: 11_800_000, roi: 38.4 },
  { id: "me", name: "나", storeName: "버블스토리", revenue: 10_200_000, roi: 34.2, isMe: true },
  { id: "park-manager", name: "박점장", storeName: "핫도그랩", revenue: 9_700_000, roi: 27.5 },
  { id: "choi-owner", name: "최사장", storeName: "붕어빵연구소", revenue: 830_000, roi: 18.7 },
];

const promotionLabels: Record<string, string> = {
  influencer: "인플루언서 홍보",
  sns: "SNS 홍보",
  flyer: "전단지 배포",
  referral: "지인 소개",
};

export default function PlayPage() {
  const { day } = useParams<{ day: string }>();
  const [activeModal, setActiveModal] = useState<ActionType | null>(null);
  const [usedActions, setUsedActions] = useState<Set<ActionType>>(new Set());
  const [alerts, setAlerts] = useState<GameAlert[]>(mockAlerts);
  const [balance, setBalance] = useState(MOCK.balance);

  const dayNumber = useMemo(() => Number(day) || 1, [day]);

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

  const completeAction = (
    action: ActionType,
    options?: {
      cost?: number;
      alert?: {
        title: string;
        description: string;
      };
    },
  ) => {
    setUsedActions((prev) => new Set(prev).add(action));

    if (typeof options?.cost === "number" && options.cost > 0) {
      setBalance((prev) => prev - options.cost);
    }

    if (options?.alert) {
      pushActionAlert(options.alert.title, options.alert.description);
    }

    closeModal();
  };

  return (
    <div className="flex h-screen w-full flex-col overflow-hidden font-display text-slate-900 selection:bg-primary selection:text-white">
      <PlayHeader
        location={MOCK.location}
        storeName={MOCK.storeName}
        menuName={MOCK.menuName}
        day={dayNumber}
        remainingSeconds={153}
        gameTime={MOCK.gameTime}
        congestion={MOCK.congestion}
        guests={MOCK.guests}
        stock={MOCK.stock}
        balance={balance}
      />

      <main className="relative flex flex-1 overflow-hidden">
        <div className="absolute inset-0 z-0 bg-transparent" />
        <div className="relative z-0 flex-1" />

        <RankingSidebar rankings={mockRankings} />
        <EventSidebar alerts={alerts} />
        <ActionBar onAction={handleAction} usedActions={usedActions} />
      </main>

      {activeModal === "discount" && (
        <DiscountModal
          currentPrice={MOCK.currentPrice}
          onClose={closeModal}
          onSubmit={(rate) => {
            completeAction("discount", {
              alert: {
                title: "할인 이벤트 적용됨",
                description: `${rate}% 할인이 적용되었습니다.`,
              },
            });
          }}
        />
      )}

      {activeModal === "emergency" && (
        <EmergencyOrderModal
          currentBalance={balance}
          menuItems={MOCK.menuItems}
          currentMenuName={MOCK.menuName}
          onClose={closeModal}
          onSubmit={({ menuIndex, quantity, totalCost }) => {
            const selectedMenu = MOCK.menuItems[menuIndex];
            const isNewMenuOrder = selectedMenu.name !== MOCK.menuName;

            completeAction("emergency", {
              cost: totalCost,
              alert: {
                title: "긴급 발주 완료",
                description: isNewMenuOrder
                  ? `${selectedMenu.name} ${quantity}개를 긴급 발주했습니다. 새 메뉴 주문입니다.`
                  : `${selectedMenu.name} ${quantity}개를 긴급 발주했습니다.`,
              },
            });
          }}
        />
      )}

      {activeModal === "promotion" && (
        <PromotionModal
          currentBalance={balance}
          onClose={closeModal}
          onSubmit={({ promotionId, cost }) => {
            completeAction("promotion", {
              cost,
              alert: {
                title: "홍보 시작됨",
                description: `${promotionLabels[promotionId] ?? "홍보"}를 시작했습니다.`,
              },
            });
          }}
        />
      )}

      {activeModal === "share" && (
        <ShareModal
          currentStock={MOCK.stock}
          onClose={closeModal}
          onSubmit={(quantity) => {
            completeAction("share", {
              alert: {
                title: "나눔 이벤트 진행",
                description: `재고 ${quantity}개 나눔을 시작했습니다.`,
              },
            });
          }}
        />
      )}

      {activeModal === "move" && (
        <MoveModal
          currentBalance={balance}
          regions={MOCK.moveRegions}
          onClose={closeModal}
          onSubmit={({ regionId, cost }) => {
            const destination = MOCK.moveRegions.find(
              (region) => region.id === regionId,
            );

            completeAction("move", {
              cost,
              alert: {
                title: "영업 지역 이전 예약",
                description: `${destination?.name ?? "선택한 지역"}으로 다음 영업부터 이동합니다.`,
              },
            });
          }}
        />
      )}
    </div>
  );
}
