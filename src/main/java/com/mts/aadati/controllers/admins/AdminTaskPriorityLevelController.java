package com.mts.aadati.controllers.admins;

import com.mts.aadati.dto.mapper.TaskPriorityLevelMapper;
import com.mts.aadati.dto.request.TaskPriorityLevelRequest;
import com.mts.aadati.dto.response.TaskPriorityLevelResponse;
import com.mts.aadati.services.TaskPriorityLevelService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Uri.BASE + Uri.ADMIN + Uri.PRIORITY_LEVEL)
@RequiredArgsConstructor
@Validated
public class AdminTaskPriorityLevelController {
    private final TaskPriorityLevelService priorityLevelService;
    private final TaskPriorityLevelMapper priorityLevelMapper;

    @PostMapping("/add")
    public ResponseEntity<TaskPriorityLevelResponse> create(
            @Valid @RequestBody TaskPriorityLevelRequest request
    ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(priorityLevelMapper.toResponse(
                        priorityLevelService.addPriority(
                                priorityLevelMapper.toEntity(request)
                        )
                )
        );
    }

    @PutMapping("/update/{priorityID}")
    public ResponseEntity<TaskPriorityLevelResponse> update(
            @PathVariable("priorityID") UUID priorityID,
            @Valid @RequestBody TaskPriorityLevelRequest request
    ){
        return ResponseEntity.ok(
                priorityLevelMapper.toResponse(
                        priorityLevelService.updatePriority(
                                priorityID,
                                request
                        )
                )
        );
    }

    @PutMapping("/toggle/{priorityID}")
    public ResponseEntity<TaskPriorityLevelResponse> toggleDelete(
            @PathVariable("priorityID") UUID priorityID
    ){
        return ResponseEntity.ok(
                priorityLevelMapper.toResponse(
                        priorityLevelService.toggleDeleted(
                                priorityID
                        )
                )
        );
    }

    @GetMapping("/search/{level}")
    public ResponseEntity<TaskPriorityLevelResponse> findPriorityLevel(
            @Min(1) @Max(50) @PathVariable("level") int priorityLevel
    ){
        return ResponseEntity.ok(
                priorityLevelMapper.toResponse(
                        priorityLevelService.findPriorityLevel(
                                priorityLevel
                        )
                )
        );
    }

    @GetMapping("/asc")
    public ResponseEntity<List<TaskPriorityLevelResponse>> getPriorityLevelAsc(){
        return ResponseEntity.ok(
                priorityLevelMapper.toResponseList(
                        priorityLevelService.findPriorityLevelAsc()
                )
        );
    }

    @GetMapping("/desc")
    public ResponseEntity<List<TaskPriorityLevelResponse>> getPriorityLevelDesc(){
        return ResponseEntity.ok(
                priorityLevelMapper.toResponseList(
                        priorityLevelService.findPriorityLevelDesc()
                )
        );
    }

    @GetMapping("/colors")
    public ResponseEntity<List<String>> getAllColor(){
        return ResponseEntity.ok(
                priorityLevelService.getAllColor()
        );
    }

}