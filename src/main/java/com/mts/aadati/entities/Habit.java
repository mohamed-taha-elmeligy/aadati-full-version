package com.mts.aadati.entities;

import com.mts.aadati.configs.auditing.Auditing;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NaturalId;
import java.util.*;

@NoArgsConstructor
@Getter
@Entity @Table(name = "habit",
        uniqueConstraints = @UniqueConstraint(columnNames = {"title","user_id"}),
        indexes = {
        @Index(name = "inx_habit_title", columnList = "title"),
        @Index(name = "inx_habit_point", columnList = "point")
})
public class Habit extends Auditing {

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "habit_id", nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID habitId;

    @Column(name = "title", length = 50, nullable = false)
    @Setter
    @NaturalId
    private String title;

    @Column(name = "point", nullable = false)
    @Setter
    private double point = 1.0;

    @Column(name = "type", nullable = false)
    @Setter
    private boolean type = true;

    @Column(name = "description", length = 1000)
    @Setter
    private String description;

    @Column(name = "is_active", nullable = false)
    @Setter
    private boolean isActive = true;

    // =====  Relationship =====
    @OneToMany(mappedBy = "habit" , fetch = FetchType.LAZY , orphanRemoval = true , cascade = CascadeType.ALL)
    private final List<HabitCompletion> habitCompletions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false)
    @Setter
    private User user ;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_category_id" , nullable = false)
    private HabitCategory habitCategory ;

    @ManyToMany(cascade = {CascadeType.DETACH,CascadeType.MERGE,CascadeType.PERSIST,CascadeType.REFRESH},
    fetch = FetchType.LAZY )
    @JoinTable(name = "habit_day_of_week",
            joinColumns = @JoinColumn(name = "habit_id" ,nullable = false),
            inverseJoinColumns = @JoinColumn(name ="day_week_id",nullable = false ))
    private final List<HabitDayWeek> habitDayWeeks = new ArrayList<>();

    // ===== Builder Constructor =====

    @Builder
    public Habit(@NonNull String title,
                 double point, boolean type,
                 String description,
                 boolean isActive,
                 @NonNull User user ,
                 @NonNull HabitCategory habitCategory ) {
        this.title = title;
        this.point = (point > 0) ? point : 1.0;
        this.type = type ;
        this.description = description;
        this.isActive =  isActive;
        this.user = user ;
        this.habitCategory = habitCategory ;
    }

    public Habit activate() {
        this.isActive = true;
        return this;
    }
    public Habit deactivate() {
        this.isActive = false;
        return this;
    }

}
