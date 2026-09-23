package com.mts.aadati.entities;

import com.mts.aadati.configs.auditing.Auditing;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;


@Entity
@Table(name = "percentage_day" , uniqueConstraints = @UniqueConstraint(
        name = "uk_user_calendar",
        columnNames = {"user_id", "habit_calendar_id"})
        , indexes = {
        @Index(name = "inx_percentage_day_rate",columnList = "rate")
        ,@Index(name = "inx_percentage_day_updated",columnList = "updated_at")
})
@NoArgsConstructor
@Getter
public class PercentageDay extends Auditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "percentage_day_id", nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID percentageDayId ;

    @Setter
    @Column(name = "rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    // === Relationships ===
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_calendar_id", nullable = false)
    private HabitCalendar habitCalendar;

    // === Constructor ===
    public PercentageDay(BigDecimal rate,
                         @NonNull HabitCalendar habitCalendar,
                         @NonNull User user) {
        this.rate = rate != null ? rate : BigDecimal.ZERO;
        this.habitCalendar = habitCalendar;
        this.user = user;
    }

    // === Business Methods ===
    public void updateRate(BigDecimal newRate) {
        if (newRate == null) {
            throw new IllegalArgumentException("Rate cannot be null");
        }
        if (newRate.compareTo(BigDecimal.ZERO) < 0
                || newRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Rate must be between 0 and 100");
        }
        this.rate = newRate;
    }
}
