package com.unidelivery.app.repo;

import com.unidelivery.app.domain.ReportSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportSnapshotRepository extends JpaRepository<ReportSnapshot, Long> {

    Optional<ReportSnapshot> findByAdvertiserIdAndTopicAndChannelAndGranularityAndWindowStartAndProjectIdAndMaterialId(
            Long advertiserId, String topic, String channel, String granularity,
            LocalDateTime windowStart, Long projectId, Long materialId);

    List<ReportSnapshot> findByAdvertiserIdAndTopicAndChannelAndWindowStartBetween(
            Long advertiserId, String topic, String channel, LocalDateTime from, LocalDateTime to);
}
