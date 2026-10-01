package com.mts.aadati.controllers.users;

import com.mts.aadati.dto.mapper.HabitCategoryMapper;
import com.mts.aadati.dto.response.HabitCategoryResponse;
import com.mts.aadati.services.HabitCategoryService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Uri.BASE + Uri.USER + Uri.HABIT_CATEGORY)
@Validated
@RequiredArgsConstructor
public class HabitCategoryController {

    private final HabitCategoryService categoryService;
    private final HabitCategoryMapper categoryMapper;

    @GetMapping("/search")
    public ResponseEntity<List<HabitCategoryResponse>> searchActiveByName(
            @NotBlank @RequestParam("name") String name
    ){
        return ResponseEntity.ok(
                categoryMapper.toResponseList(
                        categoryService.searchActiveByName(name)
                )
        );
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<HabitCategoryResponse> findActiveByName(
            @NotBlank @PathVariable("name") String name
    ){
        return ResponseEntity.ok(
                categoryMapper.toResponse(
                        categoryService.findActiveByName(name)
                )
        );
    }

    @GetMapping("/asc")
    public ResponseEntity<List<HabitCategoryResponse>> getAllActiveOrderByNameAsc(){
        return ResponseEntity.ok(
                categoryMapper.toResponseList(
                        categoryService.findAllActiveOrderByNameAsc()
                )
        );
    }
}