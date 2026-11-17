package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "mapi_audit")
public class MapiAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String endpoint;
    public String path;
    public Long advertiserId;
    public String requestSummary;
    public Integer httpStatus;
    public Integer code;
    public String requestId;
    public Long elapsedMs;
    public Integer attempt;
    public String outcome;
    public LocalDateTime createdAt;
}
