package com.unidelivery.app.repo;

import com.unidelivery.app.domain.PrecheckRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrecheckRecordRepository extends JpaRepository<PrecheckRecord, Long> {

    List<PrecheckRecord> findByAdvertiserIdAndAwemeIdAndAssetId(Long advertiserId, Long awemeId, Long assetId);
}
