package com.mts.aadati.entities;

import com.mts.aadati.configs.auditing.Auditing;
import com.mts.aadati.enums.RecurrenceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NaturalId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity @Table(
        name = "habit_task",
        uniqueConstraints = @UniqueConstraint(columnNames = {"title","user_id"}),
        indexes = {
                @Index(name = "inx_habit_task_title", columnList = "title"),
                @Index(name = "inx_habit_task_start_date", columnList = "start_date"),
                @Index(name = "inx_habit_task_recurrence_type", columnList = "recurrence_type")}
)
public class HabitTask extends Auditing {

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "habit_task_id", nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID habitTaskId;

    @Column(name = "title", nullable = false, length = 50)
    @Setter
    @NaturalId
    private String title;

    @Column(name = "description", length = 800)
    @Setter
    private String description;

    @Column(name = "recurrence_type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    @Setter
    private RecurrenceType recurrenceType;

    @Column(name = "isActive", nullable = false)
    @Setter
    private boolean isActive = true;

    @Column(name = "start_date", nullable = false)
    @Setter
    private Instant startDate;

    // =====  Relationship =====
    @OneToMany(mappedBy = "habitTask" , cascade = CascadeType.ALL ,orphanRemoval = true , fetch = FetchType.LAZY)
    private final List<TaskCompletion> taskCompletions = new ArrayList<>();

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false)
    private User user ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_priority_level_id" , nullable = false)
    @Setter
    private TaskPriorityLevel taskPriorityLevel ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_category_id" , nullable = false)
    @Setter
    private HabitCategory habitCategory ;

    // ===== Constructor =====
    public HabitTask(@NonNull String title,
                     String description,
                     boolean isActive,
                     @NonNull Instant startDate,
                     @NonNull RecurrenceType recurrenceType ,
                     @NonNull User user,
                     @NonNull TaskPriorityLevel taskPriorityLevel,
                     @NonNull HabitCategory habitCategory) {
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.recurrenceType = recurrenceType;
        this.user = user ;
        this.taskPriorityLevel = taskPriorityLevel ;
        this.habitCategory = habitCategory ;
        this.isActive =  isActive;
    }

    // ===== Helper Method for is Active =====

    public void deactivate() {
        this.isActive = false;
    }

}
