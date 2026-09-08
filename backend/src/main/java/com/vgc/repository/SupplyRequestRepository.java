package com.vgc.repository;

import com.vgc.entity.RequestStatus;
import com.vgc.entity.SupplyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupplyRequestRepository extends JpaRepository<SupplyRequest, Long> {
    List<SupplyRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SupplyRequest> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, RequestStatus status);

    List<SupplyRequest> findTop20ByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, RequestStatus status);

    List<SupplyRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    @Query("select r from SupplyRequest r join fetch r.user "
            + "where r.status in (com.vgc.entity.RequestStatus.PURCHASED, com.vgc.entity.RequestStatus.REJECTED) "
            + "order by coalesce(r.purchasedAt, r.rejectedAt, r.updatedAt) desc")
    List<SupplyRequest> findProcessedWithUser();
}
