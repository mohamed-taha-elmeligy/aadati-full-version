
package com.mts.aadati.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@Getter
@Entity @Table(name = "habit_day_week" , indexes = @Index(name = "inx_habit_day_week",columnList = "day_of_week"))
public class HabitDayWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "day_week_id" , nullable = false , updatable = false)
    private long dayWeekId ;

    @Column(name = "day_of_week" , nullable = false ,length = 10 ,unique = true)
    @Enumerated(EnumType.STRING)
    @Setter
    private DayOfWeek dayOfWeek ;

    // =====  Relationship =====
    @ManyToMany(mappedBy = "habitDayWeeks",fetch = FetchType.LAZY)
    private final List<Habit> habits =new ArrayList<>();


    // ===== Builder Constructor =====

    @Builder
    public HabitDayWeek (@NonNull DayOfWeek dayOfWeek){
        this.dayOfWeek = dayOfWeek ;
    }

}
