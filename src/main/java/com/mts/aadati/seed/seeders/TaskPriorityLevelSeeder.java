package com.mts.aadati.seed.seeders;

import com.mts.aadati.entities.TaskPriorityLevel;
import com.mts.aadati.repository.TaskPriorityLevelRepository;
import com.mts.aadati.seed.Seeder;
import com.mts.aadati.services.TaskPriorityLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskPriorityLevelSeeder implements Seeder {

    private final TaskPriorityLevelRepository taskPriorityLevelRepository;
    private final TaskPriorityLevelService priorityLevelService;

    private static final List<TaskPriorityLevel> DEFAULT_LEVELS = List.of(
            TaskPriorityLevel.createUrgent(),
            TaskPriorityLevel.createHigh(),
            TaskPriorityLevel.createImportant(),
            TaskPriorityLevel.createMedium(),
            TaskPriorityLevel.createLow()
    );

    @Override
    public void seed() {
        if (taskPriorityLevelRepository.count() > 0) {
            log.debug("Task priority levels already seeded ({} existing), skipping",
                    taskPriorityLevelRepository.count());
            return;
        }

        log.debug("Seeding default task priority levels");
        for (TaskPriorityLevel priority : DEFAULT_LEVELS)
            priorityLevelService.addPriority(priority);

        log.info("Default task priority levels seeded ({} levels)", DEFAULT_LEVELS.size());
    }

    @Override
    public int order() {
        return 20;
    }
}

