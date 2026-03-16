package com.ssafy.S14P21A205.action.service;

import com.ssafy.S14P21A205.action.dto.ActionResponse;
import com.ssafy.S14P21A205.action.dto.ActionStatusResponse;
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
    private static final int EMERGENCY_DELIVERY_SECONDS = 30;

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

        actionLogRepository.save(new ActionLog(action, store, day));
        gameStateRedisRepository.markActionUsed(store.getId(), day, field);

        return new ActionResponse(
                "PROMOTION_" + request.promotionType().name(),
                action.getCost(),
                "홍보가 실행되었습니다."
        );
    }

    @Override
    @Transactional
    public ActionResponse executeDiscount(Integer userId) {
        Store store = findStore(userId);
        int day = getCurrentDay();

        validateNotUsed(store.getId(), day, "discount");

        Action action = findSingleAction(ActionCategory.DISCOUNT);

        validateBalance(store.getId(), day, action.getCost());

        actionLogRepository.save(new ActionLog(action, store, day));
        gameStateRedisRepository.markActionUsed(store.getId(), day, "discount");

        return new ActionResponse("DISCOUNT", action.getCost(), "할인 이벤트가 실행되었습니다.");
    }

    @Override
    @Transactional
    public ActionResponse executeDonation(Integer userId) {
        Store store = findStore(userId);
        int day = getCurrentDay();

        validateNotUsed(store.getId(), day, "donation");

        Action action = findSingleAction(ActionCategory.DONATION);

        validateStock(store.getId(), day);

        actionLogRepository.save(new ActionLog(action, store, day));
        gameStateRedisRepository.markActionUsed(store.getId(), day, "donation");

        return new ActionResponse("DONATION", action.getCost(), "나눔 이벤트가 실행되었습니다.");
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

        actionLogRepository.save(new ActionLog(action, store, day));
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

    private void validateStock(Long storeId, int day) {
        GameDayLiveState state = gameDayStoreStateRedisRepository.find(storeId, day)
                .orElseThrow(() -> new BaseException(ErrorCode.GAME_STATE_NOT_FOUND));
        if (state.stock() == null || state.stock() <= 0) {
            throw new BaseException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }
}
