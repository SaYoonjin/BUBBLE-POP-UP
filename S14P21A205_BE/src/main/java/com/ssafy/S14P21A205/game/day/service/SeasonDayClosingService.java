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
@Transactional
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

        for (Store store : stores) {
            gameDayReportService.recordClosedDayReport(store, day);
        }

        // 마감 뉴스: 랭킹 갱신 + 마감 뉴스 1건 생성
        try {
            newsService.updateDayRankings(seasonId, day);
        } catch (Exception e) {
            log.error("Failed to generate closing news. seasonId={} day={}", seasonId, day, e);
        }

        if (day == season.getTotalDays()) {
            seasonFinalRankingService.saveFinalRankings(season);
        }
    }
}
