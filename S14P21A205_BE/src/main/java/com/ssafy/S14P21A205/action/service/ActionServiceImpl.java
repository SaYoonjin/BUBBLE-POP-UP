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
    // TODO: Replace fixed 30-second emergency delivery time with traffic-based calculation.
    private static final int EMERGENCY_DELIVERY_SECONDS = 30;

    private static final BigDecimal PRICE_RANGE_UPPER = new BigDecimal("1.10");
    private static final BigDecimal PRICE_RANGE_LOWER = new BigDecimal("0.90");
    private static final BigDecimal MULTIPLIER_ABOVE = new BigDecimal("0.80");
    private static final BigDecimal MULTIPLIER_AVERAGE = new BigDecimal("1.00");
    private static final BigDecimal MULTIPLIER_BELOW = new BigDecimal("1.20");

    private static final BigDecimal DONATION_BONUS_PER_UNIT = new BigDecimal("0.01");
    private static final int DONATION_UNIT_SIZE = 5;

    private final GameStateRedisRepository gameStateRedisRepository;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;
    private final ActionRepository actionRepository;
    private final ActionLogRepository actionLogRepository;
    private final StoreRepository storeRepository;
    private final SeasonRepository seasonRepository;
    private final OrderRepository orderRepository;

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

        BigDecimal multiplier = BigDecimal.ONE.add(action.getCaptureRate());
        applyInflowRateMultiplier(store.getId(), day, multiplier);

        actionLogRepository.save(new ActionLog(action, store, day, null));
        gameStateRedisRepository.markActionUsed(store.getId(), day, field);

        return new ActionResponse(
                "PROMOTION_" + request.promotionType().name(),
                action.getCost(),
                "Promotion has been executed."
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

        int previousPrice = store.getPrice();
        int newPrice = previousPrice - request.discountValue();

        if (newPrice <= 0) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }

        store.changePrice(newPrice);

        int averagePrice = store.getMenu().getOriginPrice();
        PriceRange priceRange = determinePriceRange(newPrice, averagePrice);

        gameDayStoreStateRedisRepository.updateField(
                store.getId(), day, "sale_price", String.valueOf(newPrice));

        applyInflowRateMultiplier(store.getId(), day, priceRange.multiplier);

        actionLogRepository.save(new ActionLog(action, store, day, priceRange.multiplier));
        gameStateRedisRepository.markActionUsed(store.getId(), day, "discount");

        return new DiscountResponse(
                previousPrice,
                newPrice,
                priceRange.label,
                priceRange.multiplier,
                "Discount event executed. Price changed from " + previousPrice + " to " + newPrice + "."
        );
    }

    @Override
    @Transactional
    public DonationResponse executeDonation(Integer userId, DonationRequest request) {
        Store store = findStore(userId);
        int day = getCurrentDay();

        validateNotUsed(store.getId(), day, "donation");

        Action action = findSingleAction(ActionCategory.DONATION);

        GameDayLiveState state = gameDayStoreStateRedisRepository.find(store.getId(), day)
                .orElseThrow(() -> new BaseException(ErrorCode.GAME_STATE_NOT_FOUND));
        int currentStock = state.stock() == null ? 0 : state.stock();
        if (currentStock < request.quantity()) {
            throw new BaseException(ErrorCode.INSUFFICIENT_STOCK);
        }

        int bonusUnits = request.quantity() / DONATION_UNIT_SIZE;
        BigDecimal captureRateBonus = DONATION_BONUS_PER_UNIT
                .multiply(BigDecimal.valueOf(bonusUnits))
                .setScale(2, RoundingMode.HALF_UP);

        int newStock = currentStock - request.quantity();
        gameDayStoreStateRedisRepository.updateField(
                store.getId(), day, "stock", String.valueOf(newStock));

        BigDecimal multiplier = BigDecimal.ONE.add(captureRateBonus);
        applyInflowRateMultiplier(store.getId(), day, multiplier);

        actionLogRepository.save(new ActionLog(action, store, day, captureRateBonus));
        gameStateRedisRepository.markActionUsed(store.getId(), day, "donation");

        return new DonationResponse(
                request.quantity(),
                captureRateBonus,
                request.quantity() + " items donated. Inflow bonus +" + captureRateBonus + "."
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
                "Emergency order has been placed."
        );
    }

    private Store findStore(Integer userId) {
        return storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(userId, SeasonStatus.IN_PROGRESS)
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

    private void applyInflowRateMultiplier(Long storeId, int day, BigDecimal multiplier) {
        GameDayLiveState state = gameDayStoreStateRedisRepository.find(storeId, day)
                .orElseThrow(() -> new BaseException(ErrorCode.GAME_STATE_NOT_FOUND));
        BigDecimal currentRate = state.inflowRate() != null ? state.inflowRate() : BigDecimal.ZERO;
        BigDecimal newRate = currentRate.multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
        gameDayStoreStateRedisRepository.updateField(storeId, day, "inflow_rate", newRate.toPlainString());
    }

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
