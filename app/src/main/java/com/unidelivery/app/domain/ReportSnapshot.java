package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_snapshot")
public class ReportSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long advertiserId;
    public String topic;
    /** REALTIME / OFFLINE */
    public String channel;
    public String granularity;
    public LocalDateTime windowStart;
    public Long projectId;
    public Long materialId;
    public String metricsJson;
    public LocalDateTime createdAt;
}
