package com.ssafy.S14P21A205.game.season.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.generator.PurchaseListGenerator;
import com.ssafy.S14P21A205.game.season.dto.SeasonJoinRequest;
import com.ssafy.S14P21A205.game.season.dto.SeasonJoinResponse;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.game.season.repository.SeasonRankingRecordRepository;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.LocationRepository;
import com.ssafy.S14P21A205.store.repository.MenuRepository;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonJoinService {

    private static final int INITIAL_CAPITAL = 10_000_000;
    private static final BigDecimal INTERIOR_RATE = new BigDecimal("0.10");
    private static final BigDecimal INITIAL_CAPTURE_RATE = new BigDecimal("0.10");
    private static final int STORE_NAME_MIN_LENGTH = 2;
    private static final int STORE_NAME_MAX_LENGTH = 20;
    private static final String BALANCE_KEY_PREFIX = "balance:";
    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String CAPTURE_RATE_KEY_PREFIX = "captureRate:";
    private static final Pattern STORE_NAME_PATTERN = Pattern.compile("^[\\p{IsHangul}A-Za-z0-9 ]+$");

    private final UserService userService;
    private final SeasonRepository seasonRepository;
    private final SeasonRankingRecordRepository seasonRankingRecordRepository;
    private final DailyReportRepository dailyReportRepository;
    private final StoreRepository storeRepository;
    private final LocationRepository locationRepository;
    private final MenuRepository menuRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final PurchaseListGenerator purchaseListGenerator;

    private final SeasonTimelineService seasonTimelineService = new SeasonTimelineService();
    private final Clock clock;

    @Transactional
    public SeasonJoinResponse joinCurrentSeason(Authentication authentication, SeasonJoinRequest request) {
        validateRequest(request);

        User user = userService.getCurrentUser(authentication);
        Season currentSeason = seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)
                .orElseThrow(() -> new BaseException(ErrorCode.SEASON_NOT_FOUND));

        if (hasActiveStoreInCurrentSeason(user.getId(), currentSeason.getId())) {
            throw new BaseException(ErrorCode.ALREADY_JOINED_CURRENT_SEASON);
        }

        Integer playableFromDay = resolvePlayableFromDay(currentSeason);
        if (playableFromDay == null) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE, "Joining the current season is no longer available.");
        }

        Location location = locationRepository.findById(request.locationId().longValue())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Location was not found."));

        Store previousStore = storeRepository.findFirstByUser_IdOrderBySeason_IdDescIdDesc(user.getId())
                .orElse(null);
        Menu initialMenu = resolveInitialMenu(previousStore);
        Integer initialPrice = resolveInitialPrice(previousStore, initialMenu);
        String normalizedStoreName = request.storeName().trim();

        Store savedStore = storeRepository.save(Store.create(
                user,
                location,
                initialMenu,
                currentSeason,
                normalizedStoreName,
                initialPrice,
                playableFromDay
        ));
        savedStore.initializePurchaseQueue(purchaseListGenerator.issueSeed());

        int interior = calculateInterior(location.getRent());
        int remainingBalance = INITIAL_CAPITAL - interior;

        var valueOperations = stringRedisTemplate.opsForValue();
        valueOperations.set(balanceKey(savedStore.getId()), String.valueOf(remainingBalance));
        valueOperations.set(stockKey(savedStore.getId()), "0");
        valueOperations.set(captureRateKey(savedStore.getId()), INITIAL_CAPTURE_RATE.toPlainString());

        return new SeasonJoinResponse(
                savedStore.getId(),
                savedStore.getStoreName(),
                remainingBalance,
                playableFromDay,
                playableFromDay > 1
        );
    }

    private Integer resolvePlayableFromDay(Season season) {
        return seasonTimelineService.resolveJoinPlayableFromDay(season, LocalDateTime.now(clock));
    }

    private boolean hasActiveStoreInCurrentSeason(Integer userId, Long seasonId) {
        return storeRepository.findFirstByUser_IdAndSeason_IdOrderByIdDesc(userId, seasonId)
                .filter(store -> !seasonRankingRecordRepository.existsByStore_Id(store.getId()))
                .filter(store -> dailyReportRepository.findFirstByStore_IdOrderByDayDesc(store.getId())
                        .map(report -> !Boolean.TRUE.equals(report.getIsBankrupt()))
                        .orElse(true))
                .isPresent();
    }

    private void validateRequest(SeasonJoinRequest request) {
        if (request == null || request.locationId() == null || request.locationId() <= 0) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE, "locationId must be positive.");
        }
        if (request.storeName() == null || request.storeName().trim().isEmpty()) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE, "storeName must not be blank.");
        }

        String normalizedStoreName = request.storeName().trim();
        if (normalizedStoreName.length() < STORE_NAME_MIN_LENGTH || normalizedStoreName.length() > STORE_NAME_MAX_LENGTH) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "storeName length must be between %d and %d.".formatted(STORE_NAME_MIN_LENGTH, STORE_NAME_MAX_LENGTH)
            );
        }
        if (!STORE_NAME_PATTERN.matcher(normalizedStoreName).matches()) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "storeName must contain only Korean, English letters, numbers, and spaces."
            );
        }
    }

    private Menu resolveInitialMenu(Store previousStore) {
        if (previousStore != null && previousStore.getMenu() != null) {
            return previousStore.getMenu();
        }

        return menuRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new BaseException(ErrorCode.MENU_NOT_FOUND));
    }

    private Integer resolveInitialPrice(Store previousStore, Menu menu) {
        if (previousStore != null && previousStore.getPrice() != null) {
            return previousStore.getPrice();
        }
        return menu.getOriginPrice();
    }

    private int calculateInterior(int rent) {
        return BigDecimal.valueOf(rent)
                .multiply(INTERIOR_RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String balanceKey(Long storeId) {
        return BALANCE_KEY_PREFIX + storeId;
    }

    private String stockKey(Long storeId) {
        return STOCK_KEY_PREFIX + storeId;
    }

    private String captureRateKey(Long storeId) {
        return CAPTURE_RATE_KEY_PREFIX + storeId;
    }
}
