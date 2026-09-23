package com.mts.aadati.entities;

import com.mts.aadati.configs.auditing.Auditing;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "percentage_week" , uniqueConstraints = @UniqueConstraint(
        name = "uk_user_calendar",
        columnNames = {"user_id", "habit_week_id"})
        , indexes = {
        @Index(name = "inx_percentage_week_rate",columnList = "rate")
        ,@Index(name = "inx_percentage_week_updated",columnList = "updated_at")
        ,@Index(name = "inx_percentage_week_grade",columnList = "grade")
})
@NoArgsConstructor
@Getter
public class PercentageWeek extends Auditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "percentage_week_id", nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID percentageWeekId ;

    @Setter
    @Column(name = "rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "grade" , nullable = false , length = 100 )
    private String grade ;

    // === Relationships ===
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_week_id", nullable = false)
    private HabitWeek habitWeek;

    // === Constructor ===
    public PercentageWeek(BigDecimal rate,
                         @NonNull HabitWeek habitWeek,
                         @NonNull User user) {
        this.rate = rate != null ? rate : BigDecimal.ZERO;
        this.habitWeek = habitWeek;
        this.user = user;
    }

    private String resolveGradeText() {
        int rateInt = rate.intValue();
        if (rateInt <= 20) return "Faint Spark – A modest start, needs a boost of energy.";
        if (rateInt <= 40) return "First Step – Some activity, but a strong push forward is needed.";
        if (rateInt <= 60) return "Solid Presence – Noticeable energy, with plenty of room to grow.";
        if (rateInt <= 75) return "Positive Vibes – Active and inspiring, keep this momentum going.";
        if (rateInt <= 90) return "Blazing Spirit – High energy that catches attention and motivates others.";
        if (rateInt <= 100) return "Peak Performance – Exceptional drive, your energy sets the bar high!";
        return "Invalid rating";
    }


    @PrePersist
    @PreUpdate
    private void calculateGrade() {
        this.grade = resolveGradeText();
    }
}
