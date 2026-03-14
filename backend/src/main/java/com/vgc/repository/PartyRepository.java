package com.vgc.repository;

import com.vgc.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyRepository extends JpaRepository<Party, Long> {
    boolean existsByName(String name);
}
