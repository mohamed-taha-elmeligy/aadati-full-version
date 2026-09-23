
package com.mts.aadati.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity
@Table(name ="task_completion")
public class TaskCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "task_completion_id", nullable = false , updatable = false ,columnDefinition = "UUID")
    private UUID taskCompletionId;

    @Column(name = "complete", nullable = false)
    @Setter
    private boolean complete ;

    @Column(name = "completed_at" )
    private Instant completedAt;

    @Column(name = "created_at"  )
    private LocalDate createdAt;

    // =====  Relationship =====
    @JoinColumn(name = "habit_calendar_id" , nullable = false)
    @ManyToOne( fetch = FetchType.LAZY )
    private HabitCalendar habitCalendar ;

    @JoinColumn(name = "habit_task_id" , nullable = false)
    @ManyToOne(fetch =FetchType.LAZY)
    private HabitTask habitTask ;

    // ===== Constructor ======
    public TaskCompletion (boolean complete ,@NonNull HabitCalendar habitCalendar ,@NonNull HabitTask habitTask){
        this.complete = complete;
        this.habitCalendar = habitCalendar ;
        this.habitTask = habitTask ;
    }

    // ===== Helper Method for Completed ======
    public TaskCompletion markIncomplete() {
        this.complete = false;
        return this ;
    }
    public TaskCompletion markComplete() {
        this.complete = true;
        return this ;
    }

    // ===== Lifecycle Callback ======
    @PreUpdate
    private void updateTimestamp() {
        if (this.complete && this.completedAt == null) {
            this.completedAt = Instant.now();
        } else if (!this.complete) {
            this.completedAt = null;
        }
    }
    @PrePersist
    private void createTimestamp(){
        if (this.complete && this.completedAt == null) {
            this.completedAt = Instant.now();
        } else if (!this.complete) {
            this.completedAt = null;
        }
        this.createdAt = LocalDate.now(Clock.systemDefaultZone());
    }

}
