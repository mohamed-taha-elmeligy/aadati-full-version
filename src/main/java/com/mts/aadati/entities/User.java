package com.mts.aadati.entities;

import com.mts.aadati.configs.auditing.Auditing;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@NoArgsConstructor
@Getter
@Entity @Table(name = "users" , indexes = {
        @Index(name = "inx_user_first_name" , columnList = "first_name"),
        @Index(name = "inx_user_last_name" , columnList = "last_name"),
        @Index(name = "inx_user_email" , columnList = "email")
})
public class User extends Auditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID )
    @Column(name = "user_id" , nullable = false ,updatable = false , columnDefinition = "UUID")
    private UUID userId ;

    @Column(name = "first_name", nullable = false, length = 30)
    @Setter
    private String firstName ;

    @Column(name = "last_name", nullable = false, length = 50)
    @Setter
    private String lastName ;

    @Column(name = "username", nullable = false, unique = true, length = 30)
    @Setter
    private String username ;

    @Column(name = "password", nullable = false, length = 200)
    @Setter
    private String password ;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    @Setter
    private String email ;

    @Column(name = "email_verified" , nullable = false)
    @Setter
    private boolean emailVerified ;

    // ===== Relationship =====
    @OneToMany(mappedBy = "user" ,fetch = FetchType.LAZY ,cascade = CascadeType.ALL )
    private final List<Habit> habits = new ArrayList<>() ;

    @OneToMany(mappedBy = "user" ,fetch = FetchType.LAZY ,cascade = CascadeType.ALL )
    private final List<HabitTask> habitTasks = new ArrayList<>() ;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PercentageDay> percentageDays = new ArrayList<>();

    @Setter
    @ManyToMany(cascade ={CascadeType.PERSIST,CascadeType.MERGE,CascadeType.REFRESH}, fetch = FetchType.EAGER )
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "user_id" ,nullable = false),
            inverseJoinColumns = @JoinColumn(name ="role_id",nullable = false ))
    private List<Role> roles = new ArrayList<>();

    public User(@NonNull String firstName, @NonNull String lastName, @NonNull String username,
                @NonNull String password, @NonNull String email, boolean emailVerified) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.email = email;
        this.emailVerified = emailVerified;
    }

}
