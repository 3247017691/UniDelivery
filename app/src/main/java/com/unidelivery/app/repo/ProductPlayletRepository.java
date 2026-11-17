package com.unidelivery.app.repo;

import com.unidelivery.app.domain.ProductPlaylet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductPlayletRepository extends JpaRepository<ProductPlaylet, Long> {

    Optional<ProductPlaylet> findByAdvertiserIdAndAssetId(Long advertiserId, Long assetId);
}
