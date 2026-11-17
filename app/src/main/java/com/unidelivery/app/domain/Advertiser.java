package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "advertiser")
public class Advertiser {

    @Id
    public Long advertiserId;
    public String name;
    public boolean whitelisted;
    public String whitelistEvidence;
    public String brandInfoJson;
    public LocalDateTime brandCheckedAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
