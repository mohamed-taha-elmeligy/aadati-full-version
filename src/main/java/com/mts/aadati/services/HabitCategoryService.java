package com.mts.aadati.services;

import com.mts.aadati.dto.mapper.HabitCategoryMapper;
import com.mts.aadati.dto.request.HabitCategoryRequest;
import com.mts.aadati.entities.HabitCategory;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.HabitCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitCategoryService {

    private static final String HABIT_CATEGORY_ALREADY_EXISTS = "HabitCategory already exists";
    private static final String HABIT_CATEGORY_NOT_FOUND = "HabitCategory not found by ";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final HabitCategoryRepository repository;
    private final HabitCategoryMapper mapper;

    @Transactional
    public HabitCategory addCategory(HabitCategory category) {
        if (repository.existsByName(category.getName()))
            throw new DuplicateResourceException(HABIT_CATEGORY_ALREADY_EXISTS + " with name");

        if (repository.existsByColor(category.getColor()))
            throw new DuplicateResourceException(HABIT_CATEGORY_ALREADY_EXISTS + " with color");

        return repository.save(category);
    }

    @Transactional
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
    public HabitCategory toggleDeleted(UUID categoryId) {
        HabitCategory category = getCategoryOrThrow(categoryId);
        category.setDeleted(!category.isDeleted());

        return repository.save(category);
    }

    public List<HabitCategory> searchByName(String name) {
        if (!StringUtils.hasText(name))
            throw new InvalidRequestException("Category name " + CAN_NOT_BE_NULL);

        return repository.findByNameContainingIgnoreCase(name);
    }

    public HabitCategory findByName(String name) {
        if (!StringUtils.hasText(name))
            throw new InvalidRequestException("Category name " + CAN_NOT_BE_NULL);

        return repository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_CATEGORY_NOT_FOUND + "name"));
    }

    public List<HabitCategory> findAllOrderByNameAsc() {
        return repository.findAllByOrderByNameAsc();
    }

    public List<HabitCategory> findAllOrderByCreatedAtDesc() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<HabitCategory> findAllOrderByUpdatedAtDesc() {
        return repository.findAllByOrderByUpdatedAtDesc();
    }

    public List<HabitCategory> findByUpdatedAtBetween(Instant start, Instant end) {
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates " + CAN_NOT_BE_NULL);
        if (start.isAfter(end))
            throw new InvalidRequestException("Start date must be before end date");

        return repository.findByUpdatedAtBetween(start, end);
    }

    public List<HabitCategory> findByCreatedAtBetween(Instant start, Instant end) {
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates " + CAN_NOT_BE_NULL);
        if (start.isAfter(end))
            throw new InvalidRequestException("Start date must be before end date");

        return repository.findByCreatedAtBetween(start, end);
    }

    public List<HabitCategory> searchActiveByName(String name) {
        if (!StringUtils.hasText(name))
            throw new InvalidRequestException("Category name " + CAN_NOT_BE_NULL);

        return repository.findByNameContainingIgnoreCaseAndIsDeletedFalse(name);
    }

    public HabitCategory findActiveByName(String name) {
        if (!StringUtils.hasText(name))
            throw new InvalidRequestException("Category name " + CAN_NOT_BE_NULL);

        return repository.findByNameAndIsDeletedFalse(name)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_CATEGORY_NOT_FOUND + "name"));
    }

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