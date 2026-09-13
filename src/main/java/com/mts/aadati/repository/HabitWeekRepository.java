package com.mts.aadati.repository;

import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.HabitWeek;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitWeekRepository extends JpaRepository<HabitWeek, UUID> {

    Page<HabitWeek> findByYear(int year, Pageable pageable);
    Optional<HabitWeek> findByWeekNumberAndYear(int weekNumber, int year);

    Page<HabitWeek> findByStartWeekBetween(LocalDate start, LocalDate end, Pageable pageable);
    Page<HabitWeek> findByEndWeekBetween(LocalDate start, LocalDate end, Pageable pageable);

    Page<HabitWeek> findAllByOrderByStartWeekAsc(Pageable pageable);
    List<HabitWeek> findTop10ByOrderByCreatedAtDesc();

    Optional<HabitWeek> findFirstByOrderByEndWeekDesc();

    @Query("""
        SELECT hc FROM HabitCalendar hc
        WHERE hc.habitWeek.weekId = :weekId
        """)
    List<HabitCalendar> findHabitCalendarsByWeekId(@Param("weekId") UUID weekId);

    boolean existsByStartWeekOrEndWeek(LocalDate start, LocalDate end);

    boolean existsByWeekNumberAndYear(int weekNumber, int year);

    @Modifying
    @Query("DELETE FROM HabitWeek hw WHERE hw.createdAt < :cutoffDate")
    int deleteAllByCreatedAtBefore(@Param("cutoffDate") Instant cutoffDate);

}
