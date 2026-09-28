package com.mts.aadati.services;

import com.mts.aadati.configs.caching.CacheNames;
import com.mts.aadati.dto.mapper.TaskPriorityLevelMapper;
import com.mts.aadati.dto.request.TaskPriorityLevelRequest;
import com.mts.aadati.entities.TaskPriorityLevel;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.TaskPriorityLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@CacheConfig(cacheNames = CacheNames.PRIORITY_LEVEL)
public class TaskPriorityLevelService {

    private static final String PRIORITY_NOT_FOUND = "PriorityLevel not found by ";
    private static final String PRIORITY_LEVEL_ALREADY_EXISTS = "PriorityLevel already exists";
    private static final String NAME_ALREADY_EXISTS = "Name already exists";
    private static final String COLOR_ALREADY_EXISTS = "Color already exists";

    private final TaskPriorityLevelRepository repository;
    private final TaskPriorityLevelMapper mapper;

    @Transactional
    @CacheEvict(value = CacheNames.PRIORITY_LEVEL + CacheNames.LIST_SUFFIX, allEntries = true)
    public TaskPriorityLevel addPriority(TaskPriorityLevel priority){
        if (repository.existsByPriorityLevel(priority.getPriorityLevel()))
            throw new DuplicateResourceException(PRIORITY_LEVEL_ALREADY_EXISTS);

        if (repository.existsByName(priority.getName()))
            throw new DuplicateResourceException(NAME_ALREADY_EXISTS);

        if (repository.existsByColor(priority.getColor()))
            throw new DuplicateResourceException(COLOR_ALREADY_EXISTS);

        return repository.save(priority);
    }


    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(
                            value = CacheNames.PRIORITY_LEVEL + CacheNames.LIST_SUFFIX,
                            allEntries = true
                    ),
                    @CacheEvict(
                           value = CacheNames.PRIORITY_LEVEL,
                            allEntries = true
                    )
            }
    )
    public TaskPriorityLevel updatePriority(UUID priorityID, TaskPriorityLevelRequest request){
        TaskPriorityLevel priority = getPriorityOrThrow(priorityID);

        if (repository.existsByPriorityLevelAndTaskPriorityLevelIdNot(request.priorityLevel(), priorityID))
            throw new DuplicateResourceException(PRIORITY_LEVEL_ALREADY_EXISTS);

        if (repository.existsByNameAndTaskPriorityLevelIdNot(request.name(), priorityID))
            throw new DuplicateResourceException(NAME_ALREADY_EXISTS);

        if (repository.existsByColorAndTaskPriorityLevelIdNot(request.color(), priorityID))
            throw new DuplicateResourceException(COLOR_ALREADY_EXISTS);

        mapper.update(request, priority);

        return repository.save(priority);
    }


    @Caching(
            put = {
                    @CachePut(key = "#result.priorityLevel"),
                    @CachePut(key = "'active:' + #result.priorityLevel", condition = "!#result.isDeleted")
            },
            evict = {
                    @CacheEvict(value = CacheNames.PRIORITY_LEVEL + CacheNames.LIST_SUFFIX, allEntries = true),
                    @CacheEvict(key = "'active:' + #result.priorityLevel", condition = "#result.isDeleted")
            }
    )
    @Transactional
    public TaskPriorityLevel toggleDeleted(UUID priorityID){
        TaskPriorityLevel priority = getPriorityOrThrow(priorityID);
        priority.setDeleted(!priority.isDeleted());

        return repository.save(priority);
    }

    @Cacheable(key = "#priorityLevel",sync = true)
    public TaskPriorityLevel findPriorityLevel(int priorityLevel){
        return repository.findByPriorityLevel(priorityLevel)
                .orElseThrow(()-> new ResourceNotFoundException(PRIORITY_NOT_FOUND + "priorityLevel"));
    }

    @Cacheable(
            value = CacheNames.PRIORITY_LEVEL + CacheNames.LIST_SUFFIX,
            key = "#root.methodName",
            sync = true
    )
    public List<TaskPriorityLevel> findPriorityLevelAsc(){
        return repository.findAllByOrderByPriorityLevelAsc();
    }

    @Cacheable(
            value = CacheNames.PRIORITY_LEVEL + CacheNames.LIST_SUFFIX,
            key = "#root.methodName",
            sync = true
    )
    public List<TaskPriorityLevel> findPriorityLevelDesc(){
        return repository.findAllByOrderByPriorityLevelDesc();
    }

    @Cacheable(
            value = CacheNames.PRIORITY_LEVEL + CacheNames.LIST_SUFFIX,
            key = "#root.methodName",
            sync = true
    )
    public List<String> getAllColor() {
        return repository.getAllColor();
    }

    @Cacheable(
            value = CacheNames.PRIORITY_LEVEL,
            key = "'active:' + #priorityLevel",
            sync = true)

    public TaskPriorityLevel findActivePriorityLevel(int priorityLevel){
        return repository.findByPriorityLevelAndIsDeletedFalse(priorityLevel)
                .orElseThrow(()-> new ResourceNotFoundException(PRIORITY_NOT_FOUND + "priorityLevel"));
    }

    @Cacheable(
            value = CacheNames.PRIORITY_LEVEL + CacheNames.LIST_SUFFIX,
            key = "#root.methodName",
            sync = true
    )
    public List<TaskPriorityLevel> findActivePriorityLevelAsc(){
        return repository.findAllByIsDeletedFalseOrderByPriorityLevelAsc();
    }

    @Cacheable(
            value = CacheNames.PRIORITY_LEVEL + CacheNames.LIST_SUFFIX,
            key = "#root.methodName",
            sync = true
    )
    public List<TaskPriorityLevel> findActivePriorityLevelDesc(){
        return repository.findAllByIsDeletedFalseOrderByPriorityLevelDesc();
    }

    @Cacheable(
            value = CacheNames.PRIORITY_LEVEL + CacheNames.LIST_SUFFIX,
            key = "#root.methodName",
            sync = true
    )
    public List<String> getActiveAllColor() {
        return repository.getAllActiveColor();
    }

    private TaskPriorityLevel getPriorityOrThrow(UUID priorityID){
        if(priorityID == null)
            throw new InvalidRequestException("Priority ID can't be null");

        return repository.findById(priorityID)
                .orElseThrow(()-> new ResourceNotFoundException(PRIORITY_NOT_FOUND + "id"));
    }
}