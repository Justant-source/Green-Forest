package com.vgc.repository;

import com.vgc.entity.RequestStatus;
import com.vgc.entity.SnackRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SnackRequestRepository extends JpaRepository<SnackRequest, Long> {
    Optional<SnackRequest> findByUserIdAndCatalogItemIdAndStatus(Long userId, Long catalogItemId, RequestStatus status);

    List<SnackRequest> findByCatalogItemIdAndStatus(Long catalogItemId, RequestStatus status);

    List<SnackRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SnackRequest> findByUserIdAndStatus(Long userId, RequestStatus status);

    List<SnackRequest> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, RequestStatus status);

    List<SnackRequest> findTop20ByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, RequestStatus status);

    @Query("select r.catalogItem.id, coalesce(sum(r.quantity), 0) from SnackRequest r "
            + "where r.status = com.vgc.entity.RequestStatus.PENDING group by r.catalogItem.id")
    List<Object[]> sumPendingByItem();

    @Query("select coalesce(sum(r.quantity), 0) from SnackRequest r "
            + "where r.catalogItem.id = :catalogItemId and r.status = com.vgc.entity.RequestStatus.PENDING")
    long sumPendingQtyByCatalogItemId(@Param("catalogItemId") Long catalogItemId);

    @Query("select distinct r from SnackRequest r join fetch r.catalogItem join fetch r.user "
            + "where r.status = com.vgc.entity.RequestStatus.PENDING")
    List<SnackRequest> findPendingWithItemAndUser();

    @Query("select distinct r from SnackRequest r join fetch r.catalogItem join fetch r.user "
            + "where r.status in (com.vgc.entity.RequestStatus.PURCHASED, com.vgc.entity.RequestStatus.REJECTED) "
            + "order by coalesce(r.purchasedAt, r.rejectedAt, r.updatedAt) desc")
    List<SnackRequest> findProcessedWithItemAndUser();
}
