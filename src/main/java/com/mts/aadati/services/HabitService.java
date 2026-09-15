package com.mts.aadati.services;

import com.mts.aadati.dto.mapper.HabitMapper;
import com.mts.aadati.dto.request.HabitRequest;
import com.mts.aadati.entities.Habit;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.HabitRepository;
import com.mts.aadati.utils.pagination.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitService {

    private static final String SORT_BY = "title";
    private static final String HABIT_ALREADY_EXISTS = "Habit already exists";
    private static final String HABIT_NOT_FOUND = "Habit not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final HabitMapper mapper;
    private final HabitRepository repository;

    @Transactional
    public Habit addHabit(Habit habit) {
        if (repository.existsByTitleAndUser_UserId(habit.getTitle(), habit.getUser().getUserId()))
            throw new DuplicateResourceException(HABIT_ALREADY_EXISTS);

        return repository.save(habit);
    }

    @Transactional
    public Habit updateHabit(UUID habitId, UUID userId, HabitRequest request) {
        Habit habit = getHabitOrThrow(habitId, userId);

        if (repository.existsByTitleAndUser_UserIdAndHabitIdNot(request.title(), userId, habitId))
            throw new DuplicateResourceException(HABIT_ALREADY_EXISTS);

        mapper.update(request, habit);

        return repository.save(habit);
    }

    @Transactional
    public void deleteHabit(UUID habitId, UUID userId){
        repository.delete(getHabitOrThrow(habitId, userId));
    }

    @Transactional
    public Habit toggleActive(UUID habitId, UUID userId) {
        Habit habit = getHabitOrThrow(habitId, userId);
        habit.setActive(!habit.isActive());

        return repository.save(habit);
    }

    public Habit findByIdAndUser(UUID habitId, UUID userId) {
        return getHabitOrThrow(habitId, userId);
    }

    public List<Habit> findByDayOfWeekAndUser(DayOfWeek dayOfWeek, UUID userId) {
        if (dayOfWeek == null)
            throw new InvalidRequestException("DayOfWeek " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findByHabitDayWeekAndUser(dayOfWeek, userId);
    }

    public List<Habit> searchByTitle(String title, UUID userId) {
        if (!StringUtils.hasText(title))
            throw new InvalidRequestException("Title " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findByTitleContainingIgnoreCaseAndUser_UserIdAndIsActiveTrue(title, userId);
    }

    public Page<Habit> filterHabits(UUID userId, UUID categoryId, DayOfWeek dayOfWeek, Boolean type, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.filterHabits(userId, categoryId, dayOfWeek, type, PageableUtils.pageable(pageNumber, SORT_BY));
    }

    public Page<Habit> findAllActiveByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findAllByUser_UserIdAndIsActiveTrue(userId, PageableUtils.pageable(pageNumber, SORT_BY));
    }

    public Page<Habit> findAllInactiveByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findAllByUser_UserIdAndIsActiveFalse(userId, PageableUtils.pageable(pageNumber, SORT_BY));
    }

    public long countActiveByUser(UUID userId) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.countByUser_UserIdAndIsActiveTrue(userId);
    }

    public long countByUserAndType(UUID userId, boolean type) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.countByUser_UserIdAndTypeAndIsActiveTrue(userId, type);
    }

    public long countByUserAndCategory(UUID userId, UUID categoryId) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (categoryId == null)
            throw new InvalidRequestException("Category Id " + CAN_NOT_BE_NULL);

        return repository.countByUser_UserIdAndHabitCategory_HabitCategoryIdAndIsActiveTrue(userId, categoryId);
    }

    public long countByUserAndDayOfWeek(UUID userId, DayOfWeek dayOfWeek) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (dayOfWeek == null)
            throw new InvalidRequestException("DayOfWeek " + CAN_NOT_BE_NULL);

        return repository.countByUserAndDayOfWeek(userId, dayOfWeek);
    }

    public long countByUserAndPointLessThan(UUID userId, double point) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.countByUser_UserIdAndPointLessThanAndIsActiveTrue(userId, point);
    }

    public long countByUserAndPointGreaterThan(UUID userId, double point) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.countByUser_UserIdAndPointGreaterThanAndIsActiveTrue(userId, point);
    }

    private Habit getHabitOrThrow(UUID habitId, UUID userId) {
        if (habitId == null)
            throw new InvalidRequestException("Habit Id " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findByHabitIdAndUser_UserId(habitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_NOT_FOUND + "id"));
    }

}