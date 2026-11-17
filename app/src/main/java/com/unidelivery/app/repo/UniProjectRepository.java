package com.unidelivery.app.repo;

import com.unidelivery.app.domain.UniProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UniProjectRepository extends JpaRepository<UniProject, Long> {

    Optional<UniProject> findByClientRequestId(String clientRequestId);

    Optional<UniProject> findByProjectId(Long projectId);

    Optional<UniProject> findByAwemeIdAndAssetIdAndActiveFlag(Long awemeId, Long assetId, Integer activeFlag);

    List<UniProject> findByAdvertiserIdAndStageIn(Long advertiserId, List<UniProject.Stage> stages);

    List<UniProject> findByStageIn(List<UniProject.Stage> stages);

    List<UniProject> findByAdvertiserId(Long advertiserId);
}
