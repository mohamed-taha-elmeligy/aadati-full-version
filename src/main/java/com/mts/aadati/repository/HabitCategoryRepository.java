package com.mts.aadati.repository;

import com.mts.aadati.entities.HabitCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitCategoryRepository extends JpaRepository<HabitCategory, UUID> {

    List<HabitCategory> findByNameContainingIgnoreCase(String name);
    List<HabitCategory> findByNameContainingIgnoreCaseAndIsDeletedFalse(String name);


    List<HabitCategory> findByUpdatedAtBetween(Instant start , Instant end);
    List<HabitCategory> findByCreatedAtBetween(Instant start , Instant end);
    List<HabitCategory> findAllByOrderByCreatedAtDesc();
    List<HabitCategory> findAllByOrderByUpdatedAtDesc();

    List<HabitCategory> findAllByOrderByNameAsc();
    List<HabitCategory> findAllByIsDeletedFalseOrderByNameAsc();

    boolean existsByName(String name);
    boolean existsByColor(String color);
    boolean existsByNameAndHabitCategoryIdNot(String name, UUID categoryId);
    boolean existsByColorAndHabitCategoryIdNot(String color, UUID categoryId);

    Optional<HabitCategory> findByNameAndIsDeletedFalse(String name);
    Optional<HabitCategory> findByName(String name);

}
