package com.mts.aadati.controllers.admins;

import com.mts.aadati.dto.mapper.HabitCategoryMapper;
import com.mts.aadati.dto.request.HabitCategoryRequest;
import com.mts.aadati.dto.response.HabitCategoryResponse;
import com.mts.aadati.services.HabitCategoryService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Uri.BASE + Uri.ADMIN + Uri.HABIT_CATEGORY)
@Validated
@RequiredArgsConstructor
public class AdminHabitCategoryController {

    private final HabitCategoryService categoryService;
    private final HabitCategoryMapper categoryMapper;

    @PostMapping
    public ResponseEntity<HabitCategoryResponse> create(@Valid @RequestBody HabitCategoryRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                categoryMapper.toResponse(
                        categoryService.addCategory(categoryMapper.toEntity(request))
                )
        );
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<HabitCategoryResponse> update(
            @PathVariable("categoryId") UUID categoryId,
            @Valid @RequestBody HabitCategoryRequest request
    ){
        return ResponseEntity.ok(
                categoryMapper.toResponse(
                        categoryService.updateCategory(categoryId, request)
                )
        );
    }

    @PatchMapping("/{categoryId}/toggle-deleted")
    public ResponseEntity<HabitCategoryResponse> toggleDeleted(@PathVariable("categoryId") UUID categoryId){
        return ResponseEntity.ok(
                categoryMapper.toResponse(
                        categoryService.toggleDeleted(categoryId)
                )
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<HabitCategoryResponse>> searchByName(
            @NotBlank @RequestParam("name") String name
    ){
        return ResponseEntity.ok(
                categoryMapper.toResponseList(
                        categoryService.searchByName(name)
                )
        );
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<HabitCategoryResponse> findByName(
            @NotBlank @PathVariable("name") String name
    ){
        return ResponseEntity.ok(
                categoryMapper.toResponse(
                        categoryService.findByName(name)
                )
        );
    }

    @GetMapping("/asc")
    public ResponseEntity<List<HabitCategoryResponse>> getAllOrderByNameAsc(){
        return ResponseEntity.ok(
                categoryMapper.toResponseList(
                        categoryService.findAllOrderByNameAsc()
                )
        );
    }

    @GetMapping("/created/desc")
    public ResponseEntity<List<HabitCategoryResponse>> getAllOrderByCreatedAtDesc(){
        return ResponseEntity.ok(
                categoryMapper.toResponseList(
                        categoryService.findAllOrderByCreatedAtDesc()
                )
        );
    }

    @GetMapping("/updated/desc")
    public ResponseEntity<List<HabitCategoryResponse>> getAllOrderByUpdatedAtDesc(){
        return ResponseEntity.ok(
                categoryMapper.toResponseList(
                        categoryService.findAllOrderByUpdatedAtDesc()
                )
        );
    }

    @GetMapping("/updated/between")
    public ResponseEntity<List<HabitCategoryResponse>> findByUpdatedAtBetween(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ){
        return ResponseEntity.ok(
                categoryMapper.toResponseList(
                        categoryService.findByUpdatedAtBetween(start, end)
                )
        );
    }

    @GetMapping("/created/between")
    public ResponseEntity<List<HabitCategoryResponse>> findByCreatedAtBetween(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ){
        return ResponseEntity.ok(
                categoryMapper.toResponseList(
                        categoryService.findByCreatedAtBetween(start, end)
                )
        );
    }
}