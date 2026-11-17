package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_log")
public class PlatformLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long advertiserId;
    public Long objectId;
    public String objectType;
    public String contentTitle;
    public Long secondLogId;
    public String detailJson;
    public LocalDateTime opTime;
    public LocalDateTime syncedAt;
}
