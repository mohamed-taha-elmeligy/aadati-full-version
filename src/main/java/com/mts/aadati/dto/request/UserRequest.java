package com.mts.aadati.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserRequest (
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 30, message = "First name must be between 2 and 30 characters")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s.'-]+$", message = "Name can only contain letters, spaces, dots, hyphens and apostrophes")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s.'-]+$", message = "Name can only contain letters, spaces, dots, hyphens and apostrophes")
        String lastName,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 200, message = "Password must be between 8 and 200 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Password must contain uppercase, lowercase and a digit"
        )
        String password,

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(min = 5, max = 254, message = "Email must be between 5 and 254 characters")
        String email
) {
}
