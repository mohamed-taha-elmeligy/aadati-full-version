package com.mts.aadati.repository;

import com.mts.aadati.entities.HabitCalendar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitCalendarRepository extends JpaRepository<HabitCalendar, UUID> {

    Optional<HabitCalendar> findByDate(LocalDate localDate);
    Page<HabitCalendar> findByDateBetweenOrderByDate(LocalDate start, LocalDate end, Pageable pageable);

    List<HabitCalendar> findByHabitWeek_WeekId(UUID weekId);
    Optional<HabitCalendar> findByHabitWeek_WeekIdAndDayOfWeek(UUID weekId , DayOfWeek day);


    @Query("""
    SELECT COUNT(DISTINCT h) FROM HabitCalendar h
    JOIN h.habitCompletions hc
    JOIN hc.habit hb
    WHERE h.habitWeek.weekId = :weekId
      AND hb.user.userId = :userId
      AND hc.complete = true
    """)
    long countDaysWithCompletedHabitByWeek(@Param("weekId") UUID weekId, @Param("userId") UUID userId);

    @Query("""
    SELECT COUNT(DISTINCT h) FROM HabitCalendar h
    JOIN h.taskCompletions tc
    JOIN tc.task tk
    WHERE h.habitWeek.weekId = :weekId
      AND tk.user.userId = :userId
      AND tc.complete = true
    """)
    long countDaysWithCompletedTaskByWeek(@Param("weekId") UUID weekId, @Param("userId") UUID userId);

    Optional<HabitCalendar> findFirstByOrderByDateDesc();

    @Modifying
    @Query("DELETE FROM HabitCalendar hc WHERE hc.createdAt < :cutoffDate")
    int deleteAllByCreatedAtBefore(@Param("cutoffDate") Instant cutoffDate);

    boolean existsByDate(LocalDate date);

    boolean existsByHabitWeek_WeekIdAndDayOfWeek(UUID weekId, DayOfWeek dayOfWeek);
}