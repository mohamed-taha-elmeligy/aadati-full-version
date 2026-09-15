package com.mts.aadati.services;

import com.mts.aadati.entities.PercentageWeek;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.PercentageWeekRepository;
import com.mts.aadati.utils.pagination.PageableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PercentageWeekService {

    private static final String SORT_CREATED_AT = "createdAt";
    private static final String PERCENTAGE_WEEK_ALREADY_EXISTS = "PercentageWeek already exists";
    private static final String PERSENTAGE_WEEK_NOT_FOUND = "PercentageWeek not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final PercentageWeekRepository repository;

    @Transactional
    public PercentageWeek addPercentageWeek(PercentageWeek percentageWeek) {
        if (repository.existsByUser_UserIdAndHabitWeek_WeekId(
                percentageWeek.getUser().getUserId(),
                percentageWeek.getHabitWeek().getWeekId()
        )) throw new DuplicateResourceException(PERCENTAGE_WEEK_ALREADY_EXISTS);

        return repository.save(percentageWeek);
    }

    @Transactional
    public PercentageWeek updatePercentageWeek(UUID percentageWeekId, PercentageWeek percentageWeek) {

        PercentageWeek percentage = getPercentageWeekOrThrow(percentageWeekId);
        percentage.setRate(percentageWeek.getRate());

        return repository.save(percentage);
    }

    @Transactional
    public void deletePercentageWeek(UUID percentageWeekId) {
        repository.delete(getPercentageWeekOrThrow(percentageWeekId));
    }

    @Transactional
    public int cleanupOldPercentageWeeks() {
        Instant cutoffDate = Instant.now().minus(180, ChronoUnit.DAYS);
        int deletedCount = repository.deleteAllByCreatedAtBefore(cutoffDate);
        log.info("Cleaned up {} PercentageWeek records created before {}", deletedCount, cutoffDate);
        return deletedCount;
    }

    public PercentageWeek findByUserAndHabitWeek(UUID userId, UUID habitWeekId) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        if (habitWeekId == null)
            throw new InvalidRequestException("HabitWeek Id " + CAN_NOT_BE_NULL);

        return repository.findByUser_UserIdAndHabitWeek_WeekId(userId, habitWeekId)
                .orElseThrow(() -> new ResourceNotFoundException(PERSENTAGE_WEEK_NOT_FOUND + "userId and habitWeekId"));
    }

    public double findAverageRateByUser(UUID userId) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        Double average = repository.findAverageRateByUser(userId);
        return average != null ? average : 0.0;
    }

    public long countByUser(UUID userId) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.countByUser(userId);
    }

    public Page<PercentageWeek> findAllByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findAllByUser_UserId(userId, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageWeek> findAllByUserAndRateBetween(UUID userId, BigDecimal min, BigDecimal max, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (min == null || max == null)
            throw new InvalidRequestException("Min and max rate " + CAN_NOT_BE_NULL);

        return repository.findAllByUser_UserIdAndRateBetween(userId, min, max, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageWeek> findFullyCompletedByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findFullyCompletedByUser(userId, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageWeek> findPartiallyCompletedByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findPartiallyCompletedByUser(userId, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageWeek> findNotStartedByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findNotStartedByUser(userId, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageWeek> findAllByUserAndUpdatedBetween(UUID userId, Instant start, Instant end, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates " + CAN_NOT_BE_NULL);

        return repository.findAllByUserAndUpdatedBetween(userId, start, end, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageWeek> findAllByUserAndCreatedBetween(UUID userId, Instant start, Instant end, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates " + CAN_NOT_BE_NULL);

        return repository.findAllByUserAndCreatedBetween(userId, start, end, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageWeek> findAllByUserAndGrade(UUID userId, String grade, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (!StringUtils.hasText(grade))
            throw new InvalidRequestException("Grade " + CAN_NOT_BE_NULL);

        return repository.findAllByUser_UserIdAndGrade(userId, grade, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageWeek> searchByGradeKeyword(UUID userId, String keyword, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (!StringUtils.hasText(keyword))
            throw new InvalidRequestException("Keyword " + CAN_NOT_BE_NULL);

        return repository.searchByGradeKeyword(userId, keyword, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    private PercentageWeek getPercentageWeekOrThrow(UUID percentageWeekId) {
        if (percentageWeekId == null)
            throw new InvalidRequestException("PercentageWeek Id " + CAN_NOT_BE_NULL);

        return repository.findById(percentageWeekId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(PERSENTAGE_WEEK_NOT_FOUND + "id"));
    }

}