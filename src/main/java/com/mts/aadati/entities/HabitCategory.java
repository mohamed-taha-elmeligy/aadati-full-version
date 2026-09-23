package com.mts.aadati.entities;

import com.mts.aadati.configs.auditing.Auditing;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "habit_category",
        indexes = @Index(name = "inx_habit_category_name", columnList = "name"))
public class HabitCategory extends Auditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "habit_category_id", nullable = false , updatable = false ,columnDefinition = "UUID")
    private UUID habitCategoryId;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    @Setter
    private String name;

    @Column(name = "description", length = 800)
    @Setter
    private String description;

    @Column(name = "color", nullable = false, unique = true ,length = 7)
    @Setter
    private String color;

    @Column(name = "is_deleted")
    @Setter
    private boolean isDeleted = false;

    // =====  Relationship =====
    @OneToMany(mappedBy = "habitCategory" ,fetch = FetchType.LAZY ,
            cascade = {CascadeType.DETACH, CascadeType.MERGE ,CascadeType.PERSIST ,CascadeType.REFRESH})
    private final List<Habit> habits = new ArrayList<>() ;

    @OneToMany(mappedBy = "habitCategory" ,fetch = FetchType.LAZY ,
            cascade = {CascadeType.DETACH, CascadeType.MERGE ,CascadeType.PERSIST ,CascadeType.REFRESH})
    private final List<HabitTask> habitTasks = new ArrayList<>() ;

    // ===== Constructor =====
    public HabitCategory(@NonNull String name, String description, String color) {
        this.name = name;
        this.description = description;
        this.color = (color != null && !color.trim().isEmpty()) ? color : generateRandomColor();
    }

    // ===== Helper Method for Color =====
    private String generateRandomColor() {
        return String.format("#%06x", ThreadLocalRandom.current().nextInt(0x1000000));
    }
}
