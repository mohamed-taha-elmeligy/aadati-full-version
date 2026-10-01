package com.mts.aadati.controllers.auth;

import com.mts.aadati.dto.request.LoginRequest;
import com.mts.aadati.dto.request.RefreshTokenRequest;
import com.mts.aadati.dto.request.UserRequest;
import com.mts.aadati.dto.response.AuthResponse;
import com.mts.aadati.dto.response.UserResponse;
import com.mts.aadati.services.auth.AuthService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Uri.BASE + Uri.AUTH)
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody UserRequest userRequest
    ){
        log.info("REGISTER CONTROLLER REACHED");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(userRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    ){
        return ResponseEntity.ok(
                authService.login(
                        loginRequest
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ){
        return ResponseEntity.ok(
                authService.refresh(request.refreshToken())
        );
    }
}
