package com.unidelivery.app.service;

import com.unidelivery.app.domain.OpJournal;
import com.unidelivery.app.repo.OpJournalRepository;
import com.unidelivery.mapi.MapiEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 写操作 journal：独立事务（主流程回滚不丢审计痕迹），client_request_id 唯一键承载幂等。
 * 状态：STARTED / OK / PARTIAL / FAILED / ADOPTED（超时后先查后认领）。
 */
@Service
public class JournalService {

    private final OpJournalRepository repository;
    private final TransactionTemplate requiresNew;

    public JournalService(OpJournalRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public OpJournal begin(String clientRequestId, MapiEndpoint endpoint, long advertiserId,
                           Long projectLocalId, String requestSummary) {
        return requiresNew.execute(status -> {
            Optional<OpJournal> existing = repository.findByClientRequestId(clientRequestId);
            if (existing.isPresent()) {
                return existing.get();
            }
            OpJournal row = new OpJournal();
            row.clientRequestId = clientRequestId;
            row.endpoint = endpoint.name();
            row.advertiserId = advertiserId;
            row.projectLocalId = projectLocalId;
            row.requestSummary = requestSummary;
            row.status = "STARTED";
            row.createdAt = LocalDateTime.now();
            row.updatedAt = LocalDateTime.now();
            return repository.save(row);
        });
    }

    public void finish(OpJournal journal, String status, String platformRequestId, String responseSummary) {
        requiresNew.executeWithoutResult(tx -> {
            OpJournal row = repository.findById(journal.id).orElseThrow();
            row.status = status;
            row.platformRequestId = platformRequestId;
            row.responseSummary = responseSummary;
            if (journal.projectLocalId != null) {
                row.projectLocalId = journal.projectLocalId;
            }
            row.updatedAt = LocalDateTime.now();
            repository.save(row);
        });
    }

    public Optional<OpJournal> find(String clientRequestId) {
        return repository.findByClientRequestId(clientRequestId);
    }
}
