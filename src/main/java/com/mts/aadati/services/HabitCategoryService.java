package com.mts.aadati.services;

import com.mts.aadati.configs.caching.CacheNames;
import com.mts.aadati.dto.mapper.HabitCategoryMapper;
import com.mts.aadati.dto.request.HabitCategoryRequest;
import com.mts.aadati.entities.HabitCategory;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.HabitCategoryRepository;
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
@CacheConfig(cacheNames = CacheNames.HABIT_CATEGORY + CacheNames.LIST_SUFFIX)
public class HabitCategoryService {

    private static final String HABIT_CATEGORY_ALREADY_EXISTS = "HabitCategory already exists";
    private static final String HABIT_CATEGORY_NOT_FOUND = "HabitCategory not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final HabitCategoryRepository repository;
    private final HabitCategoryMapper mapper;

    @Transactional
    @CacheEvict(allEntries = true)
    public HabitCategory addCategory(HabitCategory category) {
        if (repository.existsByName(category.getName()))
            throw new DuplicateResourceException(HABIT_CATEGORY_ALREADY_EXISTS + " with name");

        if (repository.existsByColor(category.getColor()))
            throw new DuplicateResourceException(HABIT_CATEGORY_ALREADY_EXISTS + " with color");

        return repository.save(category);
    }

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.HABIT_CATEGORY, allEntries = true),
                    @CacheEvict(allEntries = true)
            }
    )
    public HabitCategory updateCategory(UUID categoryId, HabitCategoryRequest request) {
        HabitCategory category = getCategoryOrThrow(categoryId);

        if (repository.existsByNameAndHabitCategoryIdNot(request.name(), categoryId))
            throw new DuplicateResourceException(HABIT_CATEGORY_ALREADY_EXISTS + " with name");

        if (repository.existsByColorAndHabitCategoryIdNot(request.color(), categoryId))
            throw new DuplicateResourceException(HABIT_CATEGORY_ALREADY_EXISTS + " with color");

        mapper.update(request, category);

        return repository.save(category);
    }

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.HABIT_CATEGORY, allEntries = true),
                    @CacheEvict(allEntries = true)
            }
    )
    public HabitCategory toggleDeleted(UUID categoryId) {
        HabitCategory category = getCategoryOrThrow(categoryId);
        category.setDeleted(!category.isDeleted());

        return repository.save(category);
    }

    @Cacheable(key = "#name + '-' + #root.methodName", sync = true)
    public List<HabitCategory> searchByName(String name) {
        if (!StringUtils.hasText(name))
            throw new InvalidRequestException("Category name " + CAN_NOT_BE_NULL);

        return repository.findByNameContainingIgnoreCase(name);
    }

    @Cacheable(value = CacheNames.HABIT_CATEGORY, key = "#name + '-' + #root.methodName", sync = true)
    public HabitCategory findByName(String name) {
        if (!StringUtils.hasText(name))
            throw new InvalidRequestException("Category name " + CAN_NOT_BE_NULL);

        return repository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_CATEGORY_NOT_FOUND + "name"));
    }

    @Cacheable(key = "#root.methodName", sync = true)
    public List<HabitCategory> findAllOrderByNameAsc() {
        return repository.findAllByOrderByNameAsc();
    }

    @Cacheable(key = "#root.methodName", sync = true)
    public List<HabitCategory> findAllOrderByCreatedAtDesc() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Cacheable(key = "#root.methodName", sync = true)
    public List<HabitCategory> findAllOrderByUpdatedAtDesc() {
        return repository.findAllByOrderByUpdatedAtDesc();
    }

    @Cacheable(key = "#start + '-' + #end + '-' + #root.methodName", sync = true)
    public List<HabitCategory> findByUpdatedAtBetween(Instant start, Instant end) {
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates " + CAN_NOT_BE_NULL);
        if (start.isAfter(end))
            throw new InvalidRequestException("Start date must be before end date");

        return repository.findByUpdatedAtBetween(start, end);
    }

    @Cacheable(key = "#start + '-' + #end + '-' + #root.methodName", sync = true)
    public List<HabitCategory> findByCreatedAtBetween(Instant start, Instant end) {
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates " + CAN_NOT_BE_NULL);
        if (start.isAfter(end))
            throw new InvalidRequestException("Start date must be before end date");

        return repository.findByCreatedAtBetween(start, end);
    }

    @Cacheable(key = "#name + '-' + #root.methodName", sync = true)
    public List<HabitCategory> searchActiveByName(String name) {
        if (!StringUtils.hasText(name))
            throw new InvalidRequestException("Category name " + CAN_NOT_BE_NULL);

        return repository.findByNameContainingIgnoreCaseAndIsDeletedFalse(name);
    }

    @Cacheable(value = CacheNames.HABIT_CATEGORY, key = "#name + '-' + #root.methodName", sync = true)
    public HabitCategory findActiveByName(String name) {
        if (!StringUtils.hasText(name))
            throw new InvalidRequestException("Category name " + CAN_NOT_BE_NULL);

        return repository.findByNameAndIsDeletedFalse(name)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_CATEGORY_NOT_FOUND + "name"));
    }

    @Cacheable(key = "#root.methodName", sync = true)
    public List<HabitCategory> findAllActiveOrderByNameAsc() {
        return repository.findAllByIsDeletedFalseOrderByNameAsc();
    }

    private HabitCategory getCategoryOrThrow(UUID categoryId) {
        if (categoryId == null)
            throw new InvalidRequestException("Category Id " + CAN_NOT_BE_NULL);

        return repository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_CATEGORY_NOT_FOUND + "id"));
    }

}