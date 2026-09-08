package com.vgc.repository;

import com.vgc.entity.CatalogSyncRun;
import com.vgc.entity.CatalogSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CatalogSyncRunRepository extends JpaRepository<CatalogSyncRun, Long> {
    Optional<CatalogSyncRun> findFirstByStatusOrderByStartedAtDesc(CatalogSyncStatus status);

    Optional<CatalogSyncRun> findFirstByStatusOrderByFinishedAtDesc(CatalogSyncStatus status);

    boolean existsByStatus(CatalogSyncStatus status);
}
