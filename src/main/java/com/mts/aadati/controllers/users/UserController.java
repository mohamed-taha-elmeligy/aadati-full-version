package com.mts.aadati.controllers.users;

import com.mts.aadati.configs.security.details.CustomUserDetails;
import com.mts.aadati.dto.mapper.RoleMapper;
import com.mts.aadati.dto.mapper.UserMapper;
import com.mts.aadati.dto.request.RefreshTokenRequest;
import com.mts.aadati.dto.request.UserRequest;
import com.mts.aadati.dto.response.RoleResponse;
import com.mts.aadati.dto.response.UserResponse;
import com.mts.aadati.services.UserService;
import com.mts.aadati.services.auth.AuthService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Uri.BASE + Uri.USER)
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final AuthService authService;

    @PutMapping("/update")
    public ResponseEntity<UserResponse> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserRequest userRequest
            ){

        return ResponseEntity.ok(
                userMapper.toResponse(
                        userService.updateUser(
                                userDetails.id(),
                                userRequest
                        )
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody RefreshTokenRequest refreshRequest
    ){
        String accessToken = authHeader.substring(7);
        authService.logout(accessToken, refreshRequest.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        userService.removeUserById(userDetails.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<UserResponse> profile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return ResponseEntity.ok(
                userMapper.toResponse(
                        userService.getUser(
                                userDetails.id()
                        )
                )
        );
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getRoles(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return ResponseEntity.ok(
                roleMapper.toResponseList(
                        userService.getRolesByUserId(
                                userDetails.id()
                        )
                )
        );
    }

    @GetMapping("/check/role/{role}")
    public ResponseEntity<Boolean> hasRoles(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @NotBlank @PathVariable("role") String role
    ){
        return ResponseEntity.ok(
                userService.hasRole(
                        userDetails.id(),
                        role
                )
        );
    }


}