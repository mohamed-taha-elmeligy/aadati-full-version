package com.mts.aadati.repository;

import com.mts.aadati.entities.Habit;
import com.mts.aadati.entities.HabitDayWeek;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Repository
public interface HabitDayWeekRepository extends JpaRepository<HabitDayWeek, Long> {

    List<HabitDayWeek> findByOrderByDayOfWeekAsc();

    @Query("""
    SELECT DISTINCT hb
    FROM HabitDayWeek hdw
    JOIN hdw.habits hb
    LEFT JOIN FETCH hb.user
    LEFT JOIN FETCH hb.habitCategory
    WHERE hdw.dayOfWeek = :dayOfWeek
      AND hb.user.userId = :userId And hb.isActive = True
    """)
    Page<Habit> findHabitsByDayOfWeekAndUserIdAndIsActiveTrue(
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
    SELECT COUNT(DISTINCT hb)
    FROM HabitDayWeek hdw
    JOIN hdw.habits hb
    WHERE hdw.dayOfWeek = :day
      AND hb.user.userId = :userId And hb.isActive = True
    """)
    long countHabitsByDayOfWeekAndUserIdAndIsActiveTrue(
            @Param("day") DayOfWeek day,
            @Param("userId") UUID userId
    );


    boolean existsByDayOfWeek(DayOfWeek day);
}
