package com.vgc.repository;

import com.vgc.entity.GachaPityStack;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GachaPityStackRepository extends JpaRepository<GachaPityStack, Long> {
    Optional<GachaPityStack> findByPrizeId(Long prizeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM GachaPityStack p WHERE p.prize.id = :prizeId")
    Optional<GachaPityStack> findForUpdateByPrizeId(@Param("prizeId") Long prizeId);
}
