package com.vgc.repository;

import com.vgc.entity.GachaPityStack;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GachaPityStackRepository extends JpaRepository<GachaPityStack, Long> {
    Optional<GachaPityStack> findByUserIdAndPrizeId(Long userId, Long prizeId);
    List<GachaPityStack> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM GachaPityStack p WHERE p.user.id = :userId AND p.prize.id = :prizeId")
    Optional<GachaPityStack> findForUpdateByUserIdAndPrizeId(@Param("userId") Long userId, @Param("prizeId") Long prizeId);
}
