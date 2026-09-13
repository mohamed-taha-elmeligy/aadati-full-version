package com.mts.aadati.repository;

import com.mts.aadati.entities.*;
import com.mts.aadati.enums.RecurrenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitTaskRepository extends JpaRepository<HabitTask, UUID> {

    Optional<HabitTask> findByUser_UserIdAndHabitTaskIdAndIsActiveTrue(UUID userId, UUID habitTaskId);

    List<HabitTask> findByUser_UserIdAndTitleContainingIgnoreCaseAndIsActiveTrue(UUID userId, String title);

    @Query("""
        SELECT ht FROM HabitTask ht
        LEFT JOIN FETCH ht.habitCategory
        LEFT JOIN FETCH ht.taskPriorityLevel
        WHERE ht.user.userId = :userId AND
        ht.isActive = TRUE AND
        (:recurrenceType IS NULL OR ht.recurrenceType = :recurrenceType) AND
        (:priorityLevelId IS NULL OR ht.taskPriorityLevel.taskPriorityLevelId = :priorityLevelId) AND
        (:categoryId IS NULL OR ht.habitCategory.habitCategoryId = :categoryId)
       """)
    Page<HabitTask> filterTasks(@Param("userId") UUID userId,
                                @Param("categoryId") UUID categoryId,
                                @Param("priorityLevelId") UUID priorityLevelId,
                                @Param("recurrenceType") RecurrenceType recurrenceType,
                                Pageable pageable);

    Page<HabitTask> findAllByUser_UserIdAndIsActiveTrue(UUID userId, Pageable pageable);
    Page<HabitTask> findAllByUser_UserIdAndIsActiveFalse(UUID userId, Pageable pageable);

    Page<HabitTask> findByUser_UserIdAndStartDateAndIsActiveTrue(UUID userId, Instant startDate, Pageable pageable);

    boolean existsByUser_UserIdAndTitle(UUID userId, String title);

    long countByUser_UserIdAndStartDateAndIsActiveTrue(UUID userId, Instant startDate);
    long countByUser_UserIdAndRecurrenceTypeAndIsActiveTrue(UUID userId, RecurrenceType recurrenceType);
    long countByUser_UserIdAndTaskPriorityLevel_TaskPriorityLevelIdAndIsActiveTrue(UUID userId, UUID priorityLevelId);
    long countByUser_UserIdAndHabitCategory_HabitCategoryIdAndIsActiveTrue(UUID userId, UUID categoryId);

}