package com.ssafy.S14P21A205.game.day.scheduler;

import com.ssafy.S14P21A205.game.day.service.GameDayReportService;
import com.ssafy.S14P21A205.game.day.service.GameDayStateService;
import com.ssafy.S14P21A205.game.scheduler.GameTickTask;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@org.springframework.core.annotation.Order(200)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDayStoreStateTickTask implements GameTickTask {

    private static final Logger log = LoggerFactory.getLogger(GameDayStoreStateTickTask.class);

    private final SeasonRepository seasonRepository;
    private final StoreRepository storeRepository;
    private final GameDayStateService gameDayStateService;
    private final GameDayReportService gameDayReportService;

    @Override
    public String taskName() {
        return "gameDayStoreState";
    }

    @Override
    @Transactional
    public void execute() {
        Season season = seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS).orElse(null);
        if (season == null) {
            return;
        }

        List<Store> stores = storeRepository.findBySeason_IdOrderByIdAsc(season.getId());
        for (Store store : stores) {
            try {
                gameDayStateService.refreshGameState(store);
                gameDayReportService.recordClosedDayReport(store);
            } catch (Exception e) {
                log.error(
                        "Failed to refresh game day store state. seasonId={} storeId={}",
                        season.getId(),
                        store.getId(),
                        e
                );
            }
        }
    }
}
