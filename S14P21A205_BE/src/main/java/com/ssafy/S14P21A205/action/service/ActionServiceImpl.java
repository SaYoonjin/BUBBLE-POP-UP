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
import com.ssafy.S14P21A205.game.day.policy.CaptureRatePolicy;
import com.ssafy.S14P21A205.game.day.resolver.EventEffectResolver;
import com.ssafy.S14P21A205.game.day.resolver.TrafficDelayResolver;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.shop.repository.ItemUserRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
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
    private static final BigDecimal PRICE_RANGE_UPPER = new BigDecimal("1.10");
    private static final BigDecimal PRICE_RANGE_LOWER = new BigDecimal("0.90");
    private static final BigDecimal MULTIPLIER_AVERAGE = new BigDecimal("1.00");
    private static final BigDecimal MULTIPLIER_ABOVE_200 = new BigDecimal("0.01");
    private static final BigDecimal MULTIPLIER_ABOVE_190 = new BigDecimal("0.10");
    private static final BigDecimal MULTIPLIER_ABOVE_180 = new BigDecimal("0.20");
    private static final BigDecimal MULTIPLIER_ABOVE_170 = new BigDecimal("0.30");
    private static final BigDecimal MULTIPLIER_ABOVE_160 = new BigDecimal("0.40");
    private static final BigDecimal MULTIPLIER_ABOVE_150 = new BigDecimal("0.50");
    private static final BigDecimal MULTIPLIER_ABOVE_140 = new BigDecimal("0.60");
    private static final BigDecimal MULTIPLIER_ABOVE_130 = new BigDecimal("0.70");
    private static final BigDecimal MULTIPLIER_ABOVE_120 = new BigDecimal("0.80");
    private static final BigDecimal MULTIPLIER_BELOW_80 = new BigDecimal("1.20");
    private static final BigDecimal MULTIPLIER_BELOW_70 = new BigDecimal("1.30");
    private static final BigDecimal MULTIPLIER_BELOW_60 = new BigDecimal("1.40");
    private static final BigDecimal RATIO_200 = new BigDecimal("2.00");
    private static final BigDecimal RATIO_190 = new BigDecimal("1.90");
    private static final BigDecimal RATIO_180 = new BigDecimal("1.80");
    private static final BigDecimal RATIO_170 = new BigDecimal("1.70");
    private static final BigDecimal RATIO_160 = new BigDecimal("1.60");
    private static final BigDecimal RATIO_150 = new BigDecimal("1.50");
    private static final BigDecimal RATIO_140 = new BigDecimal("1.40");
    private static final BigDecimal RATIO_130 = new BigDecimal("1.30");
    private static final BigDecimal RATIO_120 = new BigDecimal("1.20");
    private static final BigDecimal RATIO_80 = new BigDecimal("0.80");
    private static final BigDecimal RATIO_70 = new BigDecimal("0.70");
    private static final BigDecimal RATIO_60 = new BigDecimal("0.60");
    private static final BigDecimal DONATION_CAPTURE_RATE_BONUS = new BigDecimal("0.10");

    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;
    private final ActionRepository actionRepository;
    private final ActionLogRepository actionLogRepository;
    private final StoreRepository storeRepository;
    private final SeasonRepository seasonRepository;
    private final OrderRepository orderRepository;
    private final ItemUserRepository itemUserRepository;
    private final EventEffectResolver eventEffectResolver;
    private final TrafficDelayResolver trafficDelayResolver;
    private final CaptureRatePolicy captureRatePolicy;
    private final Clock clock;

    @Override
    public ActionStatusResponse getActionStatus(Integer userId) {
        Store store = findStore(userId);
        int day = getCurrentDay();
        Map<String, Boolean> actions = gameDayStoreStateRedisRepository.getActions(store.getId(), day);
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
        long updatedBalance = resolveUpdatedBalance(store.getId(), day, valueOf(action.getCost()));

        BigDecimal multiplier = BigDecimal.ONE.add(action.getCaptureRate());
        applyCaptureRateMultiplier(store.getId(), day, multiplier);

        actionLogRepository.save(new ActionLog(action, store, day, null));
        gameDayStoreStateRedisRepository.markActionUsed(store.getId(), day, field);
        gameDayStoreStateRedisRepository.saveBalance(store.getId(), day, updatedBalance);

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
        int previousPrice = store.getPrice();
        int newPrice = previousPrice - request.discountValue();

        if (newPrice < store.getMenu().getOriginPrice()) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }

        long updatedBalance = resolveUpdatedBalance(store.getId(), day, valueOf(action.getCost()));

        store.changePrice(newPrice);

        int averagePrice = storeRepository.findAveragePriceBySeasonIdAndMenuId(
                store.getSeason().getId(),
                store.getMenu().getId()
        );
        if (averagePrice <= 0) {
            averagePrice = store.getMenu().getOriginPrice();
        }
        PriceRange priceRange = determinePriceRange(newPrice, averagePrice);

        gameDayStoreStateRedisRepository.updateField(store.getId(), day, "sale_price", String.valueOf(newPrice));
        applyCaptureRateMultiplier(store.getId(), day, priceRange.multiplier);

        actionLogRepository.save(new ActionLog(action, store, day, priceRange.multiplier));
        gameDayStoreStateRedisRepository.markActionUsed(store.getId(), day, "discount");
        gameDayStoreStateRedisRepository.saveBalance(store.getId(), day, updatedBalance);

        return new DiscountResponse(
                previousPrice,
                newPrice,
                priceRange.label,
                priceRange.multiplier,
                "할인 이벤트가 실행되었습니다. 판매가가 " + previousPrice + "원에서 " + newPrice + "원으로 변경됩니다."
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

        long updatedBalance = resolveUpdatedBalance(store.getId(), day, valueOf(action.getCost()));
        BigDecimal captureRateBonus = DONATION_CAPTURE_RATE_BONUS.setScale(2, RoundingMode.HALF_UP);

        int newStock = currentStock - request.quantity();
        gameDayStoreStateRedisRepository.updateField(store.getId(), day, "stock", String.valueOf(newStock));

        BigDecimal multiplier = BigDecimal.ONE.add(captureRateBonus);
        applyCaptureRateMultiplier(store.getId(), day, multiplier);

        actionLogRepository.save(new ActionLog(action, store, day, captureRateBonus));
        gameDayStoreStateRedisRepository.markActionUsed(store.getId(), day, "donation");
        gameDayStoreStateRedisRepository.saveBalance(store.getId(), day, updatedBalance);

        return new DonationResponse(
                request.quantity(),
                captureRateBonus,
                request.quantity() + "개를 기부했습니다. 유입률 +" + captureRateBonus + " 보너스"
        );
    }

    @Override
    @Transactional
    public EmergencyOrderResponse executeEmergencyOrder(Integer userId, EmergencyOrderRequest request) {
        Store store = findStore(userId);
        int day = getCurrentDay();

        validateNotUsed(store.getId(), day, "emergency");

        Action action = findSingleAction(ActionCategory.EMERGENCY_ORDER);
        GameDayLiveState state = gameDayStoreStateRedisRepository.find(store.getId(), day)
                .orElseThrow(() -> new BaseException(ErrorCode.GAME_STATE_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);

        BigDecimal ingredientCostMultiplier = resolveIngredientCostMultiplier(store, day, state, now);
        int adjustedOriginPrice = BigDecimal.valueOf(store.getMenu().getOriginPrice())
                .multiply(resolveIngredientDiscountRate(store.getUser().getId()))
                .multiply(ingredientCostMultiplier)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        int totalCost = BigDecimal.valueOf(adjustedOriginPrice)
                .multiply(BigDecimal.valueOf(request.quantity()))
                .multiply(EMERGENCY_COST_MULTIPLIER)
                .intValue();

        long updatedBalance = resolveUpdatedBalance(
                store.getId(),
                day,
                valueOf(action.getCost()) + totalCost
        );
        int deliverySeconds = trafficDelayResolver.resolve(
                store.getLocation().getId(),
                day,
                store.getSeason().getTotalDays(),
                state.startedAt(),
                now
        ).delaySeconds();
        LocalDateTime arrivedTime = now.plusSeconds(deliverySeconds);

        actionLogRepository.save(new ActionLog(action, store, day, null));
        Order order = orderRepository.save(
                Order.createEmergency(store.getMenu(), store, request.quantity(), totalCost, day, arrivedTime)
        );

        gameDayStoreStateRedisRepository.markActionUsed(store.getId(), day, "emergency");
        gameDayStoreStateRedisRepository.saveBalance(store.getId(), day, updatedBalance);

        return new EmergencyOrderResponse(
                order.getId(),
                request.quantity(),
                totalCost,
                arrivedTime,
                "긴급 발주가 접수되었습니다."
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
                .stream()
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateNotUsed(Long storeId, int day, String field) {
        if (gameDayStoreStateRedisRepository.isActionUsed(storeId, day, field)) {
            throw new BaseException(ErrorCode.ACTION_ALREADY_USED);
        }
    }

    private long resolveUpdatedBalance(Long storeId, int day, long requiredAmount) {
        GameDayLiveState state = gameDayStoreStateRedisRepository.find(storeId, day)
                .orElseThrow(() -> new BaseException(ErrorCode.GAME_STATE_NOT_FOUND));
        long currentBalance = state.balance() == null ? 0L : state.balance();
        long normalizedRequiredAmount = Math.max(requiredAmount, 0L);
        if (currentBalance < normalizedRequiredAmount) {
            throw new BaseException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        return currentBalance - normalizedRequiredAmount;
    }

    private void applyCaptureRateMultiplier(Long storeId, int day, BigDecimal multiplier) {
        GameDayLiveState state = gameDayStoreStateRedisRepository.find(storeId, day)
                .orElseThrow(() -> new BaseException(ErrorCode.GAME_STATE_NOT_FOUND));
        BigDecimal currentRate = state.captureRate() != null
                ? state.captureRate()
                : state.startResponse() != null && state.startResponse().captureRate() != null
                ? state.startResponse().captureRate()
                : BigDecimal.ZERO;
        BigDecimal newRate = captureRatePolicy.applyMultiplier(currentRate, multiplier);
        gameDayStoreStateRedisRepository.updateField(storeId, day, "capture_rate", newRate.toPlainString());
    }

    private PriceRange determinePriceRange(int sellingPrice, int averagePrice) {
        BigDecimal ratio = BigDecimal.valueOf(sellingPrice)
                .divide(BigDecimal.valueOf(averagePrice), 4, RoundingMode.HALF_UP);

        if (ratio.compareTo(PRICE_RANGE_UPPER) > 0) {
            return new PriceRange("ABOVE", resolveAboveMultiplier(ratio));
        }
        if (ratio.compareTo(PRICE_RANGE_LOWER) < 0) {
            return new PriceRange("BELOW", resolveBelowMultiplier(ratio));
        }
        return new PriceRange("AVERAGE", MULTIPLIER_AVERAGE);
    }

    private BigDecimal resolveAboveMultiplier(BigDecimal ratio) {
        if (ratio.compareTo(RATIO_200) >= 0) {
            return MULTIPLIER_ABOVE_200;
        }
        if (ratio.compareTo(RATIO_190) >= 0) {
            return MULTIPLIER_ABOVE_190;
        }
        if (ratio.compareTo(RATIO_180) >= 0) {
            return MULTIPLIER_ABOVE_180;
        }
        if (ratio.compareTo(RATIO_170) >= 0) {
            return MULTIPLIER_ABOVE_170;
        }
        if (ratio.compareTo(RATIO_160) >= 0) {
            return MULTIPLIER_ABOVE_160;
        }
        if (ratio.compareTo(RATIO_150) >= 0) {
            return MULTIPLIER_ABOVE_150;
        }
        if (ratio.compareTo(RATIO_140) >= 0) {
            return MULTIPLIER_ABOVE_140;
        }
        if (ratio.compareTo(RATIO_130) >= 0) {
            return MULTIPLIER_ABOVE_130;
        }
        return MULTIPLIER_ABOVE_120;
    }

    private BigDecimal resolveBelowMultiplier(BigDecimal ratio) {
        if (ratio.compareTo(RATIO_60) < 0) {
            return MULTIPLIER_BELOW_60;
        }
        if (ratio.compareTo(RATIO_70) < 0) {
            return MULTIPLIER_BELOW_70;
        }
        if (ratio.compareTo(RATIO_80) < 0) {
            return MULTIPLIER_BELOW_80;
        }
        return MULTIPLIER_BELOW_80;
    }

    private BigDecimal resolveIngredientCostMultiplier(
            Store store,
            int day,
            GameDayLiveState state,
            LocalDateTime now
    ) {
        if (state.startedAt() == null) {
            return BigDecimal.ONE;
        }

        return eventEffectResolver.resolve(
                store.getSeason().getId(),
                day,
                store.getSeason().getTotalDays(),
                state.startedAt(),
                now,
                store.getLocation().getId(),
                store.getMenu().getId()
        ).ingredientCostMultiplier();
    }

    private BigDecimal resolveIngredientDiscountRate(Integer userId) {
        return itemUserRepository.findPurchasedDiscountRateByUserIdAndCategory(userId, ItemCategory.INGREDIENT)
                .filter(rate -> rate.signum() > 0)
                .orElse(BigDecimal.ONE);
    }

    private long valueOf(Integer amount) {
        return amount == null ? 0L : amount.longValue();
    }

    private record PriceRange(String label, BigDecimal multiplier) {
    }
}
