package com.mts.aadati.controllers.users;

import com.mts.aadati.configs.security.details.CustomUserDetails;
import com.mts.aadati.dto.mapper.HabitCalendarMapper;
import com.mts.aadati.dto.response.HabitCalendarResponse;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.services.HabitCalendarService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Uri.BASE + Uri.USER + Uri.HABIT_CALENDAR)
@RequiredArgsConstructor
@Validated
public class HabitCalendarController {

    private final HabitCalendarService calendarService;
    private final HabitCalendarMapper calendarMapper;

    @GetMapping("/first")
    public ResponseEntity<HabitCalendarResponse> getFirst(){
        return ResponseEntity.ok(
                calendarMapper.toResponse(
                        calendarService.findFirstByDate()
                )
        );
    }

    @GetMapping("/count/completed/tasks/week/{week}")
    public ResponseEntity<Long> countDaysCompletedTasksByWeek(
            @PathVariable("week") UUID weekId,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ){
        return ResponseEntity.ok(
                calendarService.countDaysCompletedTasksByWeek(
                        weekId,
                        userDetails.id()
                )
        );
    }

    @GetMapping("/count/completed/habits/week/{week}")
    public ResponseEntity<Long> countDaysCompletedHabitsByWeek(
            @PathVariable("week") UUID weekId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return ResponseEntity.ok(
                calendarService.countDaysCompletedHabitsByWeek(
                        weekId,
                        userDetails.id()
                )
        );
    }

    @GetMapping("/week/{week}/day/{day}")
    public ResponseEntity<HabitCalendarResponse> findByWeekAndDay(
            @PathVariable("week") UUID weekId,
            @PathVariable("day") DayOfWeek day
    ){
        return ResponseEntity.ok(
                calendarMapper.toResponse(
                        calendarService.findByWeekAndDay(weekId, day)
                )
        );
    }

    @GetMapping("/all/between/start/{start}/end/{end}/page/{page}")
    public ResponseEntity<PageModel<HabitCalendarResponse>> findByDateBetween(
            @PathVariable("start") LocalDate start,
            @PathVariable("end") LocalDate end,
            @Min(0) @PathVariable("page") int pageNumber

    ){
        PageModel<HabitCalendar> calendarPageModel = calendarService.findByDateBetween(start, end, pageNumber);

        return ResponseEntity.ok(
                calendarPageModel.map(calendarMapper::toResponse)
        );
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<HabitCalendarResponse> findByDate(
            @PathVariable("date") LocalDate date
    ){
        return ResponseEntity.ok(
                calendarMapper.toResponse(
                        calendarService.findByDate(date)
                )
        );
    }

    @GetMapping("/week/{week}")
    public ResponseEntity<List<HabitCalendarResponse>> findByHabitWeek(
            @PathVariable("week") UUID weekId
    ){
        return ResponseEntity.ok(
                calendarMapper.toResponseList(
                        calendarService.findByHabitWeek(weekId)
                )
        );
    }

}
