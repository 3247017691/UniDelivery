package com.unidelivery.app.repo;

import com.unidelivery.app.domain.UploadTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UploadTaskRepository extends JpaRepository<UploadTask, Long> {

    Optional<UploadTask> findByAdvertiserIdAndFileMd5(Long advertiserId, String fileMd5);

    List<UploadTask> findByStatus(String status);
}
