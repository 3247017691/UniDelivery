package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "aweme_auth")
public class AwemeAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long advertiserId;
    public Long awemeId;
    public String authScenarios;
    public boolean uniAuthorized;
    public LocalDateTime checkedAt;
}
