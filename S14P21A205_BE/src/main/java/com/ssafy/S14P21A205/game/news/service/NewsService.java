package com.ssafy.S14P21A205.game.news.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.game.news.dto.MenuMentionCount;
import com.ssafy.S14P21A205.game.news.repository.NewsReportRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 뉴스 생성 오케스트레이터.
 * Spark ETL(트랜잭션 밖)과 DB 저장(NewsDataSaver, 트랜잭션 안)을 분리.
 */
@Service
@RequiredArgsConstructor
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private final SparkNewsDataService sparkNewsDataService;
    private final NewsDataSaver newsDataSaver;
    private final NewsReportRepository newsReportRepository;
    private final SeasonRepository seasonRepository;

    /**
     * 시즌 뉴스 생성 (Spark ETL → DB 저장 + AI 호출).
     * Spark ETL은 트랜잭션 밖에서 실행하고,
     * DB 저장은 NewsDataSaver를 통해 별도 트랜잭션으로 처리.
     */
    public void generateSeasonNews(Long seasonId) {
        if (newsReportRepository.existsBySeasonId(seasonId)) {
            log.info("News already generated for season {}", seasonId);
            return;
        }

        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BaseException(ErrorCode.SEASON_NOT_FOUND));
        int totalDays = season.getTotalDays();

        // Spark ETL 실행 (트랜잭션 밖 — 1~2분 소요)
        log.info("[NEWS] Step 1/4: Running Spark ETL for season {}", seasonId);
        sparkNewsDataService.runNewsEtl();
        log.info("[NEWS] Step 2/4: Reading menu mentions for {} days", totalDays);
        Map<Integer, List<MenuMentionCount>> dayMentions =
                sparkNewsDataService.getMenuMentionsForDays(totalDays);
        log.info("[NEWS] Step 2/4 done: got mentions for {} days", dayMentions.size());

        // DB 저장 + AI 호출 (별도 빈 → @Transactional 프록시 정상 동작)
        newsDataSaver.saveNewsData(seasonId, season, totalDays, dayMentions);
    }

    /**
     * 시즌 준비 시간에 뉴스를 미리 만들면 이벤트가 없어서 축제 예고가 빠짐.
     * IN_PROGRESS 전환 후 이벤트 빌드 완료되면 이 메서드로 축제 예고만 보충.
     */
    public void generateEventPreviewNewsIfMissing(Long seasonId) {
        Season season = seasonRepository.findById(seasonId).orElse(null);
        if (season == null) return;
        newsDataSaver.generateMissingEventPreviewNews(seasonId, season.getTotalDays());
    }

    /**
     * 영업 마감 시 당일 순위 업데이트 + 마감 뉴스 1건 생성.
     */
    public void updateDayRankings(Long seasonId, int day) {
        newsDataSaver.updateDayRankings(seasonId, day);
    }

    /**
     * 영업 중 뉴스 생성 (메뉴 입점수 + 지역 입점수).
     */
    public void generateOpeningNews(Long seasonId, int day) {
        newsDataSaver.generateOpeningNews(seasonId, day);
    }
}
