package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "precheck_record")
public class PrecheckRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long advertiserId;
    public Long awemeId;
    public Long assetId;
    public String item;
    public boolean passed;
    public String reason;
    public String evidence;
    public LocalDateTime createdAt;
}
