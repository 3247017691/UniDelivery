package com.unidelivery.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_material")
public class ProjectMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long projectLocalId;
    public String videoId;
    public Long materialId;
    public String status;
    public String optStatus;
    public String labels;
    public Integer batchNo;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
