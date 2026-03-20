import axios from "axios";
import { useEffect, useMemo, useRef, useState } from "react";
import { useOutletContext, useParams } from "react-router-dom";
import type { GameGuardContext } from "../router/GameGuard";
import PlayHeader from "../components/play/PlayHeader";
import EventSidebar, { type GameAlert } from "../components/play/EventSidebar";
import RankingSidebar, { type RankEntry } from "../components/play/RankingSidebar";
import ActionBar, { type ActionType } from "../components/play/ActionBar";
import DiscountModal from "../components/play/modals/DiscountModal";
import EmergencyOrderModal, {
  type CurrentMenuPricing,
  type EmergencyMenuItem,
} from "../components/play/modals/EmergencyOrderModal";
import PromotionModal from "../components/play/modals/PromotionModal";
import ShareModal from "../components/play/modals/ShareModal";
import MoveModal from "../components/play/modals/MoveModal";
import { postEmergencyOrder } from "../api/action";
import { getGameDayState, startGameDay, type GameTrafficStatus } from "../api/game";
import { getCurrentOrder, type CurrentOrderResponse } from "../api/order";
import { getStoreMenus, type StoreMenuResponse } from "../api/store";
import useBrandName from "../hooks/useBrandName";
import { useUserStore } from "../stores/useUserStore";
import { normalizeDiscountMultiplier } from "../utils/dashboardItems";

interface ApiErrorResponse {
  message?: string;
}

const MOCK = {
  location: "성수",
  storeName: "버블티 스토리",
  menuName: "버블티",
  congestion: "crowded" as const,
  guests: 24,
  stock: 85,
  balance: 4_500_000,
  currentPrice: 4_100,
  moveRegions: [
    {
      id: 1,
      name: "강남구",
      icon: "🏙️",
      iconBg: "bg-blue-100",
      population: "매우 많음",
      populationColor: "text-blue-500",
      moveCost: 2_000,
    },
    {
      id: 2,
      name: "성수동",
      icon: "🧋",
      iconBg: "bg-green-100",
      population: "보통",
      populationColor: "text-green-600",
      moveCost: 1_200,
    },
    {
      id: 3,
      name: "해운대구",
      icon: "🌊",
      iconBg: "bg-purple-100",
      population: "많음",
      populationColor: "text-purple-500",
      moveCost: 1_800,
    },
  ],
};

const MENU_EMOJI_MAP: Record<number, string> = {
  1: "🍞",
  2: "🍢",
  3: "🍬",
  4: "🍽️",
  5: "🍔",
  6: "🍨",
  7: "🍗",
  8: "🌮",
  9: "🌭",
  10: "🧋",
};

const MENU_EMOJI_BY_NAME: Record<string, string> = {
  빵: "🍞",
  마라꼬치: "🍢",
  젤리: "🍬",
  떡볶이: "🍽️",
  햄버거: "🍔",
  아이스크림: "🍨",
  닭강정: "🍗",
  타코: "🌮",
  핫도그: "🌭",
  버블티: "🧋",
};

function getInitialAlerts(): GameAlert[] {
  return [
    {
      id: 1,
      type: "event",
      title: "SNS에서 입소문 확산",
      description: "손님 유입률이 15% 증가하고 있습니다.",
      time: "방금 전",
    },
    {
      id: 2,
      type: "event",
      title: "주변 축제 개최",
      description: "유동인구가 20% 증가할 예정입니다.",
      time: "5분 전",
    },
  ];
}

const baseRankings: RankEntry[] = [
  { id: "kim-boss", name: "김사장", storeName: "쿠키 팩토리", revenue: 12_400_000, roi: 42.8 },
  { id: "lee-ceo", name: "이대표", storeName: "버블티 하우스", revenue: 11_800_000, roi: 38.4 },
  { id: "me", name: "나", storeName: "버블티 스토리", revenue: 10_200_000, roi: 34.2, isMe: true },
  { id: "park-manager", name: "박점장", storeName: "핫도그랩", revenue: 9_700_000, roi: 27.5 },
  { id: "choi-owner", name: "최사장", storeName: "붕어빵연구소", revenue: 830_000, roi: 18.7 },
];

const promotionLabels: Record<string, string> = {
  influencer: "인플루언서 홍보",
  sns: "SNS 홍보",
  flyer: "전단지 배포",
  referral: "지인 소개",
};

const persistentActionTypes = new Set<ActionType>(["discount", "promotion", "share"]);

function resolveMenuEmoji(menuId: number, menuName: string) {
  return MENU_EMOJI_MAP[menuId] ?? MENU_EMOJI_BY_NAME[menuName.trim()] ?? "🍽️";
}

function mapStoreMenusToEmergencyMenus(menus: StoreMenuResponse[]): EmergencyMenuItem[] {
  return menus.map((menu) => ({
    menuId: menu.menuId,
    name: menu.menuName,
    ingredientPrice: menu.ingredientPrice,
    ingredientDiscountMultiplier: normalizeDiscountMultiplier(menu.discount),
    emoji: resolveMenuEmoji(menu.menuId, menu.menuName),
  }));
}

function getErrorMessage(error: unknown, fallbackMessage: string) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? fallbackMessage;
  }

  return fallbackMessage;
}

function formatEmergencyArrivalTime(arrivedTime: string) {
  const parsed = new Date(arrivedTime);

  if (Number.isNaN(parsed.getTime())) {
    return "";
  }

  return parsed.toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getEstimatedEmergencyArrivalTime(
  serverTime: string | null | undefined,
  delaySeconds: number | null | undefined,
) {
  if (!serverTime || typeof delaySeconds !== "number" || delaySeconds < 0) {
    return null;
  }

  const parsedServerTime = new Date(serverTime);

  if (Number.isNaN(parsedServerTime.getTime())) {
    return null;
  }

  return new Date(parsedServerTime.getTime() + delaySeconds * 1000).toISOString();
}

function getTrafficStatusLabel(status: GameTrafficStatus | null | undefined) {
  switch (status) {
    case "VERY_SMOOTH":
      return "매우 원활";
    case "SMOOTH":
      return "원활";
    case "NORMAL":
      return "보통";
    case "CONGESTED":
      return "혼잡";
    case "VERY_CONGESTED":
      return "매우 혼잡";
    default:
      return null;
  }
}

export default function PlayPage() {
  const { day } = useParams<{ day: string }>();
  const guardContext = useOutletContext<GameGuardContext>();
  const dayNumber = useMemo(() => Number(day) || 1, [day]);

  return (
    <PlayPageSession
      key={dayNumber}
      dayNumber={dayNumber}
      phaseEndTimestamp={guardContext.phaseEndTimestamp}
    />
  );
}

function PlayPageSession({
  dayNumber,
  phaseEndTimestamp,
}: {
  dayNumber: number;
  phaseEndTimestamp: number;
}) {
  const nickname = useUserStore((s) => s.nickname) ?? "버블티";
  const { brandName } = useBrandName();
  const [activeModal, setActiveModal] = useState<ActionType | null>(null);
  const [usedActions, setUsedActions] = useState<Set<ActionType>>(new Set());
  const [activeEffects, setActiveEffects] = useState<Set<ActionType>>(new Set());
  const [alerts, setAlerts] = useState<GameAlert[]>(() => getInitialAlerts());
  const [balance, setBalance] = useState(MOCK.balance);
  const [stock, setStock] = useState(MOCK.stock);
  const [guests, setGuests] = useState(MOCK.guests);
  const [currentOrder, setCurrentOrder] = useState<CurrentOrderResponse | null>(null);
  const [menuItems, setMenuItems] = useState<EmergencyMenuItem[]>([]);
  const [deliveryTrafficLabel, setDeliveryTrafficLabel] = useState<string | null>(null);
  const [emergencyArriveAt, setEmergencyArriveAt] = useState<string | null>(null);
  const [isEmergencyDataLoading, setIsEmergencyDataLoading] = useState(true);
  const [emergencyDataError, setEmergencyDataError] = useState<string | null>(null);
  const playEndTimestampMs = phaseEndTimestamp;
  const [nowMs, setNowMs] = useState(() => Date.now());
  const hasDeadlineAlertRef = useRef(false);
  const hasLowStockAlertRef = useRef(false);
  const remainingMilliseconds = Math.max(0, playEndTimestampMs - nowMs);
  const remainingSeconds = Math.max(0, Math.ceil(remainingMilliseconds / 1000));
  const playStoreName = brandName || MOCK.storeName;
  const currentMenuName = currentOrder?.menuName ?? MOCK.menuName;
  const currentMenuPricing: CurrentMenuPricing | null = currentOrder
    ? {
        costPrice: currentOrder.costPrice,
        recommendedPrice: currentOrder.recommendedPrice,
        maxSellingPrice: currentOrder.maxSellingPrice,
        sellingPrice: currentOrder.sellingPrice,
      }
    : null;

  const rankings = useMemo(
    () =>
      baseRankings.map((entry) =>
        entry.id === "me"
          ? {
              ...entry,
              name: nickname,
              storeName: playStoreName,
            }
          : entry,
      ),
    [nickname, playStoreName],
  );

  useEffect(() => {
    let isActive = true;

    const loadEmergencyOrderData = async () => {
      setIsEmergencyDataLoading(true);
      setEmergencyDataError(null);

      let startErrorMessage: string | null = null;

      try {
        await startGameDay();
      } catch (error) {
        startErrorMessage = getErrorMessage(error, "영업 상태를 준비하지 못했습니다.");
      }

      const [stateResult, orderResult, menuResult] = await Promise.allSettled([
        getGameDayState(),
        getCurrentOrder(),
        getStoreMenus(),
      ]);

      if (!isActive) {
        return;
      }

      if (stateResult.status === "fulfilled") {
        setBalance(stateResult.value.cash);
        setStock(stateResult.value.inventory.totalStock);
        setGuests(stateResult.value.customerCount);
        setDeliveryTrafficLabel(getTrafficStatusLabel(stateResult.value.traffic?.status));
        setEmergencyArriveAt(
          stateResult.value.actionStatus.emergencyOrderArriveAt ??
            getEstimatedEmergencyArrivalTime(
              stateResult.value.serverTime,
              stateResult.value.traffic?.delaySeconds,
            ),
        );
      } else {
        setDeliveryTrafficLabel(null);
        setEmergencyArriveAt(null);
      }

      if (orderResult.status === "fulfilled") {
        setCurrentOrder(orderResult.value);
      } else {
        setCurrentOrder(null);
      }

      if (menuResult.status === "fulfilled") {
        setMenuItems(mapStoreMenusToEmergencyMenus(menuResult.value.menus));
      } else {
        setMenuItems([]);
      }

      const nextError =
        stateResult.status === "rejected"
          ? getErrorMessage(
              stateResult.reason,
              startErrorMessage ?? "현재 게임 상태를 불러오지 못했습니다.",
            )
          : orderResult.status === "rejected"
            ? getErrorMessage(orderResult.reason, "현재 판매 메뉴 정보를 불러오지 못했습니다.")
            : menuResult.status === "rejected"
              ? getErrorMessage(menuResult.reason, "메뉴 목록을 불러오지 못했습니다.")
              : null;

      setEmergencyDataError(nextError);
      setIsEmergencyDataLoading(false);
    };

    void loadEmergencyOrderData();

    return () => {
      isActive = false;
    };
  }, [dayNumber]);

  useEffect(() => {
    if (Date.now() >= playEndTimestampMs) {
      return;
    }

    const timer = window.setInterval(() => {
      const nextNowMs = Date.now();
      const nextRemainingMilliseconds = Math.max(0, playEndTimestampMs - nextNowMs);
      const nextRemainingSeconds = Math.max(0, Math.ceil(nextRemainingMilliseconds / 1000));

      if (
        nextRemainingSeconds <= 60 &&
        nextRemainingSeconds > 0 &&
        !hasDeadlineAlertRef.current
      ) {
        hasDeadlineAlertRef.current = true;
        setAlerts((prev) => [
          {
            id: Date.now() + Math.floor(Math.random() * 1000),
            type: "deadline",
            title: "마감 1분 전",
            description: "영업 종료가 곧 다가옵니다.",
            time: "방금 전",
          },
          ...prev,
        ]);
      }

      setNowMs(nextNowMs);

      if (nextRemainingMilliseconds <= 0) {
        window.clearInterval(timer);
      }
    }, 100);

    return () => window.clearInterval(timer);
  }, [playEndTimestampMs]);

  const handleAction = (action: ActionType) => {
    setActiveModal(action);
  };

  const closeModal = () => setActiveModal(null);

  const pushAlert = (
    type: GameAlert["type"],
    title: string,
    description: string,
    time = "방금 전",
  ) => {
    setAlerts((prev) => [
      {
        id: Date.now() + Math.floor(Math.random() * 1000),
        type,
        title,
        description,
        time,
      },
      ...prev,
    ]);
  };

  const pushActionAlert = (title: string, description: string) => {
    pushAlert("action", title, description);
  };

  const completeAction = (
    action: ActionType,
    options?: {
      cost?: number;
      stockDelta?: number;
      alert?: {
        title: string;
        description: string;
      };
    },
  ) => {
    setUsedActions((prev) => new Set(prev).add(action));

    if (persistentActionTypes.has(action)) {
      setActiveEffects((prev) => new Set(prev).add(action));
    }

    const cost = options?.cost;
    const stockDelta = options?.stockDelta;
    const nextStock =
      typeof stockDelta === "number" && stockDelta !== 0 ? Math.max(0, stock + stockDelta) : stock;

    if (typeof cost === "number" && cost > 0) {
      setBalance((prev) => prev - cost);
    }

    if (typeof stockDelta === "number" && stockDelta !== 0) {
      setStock(nextStock);

      if (nextStock > 30) {
        hasLowStockAlertRef.current = false;
      } else if (stock > 30 && !hasLowStockAlertRef.current) {
        hasLowStockAlertRef.current = true;
        pushAlert("stock", "재고 30개 이하", "긴급 발주를 고려해보세요.");
      }
    }

    if (options?.alert) {
      pushActionAlert(options.alert.title, options.alert.description);
    }

    closeModal();
  };

  return (
    <div className="selection:bg-primary selection:text-white flex h-screen w-full flex-col overflow-hidden font-display text-slate-900">
      <PlayHeader
        location={MOCK.location}
        storeName={playStoreName}
        menuName={currentMenuName}
        day={dayNumber}
        remainingSeconds={remainingSeconds}
        remainingMilliseconds={remainingMilliseconds}
        congestion={MOCK.congestion}
        guests={guests}
        stock={stock}
        balance={balance}
      />

      <main className="relative flex flex-1 overflow-hidden">
        <div className="absolute inset-0 z-0 bg-transparent" />
        <div className="relative z-0 flex-1" />

        <RankingSidebar rankings={rankings} />
        <EventSidebar alerts={alerts} />
        <ActionBar onAction={handleAction} usedActions={usedActions} activeEffects={activeEffects} />
      </main>

      {activeModal === "discount" && (
        <DiscountModal
          currentPrice={currentOrder?.sellingPrice ?? MOCK.currentPrice}
          onClose={closeModal}
          onSubmit={(rate) => {
            completeAction("discount", {
              alert: {
                title: "할인 이벤트 적용",
                description: `${rate}% 할인이 적용되었습니다.`,
              },
            });
          }}
        />
      )}

      {activeModal === "emergency" && (
        <EmergencyOrderModal
          currentBalance={balance}
          menuItems={menuItems}
          currentMenuId={currentOrder?.menuId ?? null}
          currentMenuPricing={currentMenuPricing}
          deliveryTrafficLabel={deliveryTrafficLabel}
          estimatedArrivalTime={emergencyArriveAt}
          isInitializing={isEmergencyDataLoading}
          initializationError={emergencyDataError}
          onClose={closeModal}
          onSubmit={async ({ menuId, menuName, quantity, salePrice }) => {
            const response = await postEmergencyOrder(menuId, quantity, salePrice);
            const isNewMenuOrder = menuId !== currentOrder?.menuId;
            const arrivalLabel = formatEmergencyArrivalTime(response.arrivedTime);
            const arrivalText = arrivalLabel ? ` ${arrivalLabel} 도착 예정입니다.` : "";

            setEmergencyArriveAt(response.arrivedTime);

            completeAction("emergency", {
              cost: response.totalCost,
              alert: {
                title: "긴급 발주 완료",
                description: isNewMenuOrder
                  ? `${menuName} ${quantity}개를 긴급 발주했습니다.${arrivalText} 새 메뉴 주문입니다.`
                  : `${menuName} ${quantity}개를 긴급 발주했습니다.${arrivalText}`,
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
                title: "홍보 시작",
                description: `${promotionLabels[promotionId] ?? "홍보"}를 시작했습니다.`,
              },
            });
          }}
        />
      )}

      {activeModal === "share" && (
        <ShareModal
          currentStock={stock}
          onClose={closeModal}
          onSubmit={(quantity) => {
            completeAction("share", {
              stockDelta: -quantity,
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
          currentRegionName={MOCK.location}
          onClose={closeModal}
          onSubmit={({ regionName, cost }) => {
            completeAction("move", {
              cost,
              alert: {
                title: "영업 지역 이전 예약",
                description: `${regionName}으로 다음 영업부터 이동합니다.`,
              },
            });
          }}
        />
      )}
    </div>
  );
}
