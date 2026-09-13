package com.mts.aadati.repository;

import com.mts.aadati.entities.Habit;
import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.HabitCompletion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, UUID> {

    Optional<HabitCompletion> findByHabitCompletionIdAndHabitUser_UserId(UUID id, UUID userId);

    Page<HabitCompletion> findByHabitAndHabitUser_UserId(Habit habit, UUID userId, Pageable pageable);

    Page<HabitCompletion> findByHabitCalendarAndHabitUser_UserId(
            HabitCalendar habitCalendar,
            UUID userId,
            Pageable pageable
    );


    @Query("""
           SELECT hc FROM HabitCompletion hc
           JOIN hc.habit h
           JOIN h.user u
           WHERE u.userId = :userId
           AND h = :habit
           AND hc.complete = :complete
           """)
    Page<HabitCompletion> findByHabitAndUserAndComplete(
            @Param("habit") Habit habit,
            @Param("userId") UUID userId,
            @Param("complete") boolean complete,
            Pageable pageable
    );

    @Query("""
           SELECT COUNT(hc) FROM HabitCompletion hc
           JOIN hc.habit h
           JOIN h.user u
           WHERE u.userId = :userId
           AND h = :habit
           AND hc.complete = :complete
           """)
    long countByHabitAndUserAndComplete(
            @Param("habit") Habit habit,
            @Param("userId") UUID userId,
            @Param("complete") boolean complete
    );

    @Query("""
           SELECT COUNT(hc) FROM HabitCompletion hc
           JOIN hc.habitCalendar cal
           JOIN hc.habit h
           JOIN h.user u
           WHERE u.userId = :userId
           AND cal = :calendar
           AND hc.complete = :complete
           """)
    long countByCalendarAndUserAndComplete(
            @Param("calendar") HabitCalendar calendar,
            @Param("userId") UUID userId,
            @Param("complete") boolean complete
    );


    @Query("""
           SELECT hc FROM HabitCompletion hc
           JOIN hc.habit h
           JOIN h.user u
           WHERE u.userId = :userId
           AND hc.completedAt BETWEEN :start AND :end
           """)
    Page<HabitCompletion> findByUserAndCompletedAtBetween(
            @Param("userId") UUID userId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );

    @Query("""
           SELECT hc FROM HabitCompletion hc
           JOIN hc.habit h
           JOIN h.user u
           WHERE u.userId = :userId
           AND hc.habitCalendar.date = CURRENT_DATE
           """)
    Page<HabitCompletion> findTodayByUser(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
           SELECT hc FROM HabitCompletion hc
           JOIN hc.habit h
           JOIN h.user u
           WHERE u.userId = :userId
           AND hc.complete = :complete
           AND hc.habitCalendar.date = CURRENT_DATE
           """)
    Page<HabitCompletion> findTodayByUserAndComplete(
            @Param("userId") UUID userId,
            @Param("complete") boolean complete,
            Pageable pageable
    );

    @Query("""
           SELECT hc FROM HabitCompletion hc
           JOIN hc.habit h
           JOIN h.user u
           WHERE u.userId = :userId
           """)
    Page<HabitCompletion> findAllByUser(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
           SELECT hc FROM HabitCompletion hc
           JOIN hc.habit h
           JOIN h.user u
           WHERE u.userId = :userId
           AND LOWER(h.title) LIKE LOWER(CONCAT('%', :title, '%'))
           """)
    Page<HabitCompletion> findByHabitTitleContainingAndUser(
            @Param("title") String title,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Modifying
    @Query("DELETE FROM HabitCompletion hc WHERE hc.createdAt < :cutoffDate")
    int deleteAllByCreatedAtBefore(@Param("cutoffDate") Instant cutoffDate);

    boolean existsByHabit_HabitIdAndHabitCalendar_HabitCalendarId(UUID habitId, UUID calendarId);

}