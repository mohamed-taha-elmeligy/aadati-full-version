package com.mts.aadati.services;

import com.mts.aadati.configs.caching.CacheNames;
import com.mts.aadati.dto.mapper.HabitTaskMapper;
import com.mts.aadati.dto.request.HabitTaskRequest;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.entities.HabitTask;
import com.mts.aadati.enums.RecurrenceType;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.HabitTaskRepository;
import com.mts.aadati.utils.pagination.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@CacheConfig(cacheNames = CacheNames.HABIT_TASK + CacheNames.PAGE_SUFFIX)
public class HabitTaskService {

    private static final String SORT_BY = "title";
    private static final String HABIT_TASK_ALREADY_EXISTS = "HabitTask already exists";
    private static final String HABIT_TASK_NOT_FOUND = "HabitTask not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final HabitTaskRepository repository;
    private final HabitTaskMapper mapper;

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.HABIT_TASK + CacheNames.LIST_SUFFIX, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT_TASK + CacheNames.PAGE_SUFFIX, allEntries = true)
            }
    )
    public HabitTask addHabitTask(HabitTask habitTask) {
        if (repository.existsByUser_UserIdAndTitle(
                habitTask.getUser().getUserId(), habitTask.getTitle()))
            throw new DuplicateResourceException(HABIT_TASK_ALREADY_EXISTS);

        return repository.save(habitTask);
    }

    @Transactional
    @Caching(
            put = @CachePut(
                    value = CacheNames.HABIT_TASK,
                    key = "#result.user.userId + '-' + #result.habitTaskId"
            ),
            evict = {
                    @CacheEvict(value = CacheNames.HABIT_TASK + CacheNames.LIST_SUFFIX, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT_TASK + CacheNames.PAGE_SUFFIX, allEntries = true)
            }
    )
    public HabitTask updateHabitTask(UUID habitTaskId, UUID userId, HabitTaskRequest request) {
        HabitTask habitTask = getHabitTaskOrThrow(habitTaskId, userId);

        if (StringUtils.hasText(request.title())
                && !request.title().equalsIgnoreCase(habitTask.getTitle())
                && repository.existsByUser_UserIdAndTitleAndHabitTaskIdNot(userId, request.title(), habitTaskId))
            throw new DuplicateResourceException(HABIT_TASK_ALREADY_EXISTS);

        mapper.update(request, habitTask);

        return repository.save(habitTask);
    }

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.HABIT_TASK, key = "#userId + '-' + #habitTaskId"),
                    @CacheEvict(value = CacheNames.HABIT_TASK + CacheNames.LIST_SUFFIX, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT_TASK + CacheNames.PAGE_SUFFIX, allEntries = true)
            }
    )
    public void deleteHabitTask(UUID habitTaskId, UUID userId){
        repository.delete(getHabitTaskOrThrow(habitTaskId,userId));
    }

    @Transactional
    @Caching(
            put = @CachePut(
                    value = CacheNames.HABIT_TASK,
                    key = "#result.user.userId + '-' + #result.habitTaskId"
            ),
            evict = {
                    @CacheEvict(value = CacheNames.HABIT_TASK + CacheNames.LIST_SUFFIX, allEntries = true),
                    @CacheEvict(value = CacheNames.HABIT_TASK + CacheNames.PAGE_SUFFIX, allEntries = true)
            }
    )
    public HabitTask toggleActive(UUID habitTaskId, UUID userId) {
        HabitTask habitTask = getHabitTaskOrThrow(habitTaskId, userId);
        habitTask.setActive(!habitTask.isActive());

        return repository.save(habitTask);
    }

    @Cacheable(
            value = CacheNames.HABIT_TASK,
            key = "#userId + '-' + #habitTaskId",
            sync = true
    )
    public HabitTask findByIdAndUser(UUID habitTaskId, UUID userId) {
        return getHabitTaskOrThrow(habitTaskId, userId);
    }

    @Cacheable(
            value = CacheNames.HABIT_TASK + CacheNames.LIST_SUFFIX,
            key = "#userId + '-' + #title + '-' + #root.methodName",
            sync = true
    )
    public List<HabitTask> searchByTitle(UUID userId, String title) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (!StringUtils.hasText(title))
            throw new InvalidRequestException("Title " + CAN_NOT_BE_NULL);

        return repository.findByUser_UserIdAndTitleContainingIgnoreCaseAndIsActiveTrue(userId, title);
    }

    @Cacheable(
            key = "#userId + '-' + #categoryId + '-' + #priorityLevelId + '-' + #recurrenceType + '-' + #pageNumber + '-' + #root.methodName",
            sync = true
    )
    public PageModel<HabitTask> filterTasks(UUID userId, UUID categoryId, UUID priorityLevelId,
                                            RecurrenceType recurrenceType, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return PageModel.from(
                repository.filterTasks(userId, categoryId, priorityLevelId, recurrenceType,
                        PageableUtils.pageable(pageNumber, SORT_BY))
        );
    }

    @Cacheable(
            key = "#userId + '-' + #pageNumber + '-' + #root.methodName",
            sync = true
    )
    public PageModel<HabitTask> findAllActiveByUser(UUID userId, int pageNumber) {
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
    public PageModel<HabitTask> findAllInactiveByUser(UUID userId, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return PageModel.from(
                repository.findAllByUser_UserIdAndIsActiveFalse(userId, PageableUtils.pageable(pageNumber, SORT_BY))
        );
    }

    @Cacheable(
            key = "#userId + '-' + #startDate + '-' + #pageNumber + '-' + #root.methodName",
            sync = true
    )
    public PageModel<HabitTask> findByStartDate(UUID userId, Instant startDate, int pageNumber) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (startDate == null)
            throw new InvalidRequestException("Start date " + CAN_NOT_BE_NULL);

        return PageModel.from(
                repository.findByUser_UserIdAndStartDateAndIsActiveTrue(userId, startDate,
                        PageableUtils.pageable(pageNumber, SORT_BY))
        );
    }

    public long countByStartDate(UUID userId, Instant startDate) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (startDate == null)
            throw new InvalidRequestException("Start date " + CAN_NOT_BE_NULL);

        return repository.countByUser_UserIdAndStartDateAndIsActiveTrue(userId, startDate);
    }

    public long countByRecurrenceType(UUID userId, RecurrenceType recurrenceType) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (recurrenceType == null)
            throw new InvalidRequestException("RecurrenceType " + CAN_NOT_BE_NULL);

        return repository.countByUser_UserIdAndRecurrenceTypeAndIsActiveTrue(userId, recurrenceType);
    }

    public long countByPriorityLevel(UUID userId, UUID priorityLevelId) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (priorityLevelId == null)
            throw new InvalidRequestException("Priority Level Id " + CAN_NOT_BE_NULL);

        return repository.countByUser_UserIdAndTaskPriorityLevel_TaskPriorityLevelIdAndIsActiveTrue(userId, priorityLevelId);
    }

    public long countByCategory(UUID userId, UUID categoryId) {
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);
        if (categoryId == null)
            throw new InvalidRequestException("Category Id " + CAN_NOT_BE_NULL);

        return repository.countByUser_UserIdAndHabitCategory_HabitCategoryIdAndIsActiveTrue(userId, categoryId);
    }

    private HabitTask getHabitTaskOrThrow(UUID habitTaskId, UUID userId) {
        if (habitTaskId == null)
            throw new InvalidRequestException("HabitTask Id " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findByUser_UserIdAndHabitTaskId(userId, habitTaskId)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_TASK_NOT_FOUND + "id"));
    }

}