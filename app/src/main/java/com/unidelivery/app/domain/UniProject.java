package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "uni_project")
public class UniProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long advertiserId;
    public Long projectId;
    public Long awemeId;
    public Long assetId;
    /** 1=活跃（在投/审核中等未终态），NULL=终态；唯一索引 uk_target 落点。 */
    public Integer activeFlag;
    @Enumerated(EnumType.STRING)
    public Stage stage;
    public String status;
    public String monetizationMode;
    public BigDecimal budget;
    public BigDecimal bid;
    public String clientRequestId;
    public boolean confirmedConflict;
    public String rejectReasonJson;
    public String lastError;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public enum Stage { PRECHECK, CREATING, REVIEWING, ACTIVE, PAUSED, FAILED, TERMINATED }

    public boolean terminal() {
        return stage == Stage.TERMINATED || stage == Stage.FAILED;
    }
}
