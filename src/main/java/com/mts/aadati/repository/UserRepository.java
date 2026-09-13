package com.mts.aadati.repository;

import com.mts.aadati.entities.Role;
import com.mts.aadati.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmailOrUsername(String email, String username);
    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END
        FROM User u
        WHERE (u.email = :email OR u.username = :username)
        AND u.userId <> :userId
        """)
    boolean existsByEmailOrUsernameExcludingUser(@Param("email") String email,
                                                 @Param("username") String username,
                                                 @Param("userId") UUID userId);


    @Query("SELECT u.emailVerified FROM User u WHERE u.email = :email AND u.emailVerified = true")
    boolean getEmailIsActive(@Param("email") String email);

    long countByEmailVerifiedTrue();
    long countByEmailVerifiedFalse();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end")
    long countUsersCreatedBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countUsersByRoleName(@Param("roleName") String roleName);


    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.userId = :userId AND r.isDeleted = false")
    List<Role> findRolesByUserId(@Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Role r JOIN r.users u WHERE u.userId = :userId AND r.name = :roleName")
    boolean userHasRole(@Param("userId") UUID userId, @Param("roleName") String roleName);

}