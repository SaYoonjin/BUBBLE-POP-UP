package com.ssafy.S14P21A205.user.repository;

import com.ssafy.S14P21A205.user.entity.OAuthIdentity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<OAuthIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
