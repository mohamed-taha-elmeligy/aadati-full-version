package com.mts.aadati.services;

import com.mts.aadati.configs.caching.CacheNames;
import com.mts.aadati.dto.mapper.HabitMapper;
import com.mts.aadati.dto.request.HabitRequest;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.entities.Habit;
import com.mts.aadati.entities.HabitCategory;
import com.mts.aadati.entities.HabitDayWeek;
import com.mts.aadati.entities.User;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.HabitCategoryRepository;
import com.mts.aadati.repository.HabitDayWeekRepository;
import com.mts.aadati.repository.HabitRepository;
import com.mts.aadati.repository.UserRepository;
import com.mts.aadati.utils.pagination.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@CacheConfig(cacheNames = CacheNames.HABIT + CacheNames.PAGE_SUFFIX)
public class HabitService {

    private static final String SORT_BY = "title";
    private static final String HABIT_ALREADY_EXISTS = "Habit already exists";
    private static final String HABIT_NOT_FOUND = "Habit not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";
    private static final String HABIT_CATEGORY_NOT_FOUND_OR_INACTIVE = "Habit Category not found or inactive";

    private final HabitMapper mapper;
    private final HabitRepository repository;

    private final HabitDayWeekRepository dayWeekRepository;
    private final HabitCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.HABIT + CacheNames.LIST_SUFFIX, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT + CacheNames.PAGE_SUFFIX, allEntries = true)
            }
    )
    public Habit addHabit(UUID userId, HabitRequest request) {
        if (repository.existsByTitleAndUser_UserId(request.title(), userId))
            throw new DuplicateResourceException(HABIT_ALREADY_EXISTS);

        if (!categoryRepository.existsByHabitCategoryIdAndIsDeletedFalse(request.habitCategoryId()))
            throw new ResourceNotFoundException(HABIT_CATEGORY_NOT_FOUND_OR_INACTIVE);

        User user = userRepository.getReferenceById(userId);
        HabitCategory category = categoryRepository.getReferenceById(request.habitCategoryId());

        List<HabitDayWeek> days = request.habitDayWeekIds().stream()
                .map(dayWeekRepository::getReferenceById)
                .toList();

        Habit habit = mapper.toEntity(request,user,category,days);

        return repository.save(habit);
    }

    @Transactional
    @Caching(
            put = @CachePut(
                    value = CacheNames.HABIT,
                    key = "#result.user.userId + '-' + #result.habitId"
            ),
            evict = {
                    @CacheEvict(value = CacheNames.HABIT + CacheNames.LIST_SUFFIX, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT + CacheNames.PAGE_SUFFIX, allEntries = true)
            }
    )
    public Habit updateHabit(UUID habitId, UUID userId, HabitRequest request) {
        Habit habit = getHabitOrThrow(habitId, userId);

        if (repository.existsByTitleAndUser_UserIdAndHabitIdNot(request.title(), userId, habitId))
            throw new DuplicateResourceException(HABIT_ALREADY_EXISTS);

        if (!categoryRepository.existsByHabitCategoryIdAndIsDeletedFalse(request.habitCategoryId()))
            throw new ResourceNotFoundException(HABIT_CATEGORY_NOT_FOUND_OR_INACTIVE);

        HabitCategory category = categoryRepository.getReferenceById(request.habitCategoryId());

        List<HabitDayWeek> days = request.habitDayWeekIds().stream()
                .map(dayWeekRepository::getReferenceById)
                .toList();

        mapper.update(request,category,days, habit);

        return repository.save(habit);
    }

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.HABIT, key = "#userId + '-' + #habitId"),
                    @CacheEvict(value = CacheNames.HABIT + CacheNames.LIST_SUFFIX, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT + CacheNames.PAGE_SUFFIX, allEntries = true)
            }
    )
    public void deleteHabit(UUID habitId, UUID userId){
        repository.delete(getHabitOrThrow(habitId, userId));
    }

    @Transactional
    @Caching(
            put = @CachePut(
                    value = CacheNames.HABIT,
                    key = "#result.user.userId + '-' + #result.habitId"
            ),
            evict = {
                    @CacheEvict(value = CacheNames.HABIT + CacheNames.LIST_SUFFIX, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT + CacheNames.PAGE_SUFFIX, allEntries = true)
            }
    )
    public Habit toggleActive(UUID habitId, UUID userId) {
        Habit habit = getHabitOrThrow(habitId, userId);
        habit.setActive(!habit.isActive());

        return repository.save(habit);
    }

    @Cacheable(
            value = CacheNames.HABIT,
            key = "#userId + '-' + #habitId",
            sync = true
    )
    public Habit findByIdAndUser(UUID habitId, UUID userId) {
        return getHabitOrThrow(habitId, userId);
    }

    @Cacheable(
            value = CacheNames.HABIT + CacheNames.LIST_SUFFIX,
            key = "#dayOfWeek + '-' + #userId + '-' + #root.methodName",
            sync = true
    )
    public List<Habit> findByDayOfWeekAndUser(DayOfWeek dayOfWeek, UUID userId) {
        if (dayOfWeek == null)
            throw new InvalidRequestException("DayOfWeek " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findByHabitDayWeekAndUser(dayOfWeek, userId);
    }

    @Cacheable(
            value = CacheNames.HABIT + CacheNames.LIST_SUFFIX,
            key = "#title + '-' + #userId + '-' + #root.methodName",
            sync = true
    )
    public List<Habit> searchByTitle(String title, UUID userId) {
        if (!StringUtils.hasText(title))
            throw new InvalidRequestException("Title " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findByTitleContainingIgnoreCaseAndUser_UserIdAndIsActiveTrue(title, userId);
    }

    @Cacheable(
            key = "#userId + '-' + #categoryId + '-' + #dayOfWeek + '-' + #type + '-' + #pageNumber + '-' + #root.methodName",
            sync = true
    )
    public PageModel<Habit> filterHabits(UUID userId, UUID categoryId, DayOfWeek dayOfWeek, Boolean type, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return PageModel.from(
                repository.filterHabits(userId, categoryId, dayOfWeek, type, PageableUtils.pageable(pageNumber, SORT_BY))
        );
    }

    @Cacheable(
            key = "#userId + '-' + #pageNumber + '-' + #root.methodName",
            sync = true
    )
    public PageModel<Habit> findAllActiveByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return PageModel.from(
                repository.findAllByUser_UserIdAndIsActiveTrue(userId, PageableUtils.pageable(pageNumber, SORT_BY))
        );
    }

    @Cacheable(
            key = "#userId + '-' + #pageNumber + '-' + #root.methodName",
            sync = true
    )
    public PageModel<Habit> findAllInactiveByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return PageModel.from(
                repository.findAllByUser_UserIdAndIsActiveFalse(userId, PageableUtils.pageable(pageNumber, SORT_BY))
        );
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