package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "op_journal")
public class OpJournal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String clientRequestId;
    public String endpoint;
    public Long advertiserId;
    public Long projectLocalId;
    public String requestSummary;
    /** STARTED / OK / FAILED / ADOPTED */
    public String status;
    public String platformRequestId;
    public String responseSummary;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
