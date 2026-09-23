package com.mts.aadati.entities;

import com.mts.aadati.configs.auditing.Auditing;
import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.NaturalId;

import java.util.*;

@NoArgsConstructor
@Getter
@Entity @Table(name = "role",
        indexes = @Index(name = "inx_role_name", columnList = "name")
)
public class Role extends Auditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id", nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID roleId;

    @Column(name = "name", length = 50, nullable = false , unique = true)
    @Setter
    @NaturalId
    private String name;

    @Column(name = "description", length = 1000)
    @Setter
    private String description;

    @Column(name = "is_deleted", nullable = false)
    @Setter
    private boolean isDeleted;


    // ===== RelationShip =====
    @ManyToMany(mappedBy = "roles",fetch = FetchType.LAZY)
    private final List<User> users =new ArrayList<>();

    // ===== Constructor =====
    public Role(@NonNull String name, String description) {
        this.name = name;
        this.description = description;
        this.isDeleted = false;
    }
}
