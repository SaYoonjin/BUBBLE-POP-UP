import { useState } from "react";
import type { ActionType } from "../../play/ActionBar";
import type { GameAlert } from "../../play/EventSidebar";
import TutorialPlayLayout from "../TutorialPlayLayout";
import DiscountModal from "../../play/modals/DiscountModal";
import EmergencyOrderModal from "../../play/modals/EmergencyOrderModal";
import PromotionModal from "../../play/modals/PromotionModal";
import ShareModal from "../../play/modals/ShareModal";
import MoveModal from "../../play/modals/MoveModal";
import { MOCK_EMERGENCY_MENU_ITEMS, MOCK_MOVE_REGIONS, MOCK_PROMOTION_OPTIONS } from "../mockData";

export default function ActionStep() {
  const [openModal, setOpenModal] = useState<ActionType | null>(null);
  const [usedActions, setUsedActions] = useState<Set<ActionType>>(() => new Set());
  const [activeEffects, setActiveEffects] = useState<Set<ActionType>>(() => new Set());
  const [alerts, setAlerts] = useState<GameAlert[]>([]);

  const handleAction = (action: ActionType) => {
    if (usedActions.has(action)) return;
    setOpenModal(action);
  };

  const closeModal = () => {
    setOpenModal(null);
  };

  const completeAction = (action: ActionType, title: string, description: string) => {
    setUsedActions((prev) => new Set(prev).add(action));
    setActiveEffects((prev) => {
      const next = new Set(prev);
      next.add(action);
      return next;
    });
    setOpenModal(null);
    setAlerts((prev) => [
      {
        id: Date.now(),
        type: "action",
        title,
        description,
        createdAt: Date.now(),
      },
      ...prev,
    ]);
    // 5초 후 activeEffect 해제
    setTimeout(() => {
      setActiveEffects((prev) => {
        const next = new Set(prev);
        next.delete(action);
        return next;
      });
    }, 5000);
  };

  return (
    <TutorialPlayLayout
      alerts={alerts}
      onAction={handleAction}
      usedActions={usedActions}
      activeEffects={activeEffects}
    >
      {/* 모달이 닫혀있을 때 안내 */}
      {!openModal && (
        <div className="absolute inset-x-0 bottom-36 z-30 flex justify-center px-4">
          <div className="rounded-2xl bg-white/95 backdrop-blur-sm border border-white/60 shadow-xl px-5 py-3">
            <p className="text-sm font-bold text-slate-800 text-center">
              하단 액션 바에서 버튼을 클릭해보세요!
            </p>
            <p className="text-[11px] text-slate-500 text-center mt-1">
              5가지 액션의 실제 화면을 체험할 수 있어요 • 각 액션은 하루에 한 번씩만 사용 가능
            </p>
          </div>
        </div>
      )}

      {/* 실제 게임 모달들 */}
      {openModal === "discount" && (
        <DiscountModal
          currentPrice={2600}
          minimumPrice={1300}
          onClose={closeModal}
          onSubmit={(rate) => {
            completeAction("discount", "할인 이벤트 적용", `${rate}% 할인이 적용되었습니다.`);
          }}
        />
      )}

      {openModal === "emergency" && (
        <EmergencyOrderModal
          currentBalance={4235000}
          menuItems={MOCK_EMERGENCY_MENU_ITEMS}
          currentMenuId={10}
          currentMenuPricing={{
            costPrice: 1300,
            recommendedPrice: 2600,
            maxSellingPrice: 5200,
            sellingPrice: 2600,
          }}
          deliveryTrafficLabel="보통"
          estimatedArrivalLabel="15:00"
          onClose={closeModal}
          onSubmit={({ menuName, quantity }) => {
            completeAction("emergency", "긴급 발주 완료", `${menuName} ${quantity}개를 긴급 발주했습니다. 15:00 도착 예정`);
          }}
        />
      )}

      {openModal === "promotion" && (
        <PromotionModal
          currentBalance={4235000}
          options={MOCK_PROMOTION_OPTIONS}
          onClose={closeModal}
          onSubmit={({ promotionId }) => {
            const opt = MOCK_PROMOTION_OPTIONS.find((o) => o.id === promotionId);
            completeAction("promotion", "홍보 시작", `${opt?.name ?? "홍보"}를 시작했습니다.`);
          }}
        />
      )}

      {openModal === "share" && (
        <ShareModal
          currentStock={163}
          onClose={closeModal}
          onSubmit={(quantity) => {
            completeAction("share", "나눔 이벤트 진행", `재고 ${quantity}개 나눔을 시작했습니다.`);
          }}
        />
      )}

      {openModal === "move" && (
        <MoveModal
          currentBalance={4235000}
          currentRegionName="서울숲/성수"
          regions={MOCK_MOVE_REGIONS}
          onClose={closeModal}
          onSubmit={({ regionName }) => {
            completeAction("move", "영업 지역 이전 예약", `${regionName}으로 다음 영업부터 이동합니다.`);
          }}
        />
      )}
    </TutorialPlayLayout>
  );
}
