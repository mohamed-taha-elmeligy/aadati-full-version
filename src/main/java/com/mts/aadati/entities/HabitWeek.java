package com.mts.aadati.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity @Table(name = "habit_week" , indexes = {
        @Index(name = "idx_habit_week_year_week", columnList = "year_number, week_number"),
        @Index(name = "idx_habit_week_date_range", columnList = "start_week, end_week"),
        @Index(name = "idx_habit_week_updated", columnList = "updated_at")
})
public class HabitWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "week_id" , nullable = false ,updatable = false , columnDefinition = "UUID")
    private UUID weekId ;

    @Column(name = "week_number" , nullable = false )
    @Setter
    private int weekNumber ;

    @Column(name = "start_week",nullable = false )
    @Setter
    private LocalDate startWeek ;

    @Column(name = "end_week",nullable = false )
    @Setter
    private LocalDate endWeek ;

    @Column(name = "year_number",nullable = false )
    @Setter
    private int year ;

    @Column(name = "updated_at" ,nullable = false)
    private Instant updatedAt;


    // =====  Relationship =====
    @OneToMany( fetch = FetchType.LAZY ,
            mappedBy = "habitWeek" ,cascade = {CascadeType.REFRESH ,CascadeType.DETACH ,CascadeType.MERGE,CascadeType.PERSIST})
    private final List<HabitCalendar> habitCalendars = new ArrayList<>() ;


    // ===== Builder Constructor =====
    @Builder
    public HabitWeek (int weekNumber ,@NonNull LocalDate startWeek ,@NonNull LocalDate endWeek ,int year ){
        this.weekNumber = weekNumber ;
        this.startWeek = startWeek ;
        this.endWeek = endWeek ;
        this.year = year ;
    }

    // ===== Lifecycle =====
    @PreUpdate @PrePersist
    private void updateGrade (){
        updatedAt = Instant.now();
    }

}
