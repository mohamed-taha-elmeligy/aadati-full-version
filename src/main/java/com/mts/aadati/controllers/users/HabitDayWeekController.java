package com.mts.aadati.controllers.users;

import com.mts.aadati.configs.security.details.CustomUserDetails;
import com.mts.aadati.dto.mapper.HabitDayWeekMapper;
import com.mts.aadati.dto.mapper.HabitMapper;
import com.mts.aadati.dto.response.HabitDayWeekResponse;
import com.mts.aadati.dto.response.HabitResponse;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.entities.Habit;
import com.mts.aadati.services.HabitDayWeekService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping(Uri.BASE + Uri.USER + Uri.DAY_WEEK)
@RequiredArgsConstructor
@Validated
public class HabitDayWeekController {

    private final HabitDayWeekService dayWeekService;
    private final HabitDayWeekMapper dayWeekMapper;
    private final HabitMapper habitMapper;

    @GetMapping("/count/day/{day}")
    public ResponseEntity<Long> countHabitsByDayAndUser(
            @PathVariable("day") DayOfWeek dayOfWeek,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return ResponseEntity.ok(
                dayWeekService.countHabitsByDayAndUser(dayOfWeek,userDetails.id())
        );
    }

    @GetMapping("/habit/day/{day}/page/{page}")
    public ResponseEntity<PageModel<HabitResponse>> getHabitsByDayAndUser(
            @PathVariable("day") DayOfWeek dayOfWeek,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        PageModel<Habit> habitPageModel = dayWeekService.findHabitsByDayAndUser(
                dayOfWeek,
                userDetails.id(),
                pageNumber
        );

        return ResponseEntity.ok(
                habitPageModel.map(habitMapper::toResponse)
        );
    }

    @GetMapping("/asc")
    public ResponseEntity<List<HabitDayWeekResponse>> getDayAsc(){
        return ResponseEntity.ok(
                dayWeekMapper.toResponseList(
                        dayWeekService.findByDayAsc()
                )
        );
    }
}
