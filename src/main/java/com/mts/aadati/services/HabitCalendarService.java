package com.mts.aadati.services;

import com.mts.aadati.configs.caching.CacheNames;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.HabitCalendarRepository;
import com.mts.aadati.utils.pagination.PageableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@CacheConfig(cacheNames = CacheNames.HABIT_CALENDAR + CacheNames.PAGE_SUFFIX)
public class HabitCalendarService {

    private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;
    private static final String SORT_DATE = "date";
    private static final String HABIT_CALENDAR_NOT_FOUND = "HabitCalendar not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";
    private static final String HABIT_CALENDAR_ALREADY_EXISTS = "HabitCalendar already exists";

    private final HabitCalendarRepository repository;

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.HABIT_CALENDAR, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT_CALENDAR + CacheNames.LIST_SUFFIX, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT_CALENDAR + CacheNames.PAGE_SUFFIX, allEntries = true)
            }
    )
    public HabitCalendar addCalendar(HabitCalendar calendar) {
        if (repository.existsByDate(calendar.getDate()))
            throw new DuplicateResourceException(HABIT_CALENDAR_ALREADY_EXISTS + " with date");

        if (repository.existsByHabitWeek_WeekIdAndDayOfWeek(
                calendar.getHabitWeek().getWeekId(), calendar.getDayOfWeek()))
            throw new DuplicateResourceException(HABIT_CALENDAR_ALREADY_EXISTS + " with week and day");

        return repository.save(calendar);
    }

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.HABIT_CALENDAR, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT_CALENDAR + CacheNames.LIST_SUFFIX, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT_CALENDAR + CacheNames.PAGE_SUFFIX, allEntries = true)
            }
    )
    public int cleanupOldCalendars() {
        Instant cutoffDate = ZonedDateTime.now(ZoneOffset.UTC).minusYears(20).toInstant();
        int deletedCount = repository.deleteAllByCreatedAtBefore(cutoffDate);
        log.info("Cleaned up {} HabitCalendar records created before {}", deletedCount, cutoffDate);
        return deletedCount;
    }

    @Cacheable(value = CacheNames.HABIT_CALENDAR, key = "#date + '-' + #root.methodName", sync = true)
    public HabitCalendar findByDate(LocalDate date) {
        if (date == null)
            throw new InvalidRequestException("Date " + CAN_NOT_BE_NULL);

        return repository.findByDate(date)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_CALENDAR_NOT_FOUND + "date"));
    }

    @Cacheable(key = "#start + '-' + #end + '-' + #pageNumber + '-' + #root.methodName", sync = true)
    public PageModel<HabitCalendar> findByDateBetween(LocalDate start, LocalDate end, int pageNumber) {
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates " + CAN_NOT_BE_NULL);
        if (start.isAfter(end))
            throw new InvalidRequestException("Start date must be before end date");

        return PageModel.from(
                repository.findByDateBetweenOrderByDate(
                        start,
                        end,
                        PageableUtils.pageable(
                                pageNumber,
                                SORT_DATE,
                                SORT_DIRECTION
                        )
                )
        );
    }

    @Cacheable(value = CacheNames.HABIT_CALENDAR + CacheNames.LIST_SUFFIX, key = "#weekId + '-' + #root.methodName", sync = true)
    public List<HabitCalendar> findByHabitWeek(UUID weekId) {
        if (weekId == null)
            throw new InvalidRequestException("Week Id " + CAN_NOT_BE_NULL);

        return repository.findByHabitWeek_WeekId(weekId);
    }

    @Cacheable(value = CacheNames.HABIT_CALENDAR, key = "#weekId + '-' + #day + '-' + #root.methodName", sync = true)
    public HabitCalendar findByWeekAndDay(UUID weekId, DayOfWeek day) {
        if (weekId == null)
            throw new InvalidRequestException("Week Id " + CAN_NOT_BE_NULL);
        if (day == null)
            throw new InvalidRequestException("DayOfWeek " + CAN_NOT_BE_NULL);

        return repository.findByHabitWeek_WeekIdAndDayOfWeek(weekId, day)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_CALENDAR_NOT_FOUND + "weekId and dayOfWeek"));
    }

    public long countDaysCompletedHabitsByWeek(UUID weekId, UUID userId) {
        if (weekId == null)
            throw new InvalidRequestException("Week Id " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.countDaysWithCompletedHabitByWeek(weekId, userId);
    }

    public long countDaysCompletedTasksByWeek(UUID weekId, UUID userId) {
        if (weekId == null)
            throw new InvalidRequestException("Week Id " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.countDaysWithCompletedTaskByWeek(weekId, userId);
    }

    @Cacheable(value = CacheNames.HABIT_CALENDAR, key = "#root.methodName", sync = true)
    public HabitCalendar findFirstByDate() {
        return repository.findFirstByOrderByDateDesc()
                .orElseThrow(()-> new ResourceNotFoundException("No HabitCalendar records exist"));
    }

}