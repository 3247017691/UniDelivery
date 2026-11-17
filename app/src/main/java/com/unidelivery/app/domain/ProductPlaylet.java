package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_playlet")
public class ProductPlaylet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long advertiserId;
    public Long assetId;
    public Long playletId;
    public Long albumId;
    public Long orgId;
    public Long platformId;
    public String libraryVersion;
    public boolean deliverable;
    public LocalDateTime checkedAt;
}
