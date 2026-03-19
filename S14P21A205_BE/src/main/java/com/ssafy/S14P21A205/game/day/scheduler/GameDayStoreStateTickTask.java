package com.ssafy.S14P21A205.game.day.scheduler;

import com.ssafy.S14P21A205.game.day.service.GameDayReportService;
import com.ssafy.S14P21A205.game.day.service.GameDayStateService;
import com.ssafy.S14P21A205.game.scheduler.GameTickTask;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.news.service.NewsService;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final NewsService newsService;

    /** 영업 중 뉴스가 이미 생성 요청된 day를 추적 (시즌 내 중복 방지) */
    private final AtomicInteger openingNewsGeneratedDay = new AtomicInteger(-1);

    @Override
    public String taskName() {
        return "gameDayStoreState";
    }

    @Override
    @Transactional
    public void execute() {
        Season season = seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS).orElse(null);
        if (season == null) {
            openingNewsGeneratedDay.set(-1);
            return;
        }

        List<Store> stores = storeRepository.findBySeason_IdOrderByIdAsc(season.getId());
        for (Store store : stores) {
            try {
                gameDayStateService.refreshGameState(store);
            } catch (Exception e) {
                log.error(
                        "Failed to refresh game day store state. seasonId={} storeId={}",
                        season.getId(),
                        store.getId(),
                        e
                );
            }
        }

        int day = season.getCurrentDay() == null ? 1 : season.getCurrentDay();

        // 영업 중 뉴스: 해당 day에 한 번만 비동기 생성
        if (openingNewsGeneratedDay.getAndSet(day) != day) {
            try {
                newsService.generateOpeningNews(season.getId(), day);
            } catch (Exception e) {
                log.error("Failed to generate opening news. seasonId={} day={}", season.getId(), day, e);
            }
        }

        // 마감 뉴스: 랭킹 저장 + 마감 뉴스 1건 비동기 생성
        try {
            newsService.updateDayRankings(season.getId(), day);
        } catch (Exception e) {
            log.error("Failed to update news rankings. seasonId={}", season.getId(), e);
        }
    }
}
