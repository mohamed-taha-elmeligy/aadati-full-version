package com.mts.aadati.controllers.admins;

import com.mts.aadati.dto.mapper.UserMapper;
import com.mts.aadati.dto.request.AdminCreateUserRequest;
import com.mts.aadati.dto.response.UserResponse;
import com.mts.aadati.entities.User;
import com.mts.aadati.services.UserService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping(Uri.BASE + Uri.ADMIN)
@RequiredArgsConstructor
@Validated
public class AdminUserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/count/email/verified")
    public ResponseEntity<Long> countEmailVerified(){
        return ResponseEntity.ok(
                userService.countEmailVerified()
        );
    }

    @GetMapping("/count/email/unverified")
    public ResponseEntity<Long> countEmailUnverified(){
        return ResponseEntity.ok(
                userService.countEmailUnverified()
        );
    }

    @GetMapping("/count/created/start/{start}/end/{end}")
    public ResponseEntity<Long> countCreatedBetween(
            @PathVariable("start") Instant start,
            @PathVariable("end") Instant end
    ){
        return ResponseEntity.ok(
                userService.countCreatedBetween(start, end)
        );
    }

    @GetMapping("/count/byrole/{role}")
    public ResponseEntity<Long> countByRole(
            @NotBlank @PathVariable("role") String role
    ){
        return ResponseEntity.ok(
                userService.countByRoleName(role)
        );
    }

    @PostMapping
    public ResponseEntity<UserResponse> addUser(@Valid @RequestBody AdminCreateUserRequest request) {
        User user = userMapper.toEntity(request);
        User saved = userService.addUser(user, request.roleIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(saved));
    }
}
