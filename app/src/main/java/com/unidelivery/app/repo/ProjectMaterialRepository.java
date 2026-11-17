package com.unidelivery.app.repo;

import com.unidelivery.app.domain.ProjectMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMaterialRepository extends JpaRepository<ProjectMaterial, Long> {

    List<ProjectMaterial> findByProjectLocalId(Long projectLocalId);

    Optional<ProjectMaterial> findByProjectLocalIdAndVideoId(Long projectLocalId, String videoId);
}
