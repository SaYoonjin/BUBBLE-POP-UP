package com.ssafy.S14P21A205.action.service;

import com.ssafy.S14P21A205.action.dto.ActionStatusResponse;
import com.ssafy.S14P21A205.action.dto.PromotionPriceResponse;
import com.ssafy.S14P21A205.action.entity.Action;
import com.ssafy.S14P21A205.action.entity.ActionCategory;
import com.ssafy.S14P21A205.action.repository.ActionRepository;
import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.state.repository.GameStateRedisRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionServiceImpl implements ActionService {

    private final GameStateRedisRepository gameStateRedisRepository;
    private final ActionRepository actionRepository;
    private final StoreRepository storeRepository;
    private final SeasonRepository seasonRepository;


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


    private Store findStore(Integer userId) {
        return storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(userId, SeasonStatus.IN_PROGRESS)
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));
    }

    private int getCurrentDay() {
        Season season = seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)
                .orElseThrow(() -> new BaseException(ErrorCode.SEASON_NOT_FOUND));
        return season.getCurrentDay();
    }
}
