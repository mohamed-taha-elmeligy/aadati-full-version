package com.mts.aadati.dto.response;

import lombok.*;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken
) {}