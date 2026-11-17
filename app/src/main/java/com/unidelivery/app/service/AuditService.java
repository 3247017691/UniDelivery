package com.unidelivery.app.service;

import com.unidelivery.app.domain.MapiAudit;
import com.unidelivery.app.repo.MapiAuditRepository;
import com.unidelivery.mapi.AuditSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * 请求审计：独立事务落库（主流程回滚不丢审计）+ 结构化日志。
 */
@Service
public class AuditService implements AuditSink {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final MapiAuditRepository repository;
    private final TransactionTemplate requiresNew;

    public AuditService(MapiAuditRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void audit(AuditEvent event) {
        log.info("mapi-audit endpoint={} adv={} http={} code={} attempt={} outcome={} request_id={} elapsed_ms={}",
                event.endpoint(), event.advertiserId(), event.httpStatus(), event.code(),
                event.attempt(), event.outcome(), event.requestId(), event.elapsedMs());
        requiresNew.executeWithoutResult(status -> {
            MapiAudit row = new MapiAudit();
            row.endpoint = event.endpoint().name();
            row.path = event.path();
            row.advertiserId = event.advertiserId();
            row.requestSummary = event.requestSummary();
            row.httpStatus = event.httpStatus();
            row.code = event.code();
            row.requestId = event.requestId();
            row.elapsedMs = event.elapsedMs();
            row.attempt = event.attempt();
            row.outcome = event.outcome();
            row.createdAt = LocalDateTime.now();
            repository.save(row);
        });
    }
}
