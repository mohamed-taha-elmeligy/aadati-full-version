package com.mts.aadati.repository;

import com.mts.aadati.entities.PercentageWeek;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PercentageWeekRepository extends JpaRepository<PercentageWeek, UUID> {

    Optional<PercentageWeek> findByUser_UserIdAndHabitWeek_WeekId(UUID userId, UUID habitWeekId);

    @Query("SELECT AVG(pw.rate) FROM PercentageWeek pw WHERE pw.user.userId = :userId")
    Double findAverageRateByUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(pw) FROM PercentageWeek pw WHERE pw.user.userId = :userId")
    long countByUser(@Param("userId") UUID userId);

    Page<PercentageWeek> findAllByUser_UserId(UUID userId, Pageable pageable);

    Page<PercentageWeek> findAllByUser_UserIdAndRateBetween(UUID userId, BigDecimal min, BigDecimal max, Pageable pageable);

    @Query("SELECT pw FROM PercentageWeek pw WHERE pw.user.userId = :userId AND pw.rate = 100")
    Page<PercentageWeek> findFullyCompletedByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT pw FROM PercentageWeek pw WHERE pw.user.userId = :userId AND pw.rate > 0 AND pw.rate < 100")
    Page<PercentageWeek> findPartiallyCompletedByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT pw FROM PercentageWeek pw WHERE pw.user.userId = :userId AND pw.rate = 0")
    Page<PercentageWeek> findNotStartedByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT pw FROM PercentageWeek pw WHERE pw.user.userId = :userId AND pw.updatedAt BETWEEN :start AND :end")
    Page<PercentageWeek> findAllByUserAndUpdatedBetween(@Param("userId") UUID userId,
                                                        @Param("start") Instant start,
                                                        @Param("end") Instant end,
                                                        Pageable pageable);

    @Query("SELECT pw FROM PercentageWeek pw WHERE pw.user.userId = :userId AND pw.createdAt BETWEEN :start AND :end")
    Page<PercentageWeek> findAllByUserAndCreatedBetween(@Param("userId") UUID userId,
                                                        @Param("start") Instant start,
                                                        @Param("end") Instant end,
                                                        Pageable pageable);

    Page<PercentageWeek> findAllByUser_UserIdAndGrade(UUID userId, String grade, Pageable pageable);

    @Query("SELECT pw FROM PercentageWeek pw WHERE pw.user.userId = :userId AND LOWER(pw.grade) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<PercentageWeek> searchByGradeKeyword(@Param("userId") UUID userId,
                                              @Param("keyword") String keyword,
                                              Pageable pageable);

    boolean existsByUser_UserIdAndHabitWeek_WeekId(UUID userId, UUID habitWeekId);

    @Modifying
    @Query("DELETE FROM PercentageWeek pw WHERE pw.createdAt < :cutoffDate")
    int deleteAllByCreatedAtBefore(@Param("cutoffDate") Instant cutoffDate);

}