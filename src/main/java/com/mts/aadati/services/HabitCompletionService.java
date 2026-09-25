package com.mts.aadati.services;

import com.mts.aadati.entities.Habit;
import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.HabitCompletion;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.HabitCompletionRepository;
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
public class HabitCompletionService {

    private static final String SORT_COMPLETED_AT = "completedAt";
    private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;
    private static final String HABIT_COMPLETION_ALREADY_EXISTS = "HabitCompletion already exists";
    private static final String HABIT_COMPLETION_NOT_FOUND = "HabitCompletion not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final HabitCompletionRepository repository;

    @Transactional
    public HabitCompletion addHabitCompletion(HabitCompletion completion) {
        if (repository.existsByHabit_HabitIdAndHabitCalendar_HabitCalendarId(
                completion.getHabit().getHabitId(), completion.getHabitCalendar().getHabitCalendarId()
        )) throw new DuplicateResourceException(HABIT_COMPLETION_ALREADY_EXISTS);

        return repository.save(completion);
    }

    @Transactional
    public HabitCompletion updateHabitCompletion(UUID completionId, boolean complete) {
        HabitCompletion completion = getCompletionOrThrow(completionId);
        completion.setComplete(complete);

        return repository.save(completion);
    }

    @Transactional
    public void deleteHabitCompletion(UUID completionId) {
        repository.delete(getCompletionOrThrow(completionId));
    }

    @Transactional
    public int cleanupOldHabitCompletions() {
        Instant cutoffDate = Instant.now().minus(180, ChronoUnit.DAYS);
        int deletedCount = repository.deleteAllByCreatedAtBefore(cutoffDate);
        log.info("Cleaned up {} HabitCompletion records created before {}", deletedCount, cutoffDate);
        return deletedCount;
    }

    public HabitCompletion findByCompletionIdAndUser(UUID completionId, UUID userId) {
        if (completionId == null)
            throw new InvalidRequestException("HabitCompletion ID " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findByHabitCompletionIdAndHabit_User_UserId(completionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_COMPLETION_NOT_FOUND + "id"));
    }

    public Page<HabitCompletion> findByHabitAndUser(Habit habit, UUID userId, int pageNumber) {
        if (habit == null)
            throw new InvalidRequestException("Habit " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findByHabitAndHabit_User_UserId(
                habit,
                userId,
                PageableUtils.pageable(
                        pageNumber,
                        SORT_COMPLETED_AT,
                        SORT_DIRECTION
                )
        );
    }

    public Page<HabitCompletion> findByHabitCalendarAndUser(HabitCalendar habitCalendar, UUID userId, int pageNumber) {
        if (habitCalendar == null)
            throw new InvalidRequestException("HabitCalendar " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findByHabitCalendarAndHabit_User_UserId(
                habitCalendar,
                userId,
                PageableUtils.pageable(
                        pageNumber,
                        SORT_COMPLETED_AT,
                        SORT_DIRECTION
                )
        );
    }

    public Page<HabitCompletion> findByHabitAndUserAndComplete(Habit habit, UUID userId, boolean complete, int pageNumber) {
        if (habit == null)
            throw new InvalidRequestException("Habit " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findByHabitAndUserAndComplete(
                habit,
                userId,
                complete,
                PageableUtils.pageable(
                        pageNumber,
                        SORT_COMPLETED_AT,
                        SORT_DIRECTION
                )
        );
    }

    public Page<HabitCompletion> findByUserAndCompletedAtBetween(UUID userId, Instant start, Instant end, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates " + CAN_NOT_BE_NULL);

        return repository.findByUserAndCompletedAtBetween(
                userId,
                start,
                end,
                PageableUtils.pageable(
                        pageNumber,
                        SORT_COMPLETED_AT,
                        SORT_DIRECTION
                )
        );
    }

    public long countByHabitAndUserAndComplete(Habit habit, UUID userId, boolean complete) {
        if (habit == null)
            throw new InvalidRequestException("Habit " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.countByHabitAndUserAndComplete(habit, userId, complete);
    }

    public long countByCalendarAndUserAndComplete(HabitCalendar habitCalendar, UUID userId, boolean complete) {
        if (habitCalendar == null)
            throw new InvalidRequestException("HabitCalendar " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.countByCalendarAndUserAndComplete(habitCalendar, userId, complete);
    }

    public Page<HabitCompletion> findTodayByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findTodayByUser(
                userId,
                PageableUtils.pageable(
                        pageNumber,
                        SORT_COMPLETED_AT,
                        SORT_DIRECTION
                )
        );
    }

    public Page<HabitCompletion> findTodayByUserAndComplete(UUID userId, boolean complete, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findTodayByUserAndComplete(
                userId,
                complete,
                PageableUtils.pageable(
                        pageNumber,
                        SORT_COMPLETED_AT,
                        SORT_DIRECTION
                )
        );
    }

    public Page<HabitCompletion> findAllByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findAllByUser(
                userId,
                PageableUtils.pageable(
                        pageNumber,
                        SORT_COMPLETED_AT,
                        SORT_DIRECTION
                )
        );
    }

    public Page<HabitCompletion> findByHabitTitleContainingAndUser(String title, UUID userId, int pageNumber) {
        if (!StringUtils.hasText(title))
            throw new InvalidRequestException("Title " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User ID " + CAN_NOT_BE_NULL);

        return repository.findByHabitTitleContainingAndUser(
                title,
                userId,
                PageableUtils.pageable(
                        pageNumber,
                        SORT_COMPLETED_AT,
                        SORT_DIRECTION
                )
        );
    }

    private HabitCompletion getCompletionOrThrow(UUID completionId) {
        if (completionId == null)
            throw new InvalidRequestException("HabitCompletion ID " + CAN_NOT_BE_NULL);

        return repository.findById(completionId)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_COMPLETION_NOT_FOUND + "id"));
    }
}