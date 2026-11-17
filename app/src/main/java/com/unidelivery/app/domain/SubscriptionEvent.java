package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_event")
public class SubscriptionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String eventId;
    public String eventType;
    public String payloadJson;
    public boolean consumed;
    public LocalDateTime receivedAt;
    public LocalDateTime consumedAt;
}
