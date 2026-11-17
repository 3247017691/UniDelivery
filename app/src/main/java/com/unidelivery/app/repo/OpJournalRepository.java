package com.unidelivery.app.repo;

import com.unidelivery.app.domain.OpJournal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OpJournalRepository extends JpaRepository<OpJournal, Long> {

    Optional<OpJournal> findByClientRequestId(String clientRequestId);

    List<OpJournal> findByStatus(String status);
}
