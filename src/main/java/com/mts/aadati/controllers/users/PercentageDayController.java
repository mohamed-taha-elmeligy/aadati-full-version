package com.mts.aadati.controllers.users;

import com.mts.aadati.dto.mapper.PercentageDayMapper;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.dto.response.PercentageDayResponse;
import com.mts.aadati.configs.security.details.CustomUserDetails;
import com.mts.aadati.services.PercentageDayService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Uri.BASE + Uri.USER + Uri.PERCENTAGE_DAY)
@Validated
@RequiredArgsConstructor
public class PercentageDayController {

    private final PercentageDayService percentageDayService;
    private final PercentageDayMapper percentageDayMapper;

    @GetMapping("/calendar/{habitCalendarId}")
    public ResponseEntity<PercentageDayResponse> getByHabitCalendar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("habitCalendarId") UUID habitCalendarId
    ){
        return ResponseEntity.ok(
                percentageDayMapper.toResponse(
                        percentageDayService.findByUserAndHabitCalendar(userDetails.id(), habitCalendarId)
                )
        );
    }

    @GetMapping("/week/{habitWeekId}")
    public ResponseEntity<List<PercentageDayResponse>> getByHabitWeek(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("habitWeekId") UUID habitWeekId
    ){
        return ResponseEntity.ok(
                percentageDayMapper.toResponseList(
                        percentageDayService.findByUserAndHabitWeek(userDetails.id(), habitWeekId)
                )
        );
    }

    @GetMapping("/average")
    public ResponseEntity<Double> getAverageRate(@AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(
                percentageDayService.findAverageRateByUser(userDetails.id())
        );
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCount(@AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(
                percentageDayService.countByUser(userDetails.id())
        );
    }

    @GetMapping("/page/{page}")
    public ResponseEntity<PageModel<PercentageDayResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageDayService.findAllByUser(userDetails.id(), pageNumber)
                        .map(percentageDayMapper::toResponse)
        );
    }

    @GetMapping("/rate/between/page/{page}")
    public ResponseEntity<PageModel<PercentageDayResponse>> getAllByRateBetween(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("min") BigDecimal min,
            @RequestParam("max") BigDecimal max,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageDayService.findAllByUserAndRateBetween(userDetails.id(), min, max, pageNumber)
                        .map(percentageDayMapper::toResponse)
        );
    }

    @GetMapping("/completed/full/page/{page}")
    public ResponseEntity<PageModel<PercentageDayResponse>> getFullyCompleted(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageDayService.findFullyCompletedByUser(userDetails.id(), pageNumber)
                        .map(percentageDayMapper::toResponse)
        );
    }

    @GetMapping("/completed/partial/page/{page}")
    public ResponseEntity<PageModel<PercentageDayResponse>> getPartiallyCompleted(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageDayService.findPartiallyCompletedByUser(userDetails.id(), pageNumber)
                        .map(percentageDayMapper::toResponse)
        );
    }

    @GetMapping("/not-started/page/{page}")
    public ResponseEntity<PageModel<PercentageDayResponse>> getNotStarted(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageDayService.findNotStartedByUser(userDetails.id(), pageNumber)
                        .map(percentageDayMapper::toResponse)
        );
    }

    @GetMapping("/updated/between/page/{page}")
    public ResponseEntity<PageModel<PercentageDayResponse>> getAllByUpdatedBetween(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageDayService.findAllByUserAndUpdatedBetween(userDetails.id(), start, end, pageNumber)
                        .map(percentageDayMapper::toResponse)
        );
    }

    @GetMapping("/created/between/page/{page}")
    public ResponseEntity<PageModel<PercentageDayResponse>> getAllByCreatedBetween(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                percentageDayService.findAllByUserAndCreatedBetween(userDetails.id(), start, end, pageNumber)
                        .map(percentageDayMapper::toResponse)
        );
    }
}