package com.mts.aadati.services;

import com.mts.aadati.dto.request.TaskCompletionRequest;
import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.HabitTask;
import com.mts.aadati.entities.TaskCompletion;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.TaskCompletionRepository;
import com.mts.aadati.utils.pagination.PageableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskCompletionService {

    private static final String SORT_COMPLETED_AT = "completedAt";
    private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;
    private static final String TASK_COMPLETION_ALREADY_EXISTS = "TaskCompletion already exists";
    private static final String TASK_COMPLETION_NOT_FOUND = "TaskCompletion not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final TaskCompletionRepository repository;

    @Transactional
    public TaskCompletion addCompletion(TaskCompletion completion) {
        if (repository.existsByHabitTaskAndHabitCalendar(
                completion.getHabitTask(),
                completion.getHabitCalendar())
        )
            throw new DuplicateResourceException(TASK_COMPLETION_ALREADY_EXISTS);

        return repository.save(completion);
    }

    @Transactional
    public TaskCompletion updateCompletion(UUID completionID, TaskCompletionRequest request) {
        TaskCompletion completion = getCompletionOrThrow(completionID);

        if (repository.existsByHabitTask_HabitTaskIdAndHabitCalendar_HabitCalendarIdAndTaskCompletionIdNot(
                request.habitTaskId(),
                request.habitCalendarId(),
                completionID)
        )
            throw new DuplicateResourceException(TASK_COMPLETION_ALREADY_EXISTS);

        completion.setComplete(request.complete());

        return repository.save(completion);
    }

    @Transactional
    public void deleteCompletion(UUID completionID) {
        repository.delete(getCompletionOrThrow(completionID));
    }

    @Transactional
    public int cleanupOldCompletions() {
        Instant cutoffDate = Instant.now().minus(180, ChronoUnit.DAYS);
        int deletedCount = repository.deleteAllByCreatedAtBefore(cutoffDate);
        log.info("Cleaned up {} TaskCompletion records created before {}", deletedCount, cutoffDate);
        return deletedCount;
    }

    public TaskCompletion findByCompletionIdAndUser(UUID taskCompletionId, UUID userId) {
        if (taskCompletionId == null)
            throw new InvalidRequestException("TaskCompletion ID " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findByTaskCompletionIdAndHabitTask_User_UserId(taskCompletionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_COMPLETION_NOT_FOUND + "id"));
    }

    public Page<TaskCompletion> findByHabitTaskAndUser(HabitTask habitTask, UUID userId, int pageNumber) {
        if (habitTask == null)
            throw new InvalidRequestException("HabitTask " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findByHabitTaskAndHabitTask_User_UserId(habitTask, userId, PageableUtils.pageable(pageNumber, SORT_COMPLETED_AT,SORT_DIRECTION));
    }

    public Page<TaskCompletion> findByHabitCalendarAndUser(HabitCalendar habitCalendar, UUID userId, int pageNumber) {
        if (habitCalendar == null)
            throw new InvalidRequestException("HabitCalendar " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findByHabitCalendarAndHabitTask_User_UserId(habitCalendar, userId, PageableUtils.pageable(pageNumber, SORT_COMPLETED_AT,SORT_DIRECTION));
    }

    public Page<TaskCompletion> findByHabitTaskAndUserAndComplete(HabitTask habitTask, UUID userId, boolean complete, int pageNumber) {
        if (habitTask == null)
            throw new InvalidRequestException("HabitTask " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findByHabitTaskAndUserAndComplete(habitTask, userId, complete, PageableUtils.pageable(pageNumber, SORT_COMPLETED_AT,SORT_DIRECTION));
    }

    public Page<TaskCompletion> findByUserAndCompletedAtBetween(UUID userId, Instant start, Instant end, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates " + CAN_NOT_BE_NULL);

        return repository.findByUserAndCompletedAtBetween(userId, start, end, PageableUtils.pageable(pageNumber, SORT_COMPLETED_AT,SORT_DIRECTION));
    }

    public long countByHabitTaskAndUserAndComplete(HabitTask habitTask, UUID userId, boolean complete) {
        if (habitTask == null)
            throw new InvalidRequestException("HabitTask " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.countByHabitTaskAndUserAndComplete(habitTask, userId, complete);
    }

    public Page<TaskCompletion> findAllByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findAllByUser(userId, PageableUtils.pageable(pageNumber, SORT_COMPLETED_AT,SORT_DIRECTION));
    }

    public Page<TaskCompletion> findAllByUserAndComplete(UUID userId, boolean complete, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findAllByUserAndComplete(userId, complete, PageableUtils.pageable(pageNumber, SORT_COMPLETED_AT,SORT_DIRECTION));
    }

    public Page<TaskCompletion> findByHabitTaskTitleContainingAndUser(String title, UUID userId, int pageNumber) {
        if (!StringUtils.hasText(title))
            throw new InvalidRequestException("Title " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findByHabitTaskTitleContainingAndUser(title, userId, PageableUtils.pageable(pageNumber, SORT_COMPLETED_AT,SORT_DIRECTION));
    }

    private TaskCompletion getCompletionOrThrow(UUID completionId) {
        if (completionId == null)
            throw new InvalidRequestException("TaskCompletion ID " + CAN_NOT_BE_NULL);

        return repository.findById(completionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(TASK_COMPLETION_NOT_FOUND + "id")
                );
    }
}