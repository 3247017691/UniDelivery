package com.unidelivery.app.repo;

import com.unidelivery.app.domain.OauthToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OauthTokenRepository extends JpaRepository<OauthToken, Long> {
}
