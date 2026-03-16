package com.ssafy.S14P21A205.action.service;

import com.ssafy.S14P21A205.action.dto.ActionResponse;
import com.ssafy.S14P21A205.action.dto.ActionStatusResponse;
import com.ssafy.S14P21A205.action.dto.DiscountRequest;
import com.ssafy.S14P21A205.action.dto.DiscountResponse;
import com.ssafy.S14P21A205.action.dto.DonationRequest;
import com.ssafy.S14P21A205.action.dto.DonationResponse;
import com.ssafy.S14P21A205.action.dto.EmergencyOrderRequest;
import com.ssafy.S14P21A205.action.dto.EmergencyOrderResponse;
import com.ssafy.S14P21A205.action.dto.PromotionPriceResponse;
import com.ssafy.S14P21A205.action.dto.PromotionRequest;
import com.ssafy.S14P21A205.action.entity.Action;
import com.ssafy.S14P21A205.action.entity.ActionCategory;
import com.ssafy.S14P21A205.action.entity.ActionLog;
import com.ssafy.S14P21A205.action.repository.ActionLogRepository;
import com.ssafy.S14P21A205.action.repository.ActionRepository;
import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.game.state.repository.GameStateRedisRepository;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionServiceImpl implements ActionService {

    private static final BigDecimal EMERGENCY_COST_MULTIPLIER = new BigDecimal("1.5");
    // TODO(지원) : 긴급발주 도착 시간 고정 30초. TrafficStatus 기반으로 교통량에 따라 동적 계산으로 변경 필요.
    private static final int EMERGENCY_DELIVERY_SECONDS = 30;

    // 가격 구간 판정 기준 (평균가 대비 ±10%)
    private static final BigDecimal PRICE_RANGE_UPPER = new BigDecimal("1.10");
    private static final BigDecimal PRICE_RANGE_LOWER = new BigDecimal("0.90");
    private static final BigDecimal MULTIPLIER_ABOVE = new BigDecimal("0.80");
    private static final BigDecimal MULTIPLIER_AVERAGE = new BigDecimal("1.00");
    private static final BigDecimal MULTIPLIER_BELOW = new BigDecimal("1.20");

    // 나눔 유입률 보너스: 5개 단위당 0.01
    private static final BigDecimal DONATION_BONUS_PER_UNIT = new BigDecimal("0.01");
    private static final int DONATION_UNIT_SIZE = 5;

    private final GameStateRedisRepository gameStateRedisRepository;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;
    private final ActionRepository actionRepository;
    private final ActionLogRepository actionLogRepository;
    private final StoreRepository storeRepository;
    private final SeasonRepository seasonRepository;
    private final OrderRepository orderRepository;

    // ── GET API ──

    @Override
    public ActionStatusResponse getActionStatus(Integer userId) {
        Store store = findStore(userId);
        int day = getCurrentDay();
        Map<String, Boolean> actions = gameStateRedisRepository.getActions(store.getId(), day);
        return ActionStatusResponse.from(actions);
    }

    @Override
    public PromotionPriceResponse getPromotionPrices() {
        List<Action> promotions = actionRepository.findByCategory(ActionCategory.PROMOTION);
        return PromotionPriceResponse.from(promotions);
    }

    // ── POST API ──

    @Override
    @Transactional
    public ActionResponse executePromotion(Integer userId, PromotionRequest request) {
        Store store = findStore(userId);
        int day = getCurrentDay();
        String field = request.promotionType().name().toLowerCase();

        validateNotUsed(store.getId(), day, field);

        Action action = actionRepository
                .findByCategoryAndPromotionType(ActionCategory.PROMOTION, request.promotionType())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));

        validateBalance(store.getId(), day, action.getCost());

        // inflow_rate에 곱하기 반영 (captureRate 0.10 → ×1.10)
        BigDecimal multiplier = BigDecimal.ONE.add(action.getCaptureRate());
        applyInflowRateMultiplier(store.getId(), day, multiplier);

        actionLogRepository.save(new ActionLog(action, store, day, null));
        gameStateRedisRepository.markActionUsed(store.getId(), day, field);

        return new ActionResponse(
                "PROMOTION_" + request.promotionType().name(),
                action.getCost(),
                "홍보가 실행되었습니다."
        );
    }

    @Override
    @Transactional
    public DiscountResponse executeDiscount(Integer userId, DiscountRequest request) {
        Store store = findStore(userId);
        int day = getCurrentDay();

        validateNotUsed(store.getId(), day, "discount");

        Action action = findSingleAction(ActionCategory.DISCOUNT);
        validateBalance(store.getId(), day, action.getCost());

        // 새 판매가 계산 (현재가 - 할인금액)
        int previousPrice = store.getPrice();
        int newPrice = previousPrice - request.discountValue();

        if (newPrice <= 0) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // Store 판매가 영구 변경
        store.changePrice(newPrice);

        // TODO(지원) : 평균가를 menu.originPrice로 임시 사용 중. 같은 시즌·같은 메뉴를 파는 전체 store의 판매가 평균으로 교체 필요.
        //  Day Start에서 평균가를 계산해서 state Hash에 저장하거나, 여기서 쿼리로 조회하는 방식으로 변경.
        int averagePrice = store.getMenu().getOriginPrice();
        PriceRange priceRange = determinePriceRange(newPrice, averagePrice);

        // state Hash의 sale_price 즉시 갱신
        gameDayStoreStateRedisRepository.updateField(
                store.getId(), day, "sale_price", String.valueOf(newPrice));

        // inflow_rate에 가격구간 배수 곱하기 반영
        applyInflowRateMultiplier(store.getId(), day, priceRange.multiplier);

        // actionValue에 가격구간 배수 저장 (이력용)
        actionLogRepository.save(new ActionLog(action, store, day, priceRange.multiplier));
        gameStateRedisRepository.markActionUsed(store.getId(), day, "discount");

        return new DiscountResponse(
                previousPrice,
                newPrice,
                priceRange.label,
                priceRange.multiplier,
                "할인 이벤트가 실행되었습니다. 판매가가 " + previousPrice + "원 → " + newPrice + "원으로 변경됩니다."
        );
    }

    @Override
    @Transactional
    public DonationResponse executeDonation(Integer userId, DonationRequest request) {
        Store store = findStore(userId);
        int day = getCurrentDay();

        validateNotUsed(store.getId(), day, "donation");

        Action action = findSingleAction(ActionCategory.DONATION);

        // 재고 검증: 나눔 수량만큼 있어야 함
        GameDayLiveState state = gameDayStoreStateRedisRepository.find(store.getId(), day)
                .orElseThrow(() -> new BaseException(ErrorCode.GAME_STATE_NOT_FOUND));
        int currentStock = state.stock() == null ? 0 : state.stock();
        if (currentStock < request.quantity()) {
            throw new BaseException(ErrorCode.INSUFFICIENT_STOCK);
        }

        // 유입률 보너스 계산: 5개 단위당 0.01
        int bonusUnits = request.quantity() / DONATION_UNIT_SIZE;
        BigDecimal captureRateBonus = DONATION_BONUS_PER_UNIT
                .multiply(BigDecimal.valueOf(bonusUnits))
                .setScale(2, RoundingMode.HALF_UP);

        // state Hash의 stock 즉시 차감
        int newStock = currentStock - request.quantity();
        gameDayStoreStateRedisRepository.updateField(
                store.getId(), day, "stock", String.valueOf(newStock));

        // inflow_rate에 나눔 보너스 곱하기 반영 (보너스 0.05 → ×1.05)
        BigDecimal multiplier = BigDecimal.ONE.add(captureRateBonus);
        applyInflowRateMultiplier(store.getId(), day, multiplier);

        // actionValue에 유입률 보너스 저장 (이력용)
        actionLogRepository.save(new ActionLog(action, store, day, captureRateBonus));
        gameStateRedisRepository.markActionUsed(store.getId(), day, "donation");

        return new DonationResponse(
                request.quantity(),
                captureRateBonus,
                request.quantity() + "개를 나눔하였습니다. 유입률 +" + captureRateBonus + " 보너스."
        );
    }

    @Override
    @Transactional
    public EmergencyOrderResponse executeEmergencyOrder(Integer userId, EmergencyOrderRequest request) {
        Store store = findStore(userId);
        int day = getCurrentDay();

        validateNotUsed(store.getId(), day, "emergency");

        Action action = findSingleAction(ActionCategory.EMERGENCY_ORDER);

        int originPrice = store.getMenu().getOriginPrice();
        int totalCost = BigDecimal.valueOf(originPrice)
                .multiply(BigDecimal.valueOf(request.quantity()))
                .multiply(EMERGENCY_COST_MULTIPLIER)
                .intValue();

        validateBalance(store.getId(), day, totalCost);

        LocalDateTime arrivedTime = LocalDateTime.now().plusSeconds(EMERGENCY_DELIVERY_SECONDS);

        actionLogRepository.save(new ActionLog(action, store, day, null));
        Order order = orderRepository.save(
                Order.createEmergency(store.getMenu(), store, request.quantity(), totalCost, day, arrivedTime)
        );

        gameStateRedisRepository.markActionUsed(store.getId(), day, "emergency");

        return new EmergencyOrderResponse(
                order.getId(),
                request.quantity(),
                totalCost,
                arrivedTime,
                "긴급 발주가 접수되었습니다."
        );
    }

    // ── 공통 헬퍼 ──

    private Store findStore(Integer userId) {
        return storeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));
    }

    private int getCurrentDay() {
        Season season = seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)
                .orElseThrow(() -> new BaseException(ErrorCode.SEASON_NOT_FOUND));
        return season.getCurrentDay();
    }

    private Action findSingleAction(ActionCategory category) {
        return actionRepository.findByCategory(category)
                .stream().findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateNotUsed(Long storeId, int day, String field) {
        if (gameStateRedisRepository.isActionUsed(storeId, day, field)) {
            throw new BaseException(ErrorCode.ACTION_ALREADY_USED);
        }
    }

    private void validateBalance(Long storeId, int day, int requiredAmount) {
        if (requiredAmount <= 0) {
            return;
        }
        GameDayLiveState state = gameDayStoreStateRedisRepository.find(storeId, day)
                .orElseThrow(() -> new BaseException(ErrorCode.GAME_STATE_NOT_FOUND));
        if (state.balance() == null || state.balance() < requiredAmount) {
            throw new BaseException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    // ── 유입률 헬퍼 ──

    private void applyInflowRateMultiplier(Long storeId, int day, BigDecimal multiplier) {
        GameDayLiveState state = gameDayStoreStateRedisRepository.find(storeId, day)
                .orElseThrow(() -> new BaseException(ErrorCode.GAME_STATE_NOT_FOUND));
        BigDecimal currentRate = state.inflowRate() != null ? state.inflowRate() : BigDecimal.ZERO;
        BigDecimal newRate = currentRate.multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
        gameDayStoreStateRedisRepository.updateField(storeId, day, "inflow_rate", newRate.toPlainString());
    }

    // ── 할인 헬퍼 ──

    private PriceRange determinePriceRange(int sellingPrice, int averagePrice) {
        BigDecimal ratio = BigDecimal.valueOf(sellingPrice)
                .divide(BigDecimal.valueOf(averagePrice), 4, RoundingMode.HALF_UP);

        if (ratio.compareTo(PRICE_RANGE_UPPER) > 0) {
            return new PriceRange("ABOVE", MULTIPLIER_ABOVE);
        }
        if (ratio.compareTo(PRICE_RANGE_LOWER) < 0) {
            return new PriceRange("BELOW", MULTIPLIER_BELOW);
        }
        return new PriceRange("AVERAGE", MULTIPLIER_AVERAGE);
    }

    private record PriceRange(String label, BigDecimal multiplier) {
    }
}
