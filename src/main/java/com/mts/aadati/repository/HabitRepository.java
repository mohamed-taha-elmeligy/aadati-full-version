package com.mts.aadati.repository;

import com.mts.aadati.entities.Habit;
import com.mts.aadati.entities.HabitCategory;
import com.mts.aadati.entities.HabitDayWeek;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitRepository extends JpaRepository<Habit, UUID> {

    Optional<Habit> findByHabitIdAndUser_UserId(UUID habitId, UUID userId);

    @Query("""
    SELECT h FROM Habit h
    LEFT JOIN FETCH h.habitCategory
    JOIN h.habitDayWeeks hdw
    WHERE hdw.dayOfWeek = :dayOfWeek AND h.user.userId = :userId AND h.isActive = TRUE
    """)
    List<Habit> findByHabitDayWeekAndUser(@Param("dayOfWeek") DayOfWeek dayOfWeek, @Param("userId") UUID userId);

    List<Habit> findByTitleContainingIgnoreCaseAndUser_UserIdAndIsActiveTrue(String title, UUID userId);

    @Query("""
        SELECT h FROM Habit h
        LEFT JOIN FETCH h.habitCategory
        LEFT JOIN FETCH h.user
        WHERE h.user.userId = :userId AND
        h.isActive = TRUE AND
        (:categoryId IS NULL OR h.habitCategory.habitCategoryId = :categoryId) AND
        (:dayOfWeek IS NULL OR :dayOfWeek MEMBER OF h.habitDayWeeks) AND
        (:type IS NULL OR h.type = :type)
       """)
    Page<Habit> filterHabits(@Param("userId") UUID userId,
                             @Param("categoryId") UUID categoryId,
                             @Param("dayOfWeek") DayOfWeek dayOfWeek,
                             @Param("type") Boolean type,
                             Pageable pageable);

    Page<Habit> findAllByUser_UserIdAndIsActiveTrue(UUID userId, Pageable pageable);
    Page<Habit> findAllByUser_UserIdAndIsActiveFalse(UUID userId, Pageable pageable);

    long countByUser_UserIdAndIsActiveTrue(UUID userId);
    long countByUser_UserIdAndTypeAndIsActiveTrue(UUID userId, boolean type);
    long countByUser_UserIdAndHabitCategory_HabitCategoryIdAndIsActiveTrue(UUID userId, UUID categoryId);
    @Query("""
        SELECT COUNT(h) FROM Habit h
        JOIN h.habitDayWeeks hdw
        WHERE h.user.userId = :userId AND hdw.dayOfWeek = :dayOfWeek AND h.isActive = TRUE
        """)
    long countByUserAndDayOfWeek(@Param("userId") UUID userId, @Param("dayOfWeek") DayOfWeek dayOfWeek);
    long countByUser_UserIdAndPointLessThanAndIsActiveTrue(UUID userId, double point);
    long countByUser_UserIdAndPointGreaterThanAndIsActiveTrue(UUID userId, double point);

    boolean existsByTitleAndUser_UserId(String title, UUID userId);
    boolean existsByTitleAndUser_UserIdAndHabitIdNot(String title, UUID userId, UUID habitId);


}