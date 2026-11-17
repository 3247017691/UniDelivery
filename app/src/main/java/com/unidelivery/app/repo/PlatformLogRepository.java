package com.unidelivery.app.repo;

import com.unidelivery.app.domain.PlatformLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlatformLogRepository extends JpaRepository<PlatformLog, Long> {

    Optional<PlatformLog> findByAdvertiserIdAndObjectIdAndOpTimeAndContentTitle(
            Long advertiserId, Long objectId, LocalDateTime opTime, String contentTitle);

    List<PlatformLog> findByAdvertiserIdAndOpTimeBetween(Long advertiserId, LocalDateTime from, LocalDateTime to);
}
