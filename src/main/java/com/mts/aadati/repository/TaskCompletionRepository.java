package com.mts.aadati.repository;

import com.mts.aadati.entities.*;
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
public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, UUID> {

    Optional<TaskCompletion> findByTaskCompletionIdAndHabitTask_User_UserId(UUID taskCompletionId, UUID userId);

    Page<TaskCompletion> findByHabitTaskAndHabitTask_User_UserId(
            HabitTask habitTask,
            UUID userId,
            Pageable pageable);

    Page<TaskCompletion> findByHabitCalendarAndHabitTask_User_UserId(
            HabitCalendar habitCalendar,
            UUID userId,
            Pageable pageable);

    @Query("""
           SELECT tc FROM TaskCompletion tc
           WHERE tc.habitTask.user.userId = :userId AND tc.habitTask = :habitTask AND tc.complete = :complete
           """)
    Page<TaskCompletion> findByHabitTaskAndUserAndComplete(@Param("habitTask") HabitTask habitTask,
                                                           @Param("userId") UUID userId,
                                                           @Param("complete") boolean complete,
                                                           Pageable pageable);

    @Query("""
           SELECT tc FROM TaskCompletion tc
           WHERE tc.habitTask.user.userId = :userId AND tc.completedAt BETWEEN :start AND :end
           """)
    Page<TaskCompletion> findByUserAndCompletedAtBetween(@Param("userId") UUID userId,
                                                         @Param("start") Instant start,
                                                         @Param("end") Instant end,
                                                         Pageable pageable);

    @Query("""
           SELECT COUNT(tc) FROM TaskCompletion tc
           WHERE tc.habitTask.user.userId = :userId AND tc.habitTask = :habitTask AND tc.complete = :complete
           """)
    long countByHabitTaskAndUserAndComplete(@Param("habitTask") HabitTask habitTask,
                                            @Param("userId") UUID userId,
                                            @Param("complete") boolean complete);

    @Query("""
           SELECT tc FROM TaskCompletion tc
           WHERE tc.habitTask.user.userId = :userId
           """)
    Page<TaskCompletion> findAllByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
           SELECT tc FROM TaskCompletion tc
           WHERE tc.habitTask.user.userId = :userId AND tc.complete = :complete
           """)
    Page<TaskCompletion> findAllByUserAndComplete(@Param("userId") UUID userId,
                                                  @Param("complete") boolean complete,
                                                  Pageable pageable);

    @Query("""
       SELECT tc FROM TaskCompletion tc
       WHERE tc.habitTask.user.userId = :userId
       AND LOWER(tc.habitTask.title) LIKE LOWER(CONCAT('%', :title, '%'))
       """)
    Page<TaskCompletion> findByHabitTaskTitleContainingAndUser(@Param("title") String title,
                                                               @Param("userId") UUID userId,
                                                               Pageable pageable);

    @Modifying
    @Query("DELETE FROM TaskCompletion tc WHERE tc.createdAt < :cutoffDate")
    int deleteAllByCreatedAtBefore(@Param("cutoffDate") Instant cutoffDate);

    boolean existsByHabitTaskAndHabitCalendar(HabitTask habitTask, HabitCalendar habitCalendar);
    boolean existsByHabitTask_HabitTaskIdAndHabitCalendar_HabitCalendarIdAndTaskCompletionIdNot(
            UUID habitTaskId, UUID habitCalendarId, UUID taskCompletionId);


}