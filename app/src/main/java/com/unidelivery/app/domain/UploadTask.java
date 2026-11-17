package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "upload_task")
public class UploadTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long advertiserId;
    public String fileMd5;
    public String filename;
    public Long taskId;
    public String videoId;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
