package com.mts.aadati.controllers.users;

import com.mts.aadati.configs.security.details.CustomUserDetails;
import com.mts.aadati.dto.mapper.PercentageWeekMapper;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.dto.response.PercentageWeekResponse;
import com.mts.aadati.services.PercentageWeekService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(Uri.BASE + Uri.USER + Uri.PERCENTAGE_WEEK)
@Validated
@RequiredArgsConstructor
public class PercentageWeekController {

    private final PercentageWeekService percentageWeekService;
    private final PercentageWeekMapper percentageWeekMapper;

    @GetMapping("/week/{habitWeekId}")
    public ResponseEntity<PercentageWeekResponse> getByHabitWeek(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("habitWeekId") UUID habitWeekId
    ){
        return ResponseEntity.ok(
                percentageWeekMapper.toResponse(
                        percentageWeekService.findByUserAndHabitWeek(userDetails.id(), habitWeekId)
                )
        );
    }

    @GetMapping("/average")
    public ResponseEntity<Double> getAverageRate(@AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(
                percentageWeekService.findAverageRateByUser(userDetails.id())
        );
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCount(@AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(
                percentageWeekService.countByUser(userDetails.id())
        );
    }

    @GetMapping("/page/{page}")
    public ResponseEntity<PageModel<PercentageWeekResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageWeekService.findAllByUser(userDetails.id(), pageNumber)
                        .map(percentageWeekMapper::toResponse)
        );
    }

    @GetMapping("/rate/between/page/{page}")
    public ResponseEntity<PageModel<PercentageWeekResponse>> getAllByRateBetween(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("min") BigDecimal min,
            @RequestParam("max") BigDecimal max,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageWeekService.findAllByUserAndRateBetween(userDetails.id(), min, max, pageNumber)
                        .map(percentageWeekMapper::toResponse)
        );
    }

    @GetMapping("/completed/full/page/{page}")
    public ResponseEntity<PageModel<PercentageWeekResponse>> getFullyCompleted(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageWeekService.findFullyCompletedByUser(userDetails.id(), pageNumber)
                        .map(percentageWeekMapper::toResponse)
        );
    }

    @GetMapping("/completed/partial/page/{page}")
    public ResponseEntity<PageModel<PercentageWeekResponse>> getPartiallyCompleted(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageWeekService.findPartiallyCompletedByUser(userDetails.id(), pageNumber)
                        .map(percentageWeekMapper::toResponse)
        );
    }

    @GetMapping("/not-started/page/{page}")
    public ResponseEntity<PageModel<PercentageWeekResponse>> getNotStarted(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageWeekService.findNotStartedByUser(userDetails.id(), pageNumber)
                        .map(percentageWeekMapper::toResponse)
        );
    }

    @GetMapping("/updated/between/page/{page}")
    public ResponseEntity<PageModel<PercentageWeekResponse>> getAllByUpdatedBetween(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageWeekService.findAllByUserAndUpdatedBetween(userDetails.id(), start, end, pageNumber)
                        .map(percentageWeekMapper::toResponse)
        );
    }

    @GetMapping("/created/between/page/{page}")
    public ResponseEntity<PageModel<PercentageWeekResponse>> getAllByCreatedBetween(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageWeekService.findAllByUserAndCreatedBetween(userDetails.id(), start, end, pageNumber)
                        .map(percentageWeekMapper::toResponse)
        );
    }

    @GetMapping("/grade/{grade}/page/{page}")
    public ResponseEntity<PageModel<PercentageWeekResponse>> getAllByGrade(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @NotBlank @PathVariable("grade") String grade,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageWeekService.findAllByUserAndGrade(userDetails.id(), grade, pageNumber)
                        .map(percentageWeekMapper::toResponse)
        );
    }

    @GetMapping("/grade/search/page/{page}")
    public ResponseEntity<PageModel<PercentageWeekResponse>> searchByGradeKeyword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @NotBlank @RequestParam("keyword") String keyword,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageWeekService.searchByGradeKeyword(userDetails.id(), keyword, pageNumber)
                        .map(percentageWeekMapper::toResponse)
        );
    }
}