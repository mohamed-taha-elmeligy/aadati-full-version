package com.mts.aadati.services;

import com.mts.aadati.entities.HabitWeek;
import com.mts.aadati.entities.PercentageDay;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.PercentageDayRepository;
import com.mts.aadati.utils.pagination.PageableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PercentageDayService {

    private static final String SORT_CREATED_AT = "createdAt";
    private static final String PERCENTAGE_DAY_ALREADY_EXISTS = "PercentageDay already exists";
    private static final String PERCENTAGE_DAY_NOT_FOUND = "PercentageDay not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final PercentageDayRepository repository;

    @Transactional
    public PercentageDay addPercentageDay(PercentageDay percentageDay) {
        if (repository.existsByUser_UserIdAndHabitCalendar_HabitCalendarId(
                percentageDay.getUser().getUserId(),
                percentageDay.getHabitCalendar().getHabitCalendarId()
        )) throw new DuplicateResourceException(PERCENTAGE_DAY_ALREADY_EXISTS);

        return repository.save(percentageDay);
    }


    @Transactional
    public PercentageDay updatePercentageDay(UUID percentageDayId, PercentageDay percentageDay) {
        PercentageDay percentage = getPercentageDayOrThrow(percentageDayId);
        percentage.updateRate(percentageDay.getRate());

        return repository.save(percentage);
    }

    @Transactional
    public void deletePercentageDay(UUID percentageDayId) {
        repository.delete(getPercentageDayOrThrow(percentageDayId));
    }

    @Transactional
    public int cleanupOldPercentageDays() {
        Instant cutoffDate = Instant.now().minus(180, ChronoUnit.DAYS);
        int deletedCount = repository.deleteAllByCreatedAtBefore(cutoffDate);
        log.info("Cleaned up {} PercentageDay records created before {}", deletedCount, cutoffDate);
        return deletedCount;
    }

    public PercentageDay findByUserAndHabitCalendar(UUID userId, UUID habitCalendarId) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        if (habitCalendarId == null)
            throw new InvalidRequestException("HabitCalendar Id " + CAN_NOT_BE_NULL);

        return repository.findByUser_UserIdAndHabitCalendar_HabitCalendarId(userId, habitCalendarId)
                .orElseThrow(() -> new ResourceNotFoundException(PERCENTAGE_DAY_NOT_FOUND + "userId and habitCalendarId"));
    }

    public List<PercentageDay> findByUserAndHabitWeek(UUID userId, HabitWeek habitWeek) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        if (habitWeek == null)
            throw new InvalidRequestException("HabitWeek " + CAN_NOT_BE_NULL);

        return repository.findByUser_UserIdAndHabitCalendar_HabitWeek(userId, habitWeek);
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

    public Page<PercentageDay> findAllByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findAllByUser_UserId(userId, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageDay> findAllByUserAndRateBetween(UUID userId, BigDecimal min, BigDecimal max, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (min == null || max == null)
            throw new InvalidRequestException("Min and max rate " + CAN_NOT_BE_NULL);

        return repository.findAllByUser_UserIdAndRateBetween(userId, min, max, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageDay> findFullyCompletedByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findFullyCompletedByUser(userId, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageDay> findPartiallyCompletedByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findPartiallyCompletedByUser(userId, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageDay> findNotStartedByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findNotStartedByUser(userId, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageDay> findAllByUserAndUpdatedBetween(UUID userId, Instant start, Instant end, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates " + CAN_NOT_BE_NULL);

        return repository.findAllByUserAndUpdatedBetween(userId, start, end, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public Page<PercentageDay> findAllByUserAndCreatedBetween(UUID userId, Instant start, Instant end, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates " + CAN_NOT_BE_NULL);

        return repository.findAllByUserAndCreatedBetween(userId, start, end, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }


    private PercentageDay getPercentageDayOrThrow(UUID percentageDayId) {
        if (percentageDayId == null)
            throw new InvalidRequestException("PercentageDay Id " + CAN_NOT_BE_NULL);

        return repository.findById(percentageDayId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(PERCENTAGE_DAY_NOT_FOUND + "id"));
    }

}