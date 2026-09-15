package com.mts.aadati.services;

import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.HabitWeek;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.HabitWeekRepository;
import com.mts.aadati.utils.pagination.PageableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitWeekService {

    private static final String SORT_CREATED_AT = "createdAt";
    private static final String HABIT_WEEK_ALREADY_EXISTS = "HabitWeek already exists";
    private static final String HABIT_WEEK_NOT_FOUND = "HabitWeek not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final HabitWeekRepository repository;

    @Transactional
    public HabitWeek addWeek(HabitWeek week){
        if (repository.existsByStartWeekOrEndWeek(week.getStartWeek(),week.getEndWeek()))
            throw new DuplicateResourceException(HABIT_WEEK_ALREADY_EXISTS);

        if (repository.existsByWeekNumberAndYear(week.getWeekNumber(), week.getYear()))
            throw new DuplicateResourceException(HABIT_WEEK_ALREADY_EXISTS);

        return repository.save(week);
    }

    @Transactional
    public int cleanupOldWeeks() {
        Instant cutoffDate = Instant.now().minus(20, ChronoUnit.YEARS);
        int deletedCount = repository.deleteAllByCreatedAtBefore(cutoffDate);
        log.info("Cleaned up {} HabitWeek records created before {}", deletedCount, cutoffDate);
        return deletedCount;
    }

    public Page<HabitWeek> findByYear(int year, int pageNumber){
        return repository.findByYear(year, PageableUtils.pageable(pageNumber, SORT_CREATED_AT));
    }

    public HabitWeek findByWeekNumberAndYear(int weekNumber, int year){
        return repository.findByWeekNumberAndYear(weekNumber, year)
                .orElseThrow(()-> new ResourceNotFoundException(HABIT_WEEK_NOT_FOUND + "weekNumber and year"));
    }

    public Page<HabitWeek> findByStartWeekBetween(LocalDate start, LocalDate end, int pageNumber) {

        if (start == null)
            throw new InvalidRequestException("Start week date " + CAN_NOT_BE_NULL);
        if (end == null)
            throw new InvalidRequestException("Start week date " + CAN_NOT_BE_NULL);
        if (start.isAfter(end))
            throw new InvalidRequestException("Start Week date can't be after end week");


        return repository.findByStartWeekBetween(start, end,
                PageableUtils.pageable(pageNumber, SORT_CREATED_AT)
        );
    }

    public Page<HabitWeek> findByEndWeekBetween(LocalDate start, LocalDate end, int pageNumber) {

        if (start == null)
            throw new InvalidRequestException("Start week date " + CAN_NOT_BE_NULL);
        if (end == null)
            throw new InvalidRequestException("End week date " + CAN_NOT_BE_NULL);
        if (start.isAfter(end))
            throw new InvalidRequestException("Start Week date can't be after end week");

        return repository.findByEndWeekBetween(start, end,
                PageableUtils.pageable(pageNumber, SORT_CREATED_AT)
        );
    }

    public Page<HabitWeek> findAllByStartWeekAsc(int pageNumber){
        return repository.findAllByOrderByStartWeekAsc(PageableUtils.pageable(pageNumber,SORT_CREATED_AT));
    }

    public List<HabitWeek> findTopByCreatedAtDesc(){
        return repository.findTop10ByOrderByCreatedAtDesc();
    }

    public HabitWeek findFirst() {
        return repository.findFirstByOrderByEndWeekDesc()
                .orElseThrow(()-> new ResourceNotFoundException("HabitWeek not found"));
    }

    public List<HabitCalendar> findHabitCalendarsByWeekId(UUID weekId){
        if (weekId == null)
            throw new InvalidRequestException("Week Id " + CAN_NOT_BE_NULL);

        return repository.findHabitCalendarsByWeekId(weekId);
    }
}
