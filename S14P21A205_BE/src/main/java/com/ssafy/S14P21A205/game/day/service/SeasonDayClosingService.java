package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.game.news.service.NewsService;
import com.ssafy.S14P21A205.game.season.service.SeasonFinalRankingService;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeasonDayClosingService {

    private final SeasonRepository seasonRepository;
    private final StoreRepository storeRepository;
    private final GameDayReportService gameDayReportService;
    private final SeasonFinalRankingService seasonFinalRankingService;
    private final NewsService newsService;

    public void handleBusinessEnd(Long seasonId, int day) {
        if (seasonId == null || day < 1) {
            return;
        }

        Season season = seasonRepository.findByIdAndStatus(seasonId, SeasonStatus.IN_PROGRESS).orElse(null);
        if (season == null || season.getTotalDays() == null || day > season.getTotalDays()) {
            return;
        }

        List<Store> stores = storeRepository.findBySeason_IdOrderByIdAsc(seasonId);
        if (stores.isEmpty()) {
            log.info("Skipping day closing. seasonId={} day={} reason=no_stores", seasonId, day);
            return;
        }

        // 각 store의 recordClosedDayReport는 자체 @Transactional로 즉시 커밋
        for (Store store : stores) {
            gameDayReportService.recordClosedDayReport(store, day);
        }

        if (day == season.getTotalDays()) {
            seasonFinalRankingService.saveFinalRankings(season);
        }

        // daily_report 커밋 완료 후 뉴스 생성 (AI 호출 포함, 별도 트랜잭션)
        try {
            newsService.updateDayRankings(seasonId, day);
        } catch (Exception e) {
            log.error("Failed to generate closing news. seasonId={} day={}", seasonId, day, e);
        }
    }
}
