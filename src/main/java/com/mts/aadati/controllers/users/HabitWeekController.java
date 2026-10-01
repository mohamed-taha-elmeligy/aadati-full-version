package com.mts.aadati.controllers.users;

import com.mts.aadati.dto.mapper.HabitCalendarMapper;
import com.mts.aadati.dto.mapper.HabitWeekMapper;
import com.mts.aadati.dto.response.HabitCalendarResponse;
import com.mts.aadati.dto.response.HabitWeekResponse;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.services.HabitWeekService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Uri.BASE + Uri.USER + Uri.HABIT_WEEK)
@Validated
@RequiredArgsConstructor
public class HabitWeekController {

    private final HabitWeekService weekService;
    private final HabitWeekMapper weekMapper;
    private final HabitCalendarMapper calendarMapper;

    @GetMapping("/first")
    public ResponseEntity<HabitWeekResponse> getFirst(){
        return ResponseEntity.ok(
                weekMapper.toResponse(
                        weekService.findFirst()
                )
        );
    }

    @GetMapping("/recent")
    public ResponseEntity<List<HabitWeekResponse>> getRecent(){
        return ResponseEntity.ok(
                weekMapper.toResponseList(
                        weekService.findTopByCreatedAtDesc()
                )
        );
    }

    @GetMapping("/number/{weekNumber}/year/{year}")
    public ResponseEntity<HabitWeekResponse> findByWeekNumberAndYear(
            @Min(1) @Max(53) @PathVariable("weekNumber") int weekNumber,
            @PathVariable("year") int year
    ){
        return ResponseEntity.ok(
                weekMapper.toResponse(
                        weekService.findByWeekNumberAndYear(weekNumber, year)
                )
        );
    }

    @GetMapping("/year/{year}/page/{page}")
    public ResponseEntity<PageModel<HabitWeekResponse>> findByYear(
            @PathVariable("year") int year,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                weekService.findByYear(year, pageNumber)
                        .map(weekMapper::toResponse)
        );
    }

    @GetMapping("/asc/page/{page}")
    public ResponseEntity<PageModel<HabitWeekResponse>> getAllByStartWeekAsc(
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                weekService.findAllByStartWeekAsc(pageNumber)
                        .map(weekMapper::toResponse)
        );
    }

    @GetMapping("/start/between/start/{start}/end/{end}/page/{page}")
    public ResponseEntity<PageModel<HabitWeekResponse>> findByStartWeekBetween(
            @PathVariable("start") LocalDate start,
            @PathVariable("end") LocalDate end,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                weekService.findByStartWeekBetween(start, end, pageNumber)
                        .map(weekMapper::toResponse)
        );
    }

    @GetMapping("/end/between/start/{start}/end/{end}/page/{page}")
    public ResponseEntity<PageModel<HabitWeekResponse>> findByEndWeekBetween(
            @PathVariable("start") LocalDate start,
            @PathVariable("end") LocalDate end,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        return ResponseEntity.ok(
                weekService.findByEndWeekBetween(start, end, pageNumber)
                        .map(weekMapper::toResponse)
        );
    }

    @GetMapping("/{weekId}/calendars")
    public ResponseEntity<List<HabitCalendarResponse>> getHabitCalendarsByWeekId(
            @PathVariable("weekId") UUID weekId
    ){
        return ResponseEntity.ok(
                calendarMapper.toResponseList(
                        weekService.findHabitCalendarsByWeekId(weekId)
                )
        );
    }
}