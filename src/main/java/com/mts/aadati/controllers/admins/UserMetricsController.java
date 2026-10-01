package com.mts.aadati.controllers.admins;

import com.mts.aadati.services.UserService;
import com.mts.aadati.utils.uris.Uri;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping(Uri.BASE + Uri.ADMIN + "/count" )
@RequiredArgsConstructor
@Validated
public class UserMetricsController {

    private final UserService userService;

    @GetMapping("/email/verified")
    public ResponseEntity<Long> countEmailVerified(){
        return ResponseEntity.ok(
                userService.countEmailVerified()
        );
    }

    @GetMapping("/email/unverified")
    public ResponseEntity<Long> countEmailUnverified(){
        return ResponseEntity.ok(
                userService.countEmailUnverified()
        );
    }

    @GetMapping("/created/start/{start}/end/{end}")
    public ResponseEntity<Long> countCreatedBetween(
            @PathVariable("start") Instant start,
            @PathVariable("end") Instant end
    ){
        return ResponseEntity.ok(
                userService.countCreatedBetween(start, end)
        );
    }

    @GetMapping("/byrole/{role}")
    public ResponseEntity<Long> countByRole(
            @NotBlank @PathVariable("role") String role
    ){
        return ResponseEntity.ok(
                userService.countByRoleName(role)
        );
    }
}
