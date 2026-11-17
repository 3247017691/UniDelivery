package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_token")
public class OauthToken {

    @Id
    public Long advertiserId;
    public String accessToken;
    public String refreshToken;
    public LocalDateTime expiresAt;
    public LocalDateTime refreshExpiresAt;
    public LocalDateTime updatedAt;
}
