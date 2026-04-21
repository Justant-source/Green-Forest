package com.vgc.repository;

import com.vgc.entity.PlantGrowth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlantGrowthRepository extends JpaRepository<PlantGrowth, Long> {
    Optional<PlantGrowth> findByUserId(Long userId);
}
