package com.mts.aadati.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "task_priority_level")
public class TaskPriorityLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "task_priority_level_id", nullable = false, updatable = false,unique = true, columnDefinition = "UUID")
    private UUID taskPriorityLevelId;

    @Column(name = "priority_level", nullable = false, unique = true)
    @Setter
    private int priorityLevel;

    @Column(name = "name", nullable = false, length = 50,unique = true)
    @Setter
    private String name;

    @Setter
    @Column(name = "color", nullable = false, length = 7 , unique = true)
    private String color;

    @Column(name = "is_deleted")
    @Setter
    private boolean isDeleted = false;

    // ===== Relationship =====
    @OneToMany(mappedBy = "taskPriorityLevel" , fetch = FetchType.LAZY ,
            cascade = {CascadeType.DETACH ,CascadeType.MERGE ,CascadeType.PERSIST ,CascadeType.REFRESH} )
    private final List<HabitTask> habitTasks = new ArrayList<>();


    // ===== Builder Constructor =====
    @Builder
    public TaskPriorityLevel(int priorityLevel ,@NonNull String name,@NonNull String color) {
        this.priorityLevel = priorityLevel;
        this.name = name;
        this.color = color;
    }
    private TaskPriorityLevel(int priorityLevel) {
        this.priorityLevel = priorityLevel;
        this.name = generateNameByLevel(priorityLevel);
        this.color = generateColorByLevel(priorityLevel);
    }

    // ===== Helper Methods =====
    private String generateColorByLevel(int level) {
        return switch (level) {
            case 1 -> "#FF4444";
            case 2 -> "#FF8800";
            case 3 -> "#FFD700";
            case 4 -> "#00CC66";
            case 5 -> "#00AAFF";
            default -> "#888888";
        };
    }
    private String generateNameByLevel(int level) {
        return switch (level) {
            case 1 -> "Urgent";
            case 2 -> "High";
            case 3 -> "Important";
            case 4 -> "Medium";
            case 5 -> "Low";
            default -> "Custom-" + level;
        };
    }

    // ===== Static Factory Methods =====
    public static TaskPriorityLevel createUrgent() {
        return new  TaskPriorityLevel(1);
    }

    public static TaskPriorityLevel createHigh() {
        return new  TaskPriorityLevel(2);
    }

    public static TaskPriorityLevel createImportant() {
        return new  TaskPriorityLevel(3);
    }

    public static TaskPriorityLevel createMedium() {
        return new  TaskPriorityLevel(4);
    }

    public static TaskPriorityLevel createLow() {
        return new  TaskPriorityLevel(5);
    }
}