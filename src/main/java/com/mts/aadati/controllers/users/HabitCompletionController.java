package com.mts.aadati.controllers.users;

import com.mts.aadati.configs.security.details.CustomUserDetails;
import com.mts.aadati.dto.mapper.HabitCompletionMapper;
import com.mts.aadati.dto.request.HabitCompletionRequest;
import com.mts.aadati.dto.response.HabitCompletionResponse;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.entities.HabitCompletion;
import com.mts.aadati.services.HabitCompletionService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(Uri.BASE + Uri.USER + Uri.HABIT_COMPLETION)
@RequiredArgsConstructor
@Validated
public class HabitCompletionController {

    private final HabitCompletionService habitCompletionService;
    private final HabitCompletionMapper habitCompletionMapper;

    @PutMapping("/update")
    public ResponseEntity<HabitCompletionResponse> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody HabitCompletionRequest request
    ){

        return ResponseEntity.ok(
                habitCompletionMapper.toResponse(
                        habitCompletionService.updateHabitCompletion(
                                userDetails.id(),
                                request
                        )
                )
        );
    }

    @GetMapping("/completion/{completion}")
    public ResponseEntity<HabitCompletionResponse> findByCompletionIdAndUser(
            @PathVariable("completion") UUID habitCompletionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){

        return ResponseEntity.ok(
                habitCompletionMapper.toResponse(
                        habitCompletionService.findByCompletionIdAndUser(
                                habitCompletionId,
                                userDetails.id()
                        )
                )
        );
    }

    @GetMapping("/all/page/{page}")
    public ResponseEntity<PageModel<HabitCompletionResponse>> getAllByUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        PageModel<HabitCompletion> completion = habitCompletionService.findAllByUser(
                userDetails.id(),
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(
                        habitCompletionMapper::toResponse
                )
        );
    }

    @GetMapping("/all/date/start/{start}/end/{end}/page/{page}")
    public ResponseEntity<PageModel<HabitCompletionResponse>> getByUserAndCompletedAtBetween(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("start") Instant start,
            @PathVariable("end") Instant end,
            @Min(0)@PathVariable("page") int pageNumber
    ){
        PageModel<HabitCompletion> completion = habitCompletionService.findByUserAndCompletedAtBetween(
                userDetails.id(),
                start,
                end,
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(
                        habitCompletionMapper::toResponse
                )
        );
    }

    @GetMapping("/all/habit/{habit}/complete/{complete}/page/{page}")
    public ResponseEntity<PageModel<HabitCompletionResponse>> getByHabitAndUserAndComplete(
            @PathVariable("habit") UUID habitId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("complete") boolean complete,
            @Min(0)@PathVariable("page") int pageNumber
    ){
        PageModel<HabitCompletion> completion = habitCompletionService.findByHabitAndUserAndComplete(
                habitId,
                userDetails.id(),
                complete,
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(
                        habitCompletionMapper::toResponse
                )
        );
    }

    @GetMapping("/all/habit/{habit}/page/{page}")
    public ResponseEntity<PageModel<HabitCompletionResponse>> getByHabitAndUser(
            @PathVariable("habit") UUID habitId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0)@PathVariable("page") int pageNumber
    ){
        PageModel<HabitCompletion> completion = habitCompletionService.findByHabitAndUser(
                habitId,
                userDetails.id(),
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(
                        habitCompletionMapper::toResponse
                )
        );
    }

    @GetMapping("/all/calendar/{calendar}/page/{page}")
    public ResponseEntity<PageModel<HabitCompletionResponse>> getByHabitCalendarAndUser(
            @PathVariable("calendar") UUID habitCalendarId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0)@PathVariable("page") int pageNumber
    ){
        PageModel<HabitCompletion> completion = habitCompletionService.findByHabitCalendarAndUser(
                habitCalendarId,
                userDetails.id(),
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(
                        habitCompletionMapper::toResponse
                )
        );
    }

    @GetMapping("/search/habit/title/{title}/page/{page}")
    public ResponseEntity<PageModel<HabitCompletionResponse>> findByHabitTitleContainingAndUser(
            @PathVariable("title") String title,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        PageModel<HabitCompletion> completion = habitCompletionService.findByHabitTitleContainingAndUser(
                title,
                userDetails.id(),
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(
                        habitCompletionMapper::toResponse
                )
        );
    }

    @GetMapping("/today/complete/{complete}/page/{page}")
    public ResponseEntity<PageModel<HabitCompletionResponse>> getTodayByUserAndComplete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("complete") boolean complete,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        PageModel<HabitCompletion> completion = habitCompletionService.findTodayByUserAndComplete(
                userDetails.id(),
                complete,
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(
                        habitCompletionMapper::toResponse
                )
        );
    }

    @GetMapping("/today/page/{page}")
    public ResponseEntity<PageModel<HabitCompletionResponse>> getTodayByUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        PageModel<HabitCompletion> completion = habitCompletionService.findTodayByUser(
                userDetails.id(),
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(
                        habitCompletionMapper::toResponse
                )
        );
    }

    @GetMapping("/count/calendar/{calendar}/complete/{complete}")
    public ResponseEntity<Long> countByCalendarAndUserAndComplete(
            @PathVariable("calendar") UUID habitCalendarId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("complete") boolean complete
    ){
        return ResponseEntity.ok(
                habitCompletionService.countByCalendarAndUserAndComplete(
                        habitCalendarId,
                        userDetails.id(),
                        complete
                )
        );
    }

    @GetMapping("/count/habitid/{habit}/complete/{complete}")
    public ResponseEntity<Long> countByHabitAndUserAndComplete(
            @PathVariable("habit") UUID habitId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("complete") boolean complete
    ){
        return ResponseEntity.ok(
                habitCompletionService.countByHabitAndUserAndComplete(
                        habitId,
                        userDetails.id(),
                        complete
                )
        );
    }
}
