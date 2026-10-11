package com.mts.aadati.services;

import com.mts.aadati.configs.caching.CacheNames;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.HabitWeek;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.HabitWeekRepository;
import com.mts.aadati.utils.pagination.PageableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@CacheConfig(cacheNames = CacheNames.HABIT_WEEK + CacheNames.PAGE_SUFFIX)
public class HabitWeekService {

    private static final String SORT_CREATED_AT = "createdAt";
    private static final String HABIT_WEEK_ALREADY_EXISTS = "HabitWeek already exists";
    private static final String HABIT_WEEK_NOT_FOUND = "HabitWeek not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final HabitWeekRepository repository;

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(
                            value = CacheNames.HABIT_WEEK + CacheNames.LIST_SUFFIX,
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = CacheNames.HABIT_WEEK + CacheNames.PAGE_SUFFIX,
                            allEntries = true
                    )
            }
    )
    public void addAll(List<HabitWeek> weeks) {
        if (weeks.isEmpty())
            return;

        Set<LocalDate> startWeeks = new HashSet<>();
        Set<String> weekKeys = new HashSet<>();

        for (HabitWeek w : weeks) {
            if (!startWeeks.add(w.getStartWeek()))
                throw new DuplicateResourceException(HABIT_WEEK_ALREADY_EXISTS + " with start " + w.getStartWeek());

            String key = w.getWeekNumber() + "-" + w.getYear();
            if (!weekKeys.add(key))
                throw new DuplicateResourceException(HABIT_WEEK_ALREADY_EXISTS + " with week " + key);
        }

        if (repository.existsByStartWeekIn(startWeeks))
            throw new DuplicateResourceException(HABIT_WEEK_ALREADY_EXISTS + " with start week");

        repository.saveAll(weeks);
    }

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(
                            value = CacheNames.HABIT_WEEK,
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = CacheNames.HABIT_WEEK + CacheNames.LIST_SUFFIX,
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = CacheNames.HABIT_WEEK + CacheNames.PAGE_SUFFIX,
                            allEntries = true
                    )
            }
    )
    public int cleanupOldWeeks() {
        Instant cutoffDate = ZonedDateTime.now(ZoneOffset.UTC).minusYears(20).toInstant();
        int deletedCount = repository.deleteAllByCreatedAtBefore(cutoffDate);
        log.info("Cleaned up {} HabitWeek records created before {}", deletedCount, cutoffDate);
        return deletedCount;
    }

    @Cacheable(
            key = "#year + '-' + #pageNumber + '-' + #root.methodName",
            sync = true
    )
    public PageModel<HabitWeek> findByYear(int year, int pageNumber){
        return PageModel.from(
                repository.findByYear(year, PageableUtils.pageable(pageNumber, SORT_CREATED_AT))
        );
    }

    @Cacheable(
            value = CacheNames.HABIT_WEEK,
            key = "#weekNumber + '-' + #year",
            sync = true
    )
    public HabitWeek findByWeekNumberAndYear(int weekNumber, int year){
        return repository.findByWeekNumberAndYear(weekNumber, year)
                .orElseThrow(()-> new ResourceNotFoundException(HABIT_WEEK_NOT_FOUND + "weekNumber and year"));
    }

    @Cacheable(
            key = "#start + '-' + #end + '-' + #pageNumber + '-' + #root.methodName",
            sync = true
    )
    public PageModel<HabitWeek> findByStartWeekBetween(LocalDate start, LocalDate end, int pageNumber) {

        if (start == null)
            throw new InvalidRequestException("Start week date " + CAN_NOT_BE_NULL);
        if (end == null)
            throw new InvalidRequestException("Start week date " + CAN_NOT_BE_NULL);
        if (start.isAfter(end))
            throw new InvalidRequestException("Start Week date can't be after end week");

        return PageModel.from(
                repository.findByStartWeekBetween(
                        start,
                        end,
                        PageableUtils.pageable(pageNumber, SORT_CREATED_AT)
                )
        );
    }

    @Cacheable(
            key = "#start + '-' + #end + '-' + #pageNumber + '-' + #root.methodName",
            sync = true
    )
    public PageModel<HabitWeek> findByEndWeekBetween(LocalDate start, LocalDate end, int pageNumber) {

        if (start == null)
            throw new InvalidRequestException("Start week date " + CAN_NOT_BE_NULL);
        if (end == null)
            throw new InvalidRequestException("End week date " + CAN_NOT_BE_NULL);
        if (start.isAfter(end))
            throw new InvalidRequestException("Start Week date can't be after end week");

        return PageModel.from(
                repository.findByEndWeekBetween(
                        start,
                        end,
                        PageableUtils.pageable(pageNumber, SORT_CREATED_AT)
                )
        );
    }

    @Cacheable(
            key = "#pageNumber + '-' + #root.methodName",
            sync = true
    )
    public PageModel<HabitWeek> findAllByStartWeekAsc(int pageNumber){
        return PageModel.from(
                repository.findAllByOrderByStartWeekAsc(PageableUtils.pageable(pageNumber,SORT_CREATED_AT))
        );
    }

    @Cacheable(
            value = CacheNames.HABIT_WEEK + CacheNames.LIST_SUFFIX,
            key = "#root.methodName",
            sync = true
    )
    public List<HabitWeek> findTopByCreatedAtDesc(){
        return repository.findTop10ByOrderByCreatedAtDesc();
    }

    @Cacheable(
            value = CacheNames.HABIT_WEEK,
            key = "#root.methodName",
            sync = true
    )
    public HabitWeek findFirst() {
        return repository.findFirstByOrderByEndWeekDesc()
                .orElseThrow(()-> new ResourceNotFoundException(HABIT_WEEK_NOT_FOUND + "any week"));
    }
    @Cacheable(
            value = CacheNames.HABIT_WEEK,
            key = "#root.methodName",
            sync = true
    )
    public Optional<HabitWeek> findLatest() {
        return repository.findFirstByOrderByEndWeekDesc();
    }

    @Cacheable(
            value = CacheNames.HABIT_WEEK + CacheNames.LIST_SUFFIX,
            key = "#weekId + '-' + #root.methodName",
            sync = true
    )
    public List<HabitCalendar> findHabitCalendarsByWeekId(UUID weekId){
        if (weekId == null)
            throw new InvalidRequestException("Week Id " + CAN_NOT_BE_NULL);

        return repository.findHabitCalendarsByWeekId(weekId);
    }

    @Cacheable(
            value = CacheNames.HABIT_WEEK + CacheNames.LIST_SUFFIX,
            key = "#from + '-' + #to + '-' + #root.methodName",
            sync = true
    )
    public List<HabitWeek> findAllBetween(LocalDate from, LocalDate to){
        if (from == null)
            throw new InvalidRequestException("from week date " + CAN_NOT_BE_NULL);
        if (to == null)
            throw new InvalidRequestException("to week date " + CAN_NOT_BE_NULL);
        if (from.isAfter(to))
            throw new InvalidRequestException("Start Week date can't be after end week");

        return repository.findAllByStartWeekLessThanEqualAndEndWeekGreaterThanEqual(to,from);
    }
}