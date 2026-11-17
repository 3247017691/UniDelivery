package com.unidelivery.app.repo;

import com.unidelivery.app.domain.Advertiser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvertiserRepository extends JpaRepository<Advertiser, Long> {

    List<Advertiser> findByWhitelistedTrue();
}
