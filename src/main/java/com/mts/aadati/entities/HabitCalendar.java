package com.mts.aadati.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity @Table(name = "habit_calendar" ,
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_habit_calendar_week_day", columnNames = {"week_id", "day_of_week"})
        },
        indexes = {
        @Index(name = "inx_habit_calendar_date", columnList = "date"),
        @Index(name = "inx_habit_calendar_day_of_week", columnList = "day_of_week"),
})
public class HabitCalendar {

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "habit_calendar_id" , nullable = false , updatable = false ,columnDefinition = "UUID")
    private UUID habitCalendarId ;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week" , length =10 , nullable = false)
    @Setter
    private DayOfWeek dayOfWeek ;

    @Column(name = "date" , nullable = false, unique = true)
    @Setter
    private LocalDate date ;


    @Column(name = "updated_at" , nullable = false)
    private Instant updatedAt;
    // =====  Relationship =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id" , nullable = false)
    private HabitWeek habitWeek ;


    @OneToMany(mappedBy = "habitCalendar" , fetch = FetchType.LAZY , cascade = CascadeType.ALL , orphanRemoval = true)
    private final List<HabitCompletion> habitCompletions = new ArrayList<>() ;

    @OneToMany(mappedBy = "habitCalendar" , fetch = FetchType.LAZY , cascade = CascadeType.ALL , orphanRemoval = true)
    private final List<TaskCompletion> taskCompletions = new ArrayList<>() ;

    @OneToMany(mappedBy = "habitCalendar", fetch = FetchType.LAZY)
    private final List<PercentageDay> percentageDays = new ArrayList<>();

    // ===== Constructor =====
    public HabitCalendar( @NonNull DayOfWeek dayOfWeek, @NonNull LocalDate date ,@NonNull HabitWeek habitWeek) {
        this.dayOfWeek = dayOfWeek;
        this.date = date;
        this.habitWeek = habitWeek ;
    }

    @PrePersist
    @PreUpdate
    private void onCreate() {
        updatedAt = Instant.now();
    }
}
