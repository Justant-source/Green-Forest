package com.vgc.repository;

import com.vgc.entity.AttendanceCheckin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceCheckinRepository extends JpaRepository<AttendanceCheckin, Long> {
    Optional<AttendanceCheckin> findByUserIdAndCheckinDate(Long userId, LocalDate date);
    List<AttendanceCheckin> findByCheckinDateOrderByCheckinAtAsc(LocalDate date);
    List<AttendanceCheckin> findByUserIdAndCheckinDateBetweenOrderByCheckinDateAsc(Long userId, LocalDate from, LocalDate to);
    long countByCheckinDate(LocalDate date);
    long countByUserIdAndCheckinDateBetween(Long userId, LocalDate from, LocalDate to);
    long countByCheckinDateBetween(LocalDate from, LocalDate to);
    boolean existsByCheckinDateAndWinnerTrue(LocalDate date);

    @Query("SELECT a.user.id, a.user.nickname, COUNT(a) as cnt FROM AttendanceCheckin a " +
           "WHERE a.checkinDate BETWEEN :from AND :to " +
           "GROUP BY a.user.id, a.user.nickname ORDER BY cnt DESC")
    List<Object[]> findTopAttendanceUsers(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT a FROM AttendanceCheckin a WHERE a.checkinDate BETWEEN :from AND :to AND a.winner = true ORDER BY a.checkinDate DESC")
    List<AttendanceCheckin> findWinnersBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
