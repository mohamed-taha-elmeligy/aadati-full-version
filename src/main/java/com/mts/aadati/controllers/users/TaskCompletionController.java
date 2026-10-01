package com.mts.aadati.controllers.users;

import com.mts.aadati.configs.security.details.CustomUserDetails;
import com.mts.aadati.dto.mapper.TaskCompletionMapper;
import com.mts.aadati.dto.request.TaskCompletionRequest;
import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.dto.response.TaskCompletionResponse;
import com.mts.aadati.entities.TaskCompletion;
import com.mts.aadati.services.TaskCompletionService;
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
@RequestMapping(Uri.BASE + Uri.USER + Uri.TASK_COMPLETION)
@RequiredArgsConstructor
@Validated
public class TaskCompletionController {

    private final TaskCompletionService taskCompletionService;
    private final TaskCompletionMapper taskCompletionMapper;

    @PutMapping("/update")
    public ResponseEntity<TaskCompletionResponse> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TaskCompletionRequest request
    ){
        return ResponseEntity.ok(
                taskCompletionMapper.toResponse(
                        taskCompletionService.updateCompletion(
                                userDetails.id(),
                                request
                        )
                )
        );
    }

    @GetMapping("/count/taskid/{taskId}/complete/{complete}")
    public ResponseEntity<Long> countByTaskAndUserAndComplete(
            @PathVariable("taskId") UUID taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("complete") boolean complete
    ){
        return ResponseEntity.ok(
                taskCompletionService.countByHabitTaskAndUserAndComplete(
                        taskId,
                        userDetails.id(),
                        complete
                )
        );
    }

    @GetMapping("/search/task/title/{title}/page/{page}")
    public ResponseEntity<PageModel<TaskCompletionResponse>> searchByTaskTitleAndUser(
            @PathVariable("title") String title,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
            ){
        PageModel<TaskCompletion> completion = taskCompletionService.findByHabitTaskTitleContainingAndUser(
                title,
                userDetails.id(),
                pageNumber);

        return ResponseEntity.ok(
                completion.map(taskCompletionMapper::toResponse)
        );
    }

    @GetMapping("/all/complete/{complete}/page/{page}")
    public ResponseEntity<PageModel<TaskCompletionResponse>> getAllByUserAndComplete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("complete") boolean complete,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        PageModel<TaskCompletion> completion = taskCompletionService.findAllByUserAndComplete(
                userDetails.id(),
                complete,
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(taskCompletionMapper::toResponse)
        );
    }

    @GetMapping("/all/page/{page}")
    public ResponseEntity<PageModel<TaskCompletionResponse>> getAllByUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        PageModel<TaskCompletion> completion = taskCompletionService.findAllByUser(
                userDetails.id(),
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(taskCompletionMapper::toResponse)
        );
    }

    @GetMapping("/all/start/{start}/end/{end}/page/{page}")
    public ResponseEntity<PageModel<TaskCompletionResponse>> getByUserAndCompletedAtBetween(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("start") Instant start,
            @PathVariable("end") Instant end,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        PageModel<TaskCompletion> completion = taskCompletionService.findByUserAndCompletedAtBetween(
                userDetails.id(),
                start,
                end,
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(taskCompletionMapper::toResponse)
        );
    }

    @GetMapping("/all/taskid/{taskid}/complete/{complete}/page/{page}")
    public ResponseEntity<PageModel<TaskCompletionResponse>> getByHabitTaskAndUserAndComplete(
            @PathVariable("taskid") UUID taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("complete") boolean complete,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        PageModel<TaskCompletion> completion = taskCompletionService.findByHabitTaskAndUserAndComplete(
                taskId,
                userDetails.id(),
                complete,
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(taskCompletionMapper::toResponse)
        );
    }

    @GetMapping("/all/calendarId/{calendarId}/page/{page}")
    public ResponseEntity<PageModel<TaskCompletionResponse>> getByHabitCalendarAndUser(
            @PathVariable("calendarId") UUID calendarId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        PageModel<TaskCompletion> completion = taskCompletionService.findByHabitCalendarAndUser(
                calendarId,
                userDetails.id(),
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(taskCompletionMapper::toResponse)
        );
    }

    @GetMapping("/all/taskid/{taskid}/page/{page}")
    public ResponseEntity<PageModel<TaskCompletionResponse>> findByHabitTaskAndUser(
            @PathVariable("taskid") UUID taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Min(0) @PathVariable("page") int pageNumber
    ){
        PageModel<TaskCompletion> completion = taskCompletionService.findByHabitTaskAndUser(
                taskId,
                userDetails.id(),
                pageNumber
        );

        return ResponseEntity.ok(
                completion.map(taskCompletionMapper::toResponse)
        );
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<TaskCompletionResponse> findByCompletionIdAndUser(
            @PathVariable("id") UUID taskCompletionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return ResponseEntity.ok(
                taskCompletionMapper.toResponse(
                        taskCompletionService.findByCompletionIdAndUser(
                                taskCompletionId,
                                userDetails.id()
                        )
                )
        );
    }

}
