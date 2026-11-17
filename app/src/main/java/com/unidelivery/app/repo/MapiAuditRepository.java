package com.unidelivery.app.repo;

import com.unidelivery.app.domain.MapiAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapiAuditRepository extends JpaRepository<MapiAudit, Long> {
}
