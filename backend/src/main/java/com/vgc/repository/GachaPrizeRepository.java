package com.vgc.repository;

import com.vgc.entity.GachaPrize;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GachaPrizeRepository extends JpaRepository<GachaPrize, Long> {
    List<GachaPrize> findByActiveTrueAndRemainingStockGreaterThanOrderByDisplayOrderAscIdAsc(int minStock);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM GachaPrize p WHERE p.id = :id")
    Optional<GachaPrize> findForUpdateById(@Param("id") Long id);
}
