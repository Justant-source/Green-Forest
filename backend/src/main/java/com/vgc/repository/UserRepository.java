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

    List<User> findByPartyIdOrderByEarnedDropsDesc(Long partyId);

    @Query("SELECT u FROM User u WHERE u.party IS NOT NULL ORDER BY u.totalDrops DESC")
    List<User> findAllWithPartyOrderByTotalDropsDesc();

    @Query("SELECT u FROM User u WHERE u.party IS NOT NULL ORDER BY u.earnedDrops DESC")
    List<User> findAllWithPartyOrderByEarnedDropsDesc();

    @Query("SELECT u.party.id, u.party.name, SUM(u.earnedDrops), COUNT(u) " +
           "FROM User u WHERE u.party IS NOT NULL " +
           "GROUP BY u.party.id, u.party.name " +
           "ORDER BY SUM(u.earnedDrops) DESC")
    List<Object[]> getPartyRankings();

    List<User> findByNicknameIn(List<String> nicknames);

    List<User> findByNicknameContainingIgnoreCase(String keyword);

    List<User> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT u FROM User u WHERE u.birthDate IS NOT NULL AND FUNCTION('MONTH', u.birthDate) = :month AND FUNCTION('DAY', u.birthDate) = :day")
    List<User> findByBirthMonthAndDay(@Param("month") int month, @Param("day") int day);

    @Query("SELECT u FROM User u WHERE u.birthDate IS NOT NULL")
    List<User> findAllWithBirthDate();
}
