package com.vgc.repository;

import com.vgc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    Optional<User> findByNickname(String nickname);

    List<User> findByPartyIdOrderByTotalDropsDesc(Long partyId);

    @Query("SELECT u FROM User u WHERE u.party IS NOT NULL ORDER BY u.totalDrops DESC")
    List<User> findAllWithPartyOrderByTotalDropsDesc();

    @Query("SELECT u.party.id, u.party.name, SUM(u.totalDrops), COUNT(u) " +
           "FROM User u WHERE u.party IS NOT NULL " +
           "GROUP BY u.party.id, u.party.name " +
           "ORDER BY SUM(u.totalDrops) DESC")
    List<Object[]> getPartyRankings();

    List<User> findByNicknameIn(List<String> nicknames);

    List<User> findByNicknameContainingIgnoreCase(String keyword);
}
