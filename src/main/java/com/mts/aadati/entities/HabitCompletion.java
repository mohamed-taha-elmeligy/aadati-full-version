
package com.mts.aadati.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity @Table(name ="habit_completion" , indexes = @Index(name = "inx_habit_completion_checker" , columnList = "complete"))
public class HabitCompletion {

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "habit_completion_id", nullable = false , updatable = false ,columnDefinition = "UUID")
    private UUID habitCompletionId;

    @Column(name = "complete", nullable = false)
    @Setter
    private boolean complete ;

    @Column(name = "completed_at"  )
    private Instant completedAt;


    @Column(name = "created_at"  )
    private Instant createdAt;

    // =====  Relationship =====
    @JoinColumn(name = "habit_calendar_id" , nullable = false)
    @ManyToOne( fetch = FetchType.LAZY )
    private HabitCalendar habitCalendar ;

    @JoinColumn(name = "habit_id" ,nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Habit habit ;

    // ===== Constructor ======
    public HabitCompletion (boolean complete ,@NonNull HabitCalendar habitCalendar ,@NonNull Habit habit){
        this.complete = complete;
        this.habitCalendar = habitCalendar ;
        this.habit = habit ;
    }

    // ===== Helper Method for Completed ======
    public boolean isCompleted() {
        return complete;
    }
    public String getCompleteText() {
        return complete ? "Completed" : "Uncompleted";
    }
    public HabitCompletion markIncomplete() {
        this.complete = false;
        return this ;
    }
    public HabitCompletion markComplete() {
        this.complete = true;
        return this ;
    }

    // ===== Lifecycle Callback ======
    @PrePersist
    private void onCreate() {
        this.createdAt = Instant.now();
        if (this.complete && this.completedAt == null) {
            this.completedAt = Instant.now();
        }
    }
    @PreUpdate
    private void onUpdate() {
        if (this.complete && this.completedAt == null) {
            this.completedAt = Instant.now();
        } else if (!this.complete) {
            this.completedAt = null;
        }
    }
}
