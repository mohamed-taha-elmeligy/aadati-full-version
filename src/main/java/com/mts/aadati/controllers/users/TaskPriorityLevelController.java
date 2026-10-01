package com.mts.aadati.controllers.users;

import com.mts.aadati.dto.mapper.TaskPriorityLevelMapper;
import com.mts.aadati.dto.response.TaskPriorityLevelResponse;
import com.mts.aadati.services.TaskPriorityLevelService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Uri.BASE + Uri.USER + Uri.PRIORITY_LEVEL)
@RequiredArgsConstructor
@Validated
public class TaskPriorityLevelController {

    private final TaskPriorityLevelService priorityLevelService;
    private final TaskPriorityLevelMapper priorityLevelMapper;

    @GetMapping("/asc")
    public ResponseEntity<List<TaskPriorityLevelResponse>> getActivePriorityLevelAsc(){
        return ResponseEntity.ok(
                priorityLevelMapper.toResponseList(
                        priorityLevelService.findActivePriorityLevelAsc()
                )
        );
    }

    @GetMapping("/desc")
    public ResponseEntity<List<TaskPriorityLevelResponse>> getActivePriorityLevelDesc(){
        return ResponseEntity.ok(
                priorityLevelMapper.toResponseList(
                        priorityLevelService.findActivePriorityLevelDesc()
                )
        );
    }

    @GetMapping("/colors")
    public ResponseEntity<List<String>> getActiveAllColor(){
        return ResponseEntity.ok(
                priorityLevelService.getActiveAllColor()
        );
    }

    @GetMapping("/search/{level}")
    public ResponseEntity<TaskPriorityLevelResponse> findActivePriorityLevel(
            @Min(1) @Max(50) @PathVariable("level") int priorityLevel
    ){
        return ResponseEntity.ok(
                priorityLevelMapper.toResponse(
                        priorityLevelService.findActivePriorityLevel(
                                priorityLevel
                        )
                )
        );
    }
}
