package com.unidelivery.app.repo;

import com.unidelivery.app.domain.SubscriptionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, Long> {

    Optional<SubscriptionEvent> findByEventId(String eventId);

    List<SubscriptionEvent> findByConsumedFalse();
}
