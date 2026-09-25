package com.mts.aadati.repository;

import com.mts.aadati.entities.TaskPriorityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskPriorityLevelRepository extends JpaRepository<TaskPriorityLevel, UUID> {

    Optional<TaskPriorityLevel> findByPriorityLevel(int priorityLevel);
    List<TaskPriorityLevel> findAllByOrderByPriorityLevelAsc();
    List<TaskPriorityLevel> findAllByOrderByPriorityLevelDesc();
    @Query("SELECT DISTINCT t.color FROM TaskPriorityLevel t")
    List<String> getAllColor();

    Optional<TaskPriorityLevel> findByPriorityLevelAndIsDeletedFalse(int priorityLevel);
    List<TaskPriorityLevel> findAllByIsDeletedFalseOrderByPriorityLevelAsc();
    List<TaskPriorityLevel> findAllByIsDeletedFalseOrderByPriorityLevelDesc();
    @Query("SELECT DISTINCT t.color FROM TaskPriorityLevel t WHERE t.isDeleted = false")
    List<String> getAllActiveColor();

    boolean existsByPriorityLevel(int priorityLevel);
    boolean existsByColor(String color);
    boolean existsByName(String name);

    boolean existsByPriorityLevelAndTaskPriorityLevelIdNot(int priorityLevel, UUID taskPriorityLevelId);
    boolean existsByNameAndTaskPriorityLevelIdNot(String name, UUID taskPriorityLevelId);
    boolean existsByColorAndTaskPriorityLevelIdNot(String color, UUID taskPriorityLevelId);
}