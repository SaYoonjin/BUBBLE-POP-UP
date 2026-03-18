package com.ssafy.S14P21A205.user.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.season.entity.SeasonRankingRecord;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRankingRecordRepository;
import com.ssafy.S14P21A205.user.dto.UserRecordResponse;
import com.ssafy.S14P21A205.user.dto.UserRecordsResponse;
import com.ssafy.S14P21A205.user.entity.OAuthIdentity;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.repository.OAuthIdentityRepository;
import com.ssafy.S14P21A205.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 용도: 이메일 기준 사용자 생성/조회 및 닉네임 변경 처리. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private record OAuthProfile(
            String provider,
            String providerUserId,
            String email,
            String nickname
    ) {}

    private final UserRepository userRepository;
    private final OAuthIdentityRepository oauthIdentityRepository;
    private final SeasonRankingRecordRepository seasonRankingRecordRepository;

    /** 용도: OAuth 인증 기준 사용자 upsert. */
    @Transactional
    public User upsertFromAuthentication(OAuth2AuthenticationToken authenticationToken) {
        OAuthProfile profile = extractProfile(authenticationToken);
        if (!StringUtils.hasText(profile.email())) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }

        User user = oauthIdentityRepository.findByProviderAndProviderUserId(profile.provider(), profile.providerUserId())
                .map(OAuthIdentity::getUser)
                .orElseGet(() -> createOrLinkUser(profile));
        return loadUser(user);
    }

    /** 용도: 이메일 기준 닉네임 변경. */
    @Transactional
    public User changeNickname(String rawEmail, String rawNickname) {
        String email = normalizeEmail(rawEmail);
        if (!StringUtils.hasText(email)) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String nickname = rawNickname == null ? null : rawNickname.trim();
        if (!StringUtils.hasText(nickname) || nickname.length() > 30) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createOrGet(email, nickname));
        user.changeNickname(nickname);
        return user;
    }

    /** 용도: 현재 인증 사용자 조회. */
    public User getCurrentUser(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken) {
            return upsertFromAuthentication(oauth2AuthenticationToken);
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return getRequiredUser(jwtAuthenticationToken.getToken().getSubject());
        }
        throw new BaseException(ErrorCode.UNAUTHORIZED);
    }

    /** 용도: 인증 사용자 기준 사용자 조회. */
    public User getUser(Authentication authentication) {
        return getCurrentUser(authentication);
    }

    public UserRecordsResponse getUserRecords(Authentication authentication) {
        User user = getCurrentUser(authentication);

        return new UserRecordsResponse(
                seasonRankingRecordRepository
                        .findTop10ByStore_User_IdAndStore_Season_StatusOrderByStore_Season_IdDesc(
                                user.getId(),
                                SeasonStatus.FINISHED
                        )
                        .stream()
                        .map(this::toUserRecordResponse)
                        .toList()
        );
    }

    /** 용도: 인증 사용자 기준 닉네임 변경. */
    @Transactional
    public User changeNickname(Authentication authentication, String rawNickname) {
        User user = getCurrentUser(authentication);
        String nickname = rawNickname == null ? null : rawNickname.trim();
        if (!StringUtils.hasText(nickname) || nickname.length() > 30) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }
        user.changeNickname(nickname);
        return user;
    }

    private OAuthProfile extractProfile(OAuth2AuthenticationToken authenticationToken) {
        if (authenticationToken == null) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
        OAuth2User oauth2User = authenticationToken.getPrincipal();
        if (oauth2User == null) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }

        String provider = normalizeProvider(authenticationToken.getAuthorizedClientRegistrationId());
        String providerUserId = extractProviderUserId(oauth2User);
        if (!StringUtils.hasText(provider) || !StringUtils.hasText(providerUserId)) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }

        String email = normalizeEmail(asString(oauth2User.getAttribute("email")));
        String nickname = resolveInitialNickname(oauth2User, email);
        return new OAuthProfile(provider, providerUserId, email, nickname);
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail == null ? null : rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeProvider(String provider) {
        return provider == null ? null : provider.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveInitialNickname(OAuth2User oauth2User, String email) {
        String profileName = asString(oauth2User == null ? null : oauth2User.getAttribute("name"));
        if (StringUtils.hasText(profileName)) {
            return profileName.trim();
        }

        if (!StringUtils.hasText(email)) {
            return null;
        }

        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            String localPart = email.substring(0, atIndex);
            if (StringUtils.hasText(localPart)) {
                return localPart.trim();
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String extractProviderUserId(OAuth2User oauth2User) {
        String userId = asString(oauth2User.getAttribute("userId"));
        if (StringUtils.hasText(userId)) {
            return userId;
        }
        String sub = asString(oauth2User.getAttribute("sub"));
        if (StringUtils.hasText(sub)) {
            return sub;
        }
        String id = asString(oauth2User.getAttribute("id"));
        if (StringUtils.hasText(id)) {
            return id;
        }
        String name = oauth2User.getName();
        return StringUtils.hasText(name) ? name : null;
    }

    private User createOrLinkUser(OAuthProfile profile) {
        User user = userRepository.findByEmail(profile.email())
                .orElseGet(() -> createOrGet(profile.email(), profile.nickname()));
        return linkIdentity(user, profile);
    }

    private User linkIdentity(User user, OAuthProfile profile) {
        try {
            oauthIdentityRepository.save(new OAuthIdentity(user, profile.provider(), profile.providerUserId()));
            return user;
        } catch (DataIntegrityViolationException e) {
            return oauthIdentityRepository.findByProviderAndProviderUserId(profile.provider(), profile.providerUserId())
                    .map(OAuthIdentity::getUser)
                    .orElseThrow(() -> e);
        }
    }

    private User createOrGet(String email, String nickname) {
        try {
            return userRepository.save(new User(email, nickname));
        } catch (DataIntegrityViolationException e) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> e);
        }
    }

    private User loadUser(User user) {
        if (user == null || user.getId() == null) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.UNAUTHORIZED));
    }

    private UserRecordResponse toUserRecordResponse(SeasonRankingRecord record) {
        var store = record.getStore();
        return new UserRecordResponse(
                Math.toIntExact(store.getSeason().getId()),
                Boolean.TRUE.equals(record.getIsBankruptcy()) ? null : record.getFinalRank(),
                store.getLocation().getLocationName(),
                store.getStoreName(),
                record.getTotalNetProfit(),
                record.getRewardPoints()
        );
    }

    private User getRequiredUser(String rawUserId) {
        if (!StringUtils.hasText(rawUserId)) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
        try {
            Integer userId = Integer.parseInt(rawUserId);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new BaseException(ErrorCode.UNAUTHORIZED));
        } catch (NumberFormatException e) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
    }
}
