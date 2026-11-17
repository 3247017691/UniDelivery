package com.unidelivery.app.repo;

import com.unidelivery.app.domain.AwemeAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AwemeAuthRepository extends JpaRepository<AwemeAuth, Long> {

    Optional<AwemeAuth> findByAdvertiserIdAndAwemeId(Long advertiserId, Long awemeId);
}
