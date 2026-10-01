package com.mts.aadati.controllers.users;

import com.mts.aadati.dto.mapper.HabitMapper;
import com.mts.aadati.dto.request.HabitRequest;
import com.mts.aadati.dto.response.HabitResponse;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.configs.security.details.CustomUserDetails;
import com.mts.aadati.services.HabitService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Uri.BASE + Uri.USER + Uri.HABIT)
@Validated
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;
    private final HabitMapper habitMapper;

    @PostMapping
    public ResponseEntity<HabitResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody HabitRequest request
    ){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                habitMapper.toResponse(
                        habitService.addHabit(userDetails.id(), request)
                )
        );
    }

    @PutMapping("/{habitId}")
    public ResponseEntity<HabitResponse> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("habitId") UUID habitId,
            @Valid @RequestBody HabitRequest request
    ){
        return ResponseEntity.ok(
                habitMapper.toResponse(
                        habitService.updateHabit(habitId, userDetails.id(), request)
                )
        );
    }

    @DeleteMapping("/{habitId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("habitId") UUID habitId
    ){
        habitService.deleteHabit(habitId, userDetails.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{habitId}/toggle-active")
    public ResponseEntity<HabitResponse> toggleActive(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("habitId") UUID habitId
    ){
        return ResponseEntity.ok(
                habitMapper.toResponse(
                        habitService.toggleActive(habitId, userDetails.id())
                )
        );
    }

    @GetMapping("/{habitId}")
    public ResponseEntity<HabitResponse> getById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("habitId") UUID habitId
    ){
        return ResponseEntity.ok(
                habitMapper.toResponse(
                        habitService.findByIdAndUser(habitId, userDetails.id())
                )
        );
    }

    @GetMapping("/day/{dayOfWeek}")
    public ResponseEntity<List<HabitResponse>> getByDayOfWeek(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("dayOfWeek") DayOfWeek dayOfWeek
    ){
        return ResponseEntity.ok(
                habitMapper.toResponseList(
                        habitService.findByDayOfWeekAndUser(dayOfWeek, userDetails.id())
                )
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<HabitResponse>> searchByTitle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @NotBlank @RequestParam("title") String title
    ){
        return ResponseEntity.ok(
                habitMapper.toResponseList(
                        habitService.searchByTitle(title, userDetails.id())
                )
        );
    }

    @GetMapping("/filter/page/{page}")
    public ResponseEntity<PageModel<HabitResponse>> filterHabits(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "dayOfWeek", required = false) DayOfWeek dayOfWeek,
            @RequestParam(value = "type", required = false) Boolean type,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                habitService.filterHabits(userDetails.id(), categoryId, dayOfWeek, type, pageNumber)
                        .map(habitMapper::toResponse)
        );
    }

    @GetMapping("/active/page/{page}")
    public ResponseEntity<PageModel<HabitResponse>> getAllActive(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                habitService.findAllActiveByUser(userDetails.id(), pageNumber)
                        .map(habitMapper::toResponse)
        );
    }

    @GetMapping("/inactive/page/{page}")
    public ResponseEntity<PageModel<HabitResponse>> getAllInactive(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                habitService.findAllInactiveByUser(userDetails.id(), pageNumber)
                        .map(habitMapper::toResponse)
        );
    }

    @GetMapping("/count/active")
    public ResponseEntity<Long> countActive(@AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(
                habitService.countActiveByUser(userDetails.id())
        );
    }

    @GetMapping("/count/type/{type}")
    public ResponseEntity<Long> countByType(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("type") boolean type
    ){
        return ResponseEntity.ok(
                habitService.countByUserAndType(userDetails.id(), type)
        );
    }

    @GetMapping("/count/category/{categoryId}")
    public ResponseEntity<Long> countByCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("categoryId") UUID categoryId
    ){
        return ResponseEntity.ok(
                habitService.countByUserAndCategory(userDetails.id(), categoryId)
        );
    }

    @GetMapping("/count/day/{dayOfWeek}")
    public ResponseEntity<Long> countByDayOfWeek(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("dayOfWeek") DayOfWeek dayOfWeek
    ){
        return ResponseEntity.ok(
                habitService.countByUserAndDayOfWeek(userDetails.id(), dayOfWeek)
        );
    }

    @GetMapping("/count/point/less-than")
    public ResponseEntity<Long> countByPointLessThan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @DecimalMin("0.5") @DecimalMax("10.0") @RequestParam("point") double point
    ){
        return ResponseEntity.ok(
                habitService.countByUserAndPointLessThan(userDetails.id(), point)
        );
    }

    @GetMapping("/count/point/greater-than")
    public ResponseEntity<Long> countByPointGreaterThan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @DecimalMin("0.5") @DecimalMax("10.0") @RequestParam("point") double point
    ){
        return ResponseEntity.ok(
                habitService.countByUserAndPointGreaterThan(userDetails.id(), point)
        );
    }
}