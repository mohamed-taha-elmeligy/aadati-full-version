package com.mts.aadati.controllers.users;

import com.mts.aadati.dto.mapper.HabitTaskMapper;
import com.mts.aadati.dto.request.HabitTaskRequest;
import com.mts.aadati.dto.response.HabitTaskResponse;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.enums.RecurrenceType;
import com.mts.aadati.configs.security.details.CustomUserDetails;
import com.mts.aadati.services.HabitTaskService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Uri.BASE + Uri.USER + Uri.HABIT_TASK)
@Validated
@RequiredArgsConstructor
public class HabitTaskController {

    private final HabitTaskService habitTaskService;
    private final HabitTaskMapper habitTaskMapper;

    @PostMapping
    public ResponseEntity<HabitTaskResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody HabitTaskRequest request
    ){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                habitTaskMapper.toResponse(
                        habitTaskService.addHabitTask(userDetails.id(), request)
                )
        );
    }

    @PutMapping("/{habitTaskId}")
    public ResponseEntity<HabitTaskResponse> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("habitTaskId") UUID habitTaskId,
            @Valid @RequestBody HabitTaskRequest request
    ){
        return ResponseEntity.ok(
                habitTaskMapper.toResponse(
                        habitTaskService.updateHabitTask(habitTaskId, userDetails.id(), request)
                )
        );
    }

    @DeleteMapping("/{habitTaskId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("habitTaskId") UUID habitTaskId
    ){
        habitTaskService.deleteHabitTask(habitTaskId, userDetails.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{habitTaskId}/toggle-active")
    public ResponseEntity<HabitTaskResponse> toggleActive(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("habitTaskId") UUID habitTaskId
    ){
        return ResponseEntity.ok(
                habitTaskMapper.toResponse(
                        habitTaskService.toggleActive(habitTaskId, userDetails.id())
                )
        );
    }

    @GetMapping("/{habitTaskId}")
    public ResponseEntity<HabitTaskResponse> getById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("habitTaskId") UUID habitTaskId
    ){
        return ResponseEntity.ok(
                habitTaskMapper.toResponse(
                        habitTaskService.findByIdAndUser(habitTaskId, userDetails.id())
                )
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<HabitTaskResponse>> searchByTitle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @NotBlank @RequestParam("title") String title
    ){
        return ResponseEntity.ok(
                habitTaskMapper.toResponseList(
                        habitTaskService.searchByTitle(userDetails.id(), title)
                )
        );
    }

    @GetMapping("/filter/page/{page}")
    public ResponseEntity<PageModel<HabitTaskResponse>> filterTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "priorityLevelId", required = false) UUID priorityLevelId,
            @RequestParam(value = "recurrenceType", required = false) RecurrenceType recurrenceType,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                habitTaskService.filterTasks(userDetails.id(), categoryId, priorityLevelId, recurrenceType, pageNumber)
                        .map(habitTaskMapper::toResponse)
        );
    }

    @GetMapping("/active/page/{page}")
    public ResponseEntity<PageModel<HabitTaskResponse>> getAllActive(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                habitTaskService.findAllActiveByUser(userDetails.id(), pageNumber)
                        .map(habitTaskMapper::toResponse)
        );
    }

    @GetMapping("/inactive/page/{page}")
    public ResponseEntity<PageModel<HabitTaskResponse>> getAllInactive(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                habitTaskService.findAllInactiveByUser(userDetails.id(), pageNumber)
                        .map(habitTaskMapper::toResponse)
        );
    }

    @GetMapping("/start-date/page/{page}")
    public ResponseEntity<PageModel<HabitTaskResponse>> getByStartDate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                habitTaskService.findByStartDate(userDetails.id(), startDate, pageNumber)
                        .map(habitTaskMapper::toResponse)
        );
    }

    @GetMapping("/count/start-date")
    public ResponseEntity<Long> countByStartDate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate
    ){
        return ResponseEntity.ok(
                habitTaskService.countByStartDate(userDetails.id(), startDate)
        );
    }

    @GetMapping("/count/recurrence-type/{recurrenceType}")
    public ResponseEntity<Long> countByRecurrenceType(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("recurrenceType") RecurrenceType recurrenceType
    ){
        return ResponseEntity.ok(
                habitTaskService.countByRecurrenceType(userDetails.id(), recurrenceType)
        );
    }

    @GetMapping("/count/priority-level/{priorityLevelId}")
    public ResponseEntity<Long> countByPriorityLevel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("priorityLevelId") UUID priorityLevelId
    ){
        return ResponseEntity.ok(
                habitTaskService.countByPriorityLevel(userDetails.id(), priorityLevelId)
        );
    }

    @GetMapping("/count/category/{categoryId}")
    public ResponseEntity<Long> countByCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("categoryId") UUID categoryId
    ){
        return ResponseEntity.ok(
                habitTaskService.countByCategory(userDetails.id(), categoryId)
        );
    }
}