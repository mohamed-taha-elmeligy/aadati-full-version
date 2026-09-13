package com.mts.aadati.repository;

import com.mts.aadati.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findByNameContainingIgnoreCase(String name);
    Optional<Role> findByNameIgnoreCase(String name);
    List<Role> findAllByIsDeletedFalse();
    List<Role> findAllByIsDeletedTrue();

    long countByIsDeletedFalse();

    boolean existsByNameAndRoleIdNot(String name, UUID roleId);
    boolean existsByName(String name );

}
