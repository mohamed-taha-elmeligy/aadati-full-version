package com.mts.aadati.repository;

import com.mts.aadati.entities.HabitWeek;
import com.mts.aadati.entities.PercentageDay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PercentageDayRepository extends JpaRepository<PercentageDay, UUID> {

    Optional<PercentageDay> findByUser_UserIdAndHabitCalendar_HabitCalendarId(UUID userId, UUID habitCalendarId);

    Page<PercentageDay> findAllByUser_UserId(UUID userId, Pageable pageable);

    Page<PercentageDay> findAllByUser_UserIdAndRateBetween(UUID userId, BigDecimal min, BigDecimal max, Pageable pageable);

    @Query("SELECT pd FROM PercentageDay pd WHERE pd.user.userId = :userId AND pd.rate = 100")
    Page<PercentageDay> findFullyCompletedByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT pd FROM PercentageDay pd WHERE pd.user.userId = :userId AND pd.rate > 0 AND pd.rate < 100")
    Page<PercentageDay> findPartiallyCompletedByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT pd FROM PercentageDay pd WHERE pd.user.userId = :userId AND pd.rate = 0")
    Page<PercentageDay> findNotStartedByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT pd FROM PercentageDay pd WHERE pd.user.userId = :userId AND pd.updatedAt BETWEEN :start AND :end")
    Page<PercentageDay> findAllByUserAndUpdatedBetween(@Param("userId") UUID userId,
                                                       @Param("start") Instant start,
                                                       @Param("end") Instant end,
                                                       Pageable pageable);

    @Query("SELECT pd FROM PercentageDay pd WHERE pd.user.userId = :userId AND pd.createdAt BETWEEN :start AND :end")
    Page<PercentageDay> findAllByUserAndCreatedBetween(@Param("userId") UUID userId,
                                                       @Param("start") Instant start,
                                                       @Param("end") Instant end,
                                                       Pageable pageable);

    @Query("SELECT AVG(pd.rate) FROM PercentageDay pd WHERE pd.user.userId = :userId")
    Double findAverageRateByUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(pd) FROM PercentageDay pd WHERE pd.user.userId = :userId")
    long countByUser(@Param("userId") UUID userId);

    List<PercentageDay> findByUser_UserIdAndHabitCalendar_HabitWeek(
            UUID userId,
            HabitWeek habitWeek
    );

    boolean existsByUser_UserIdAndHabitCalendar_HabitCalendarId(UUID userId, UUID habitCalendarId);

    @Modifying
    @Query("DELETE FROM PercentageDay pd WHERE pd.createdAt < :cutoffDate")
    int deleteAllByCreatedAtBefore(@Param("cutoffDate") Instant cutoffDate);

}