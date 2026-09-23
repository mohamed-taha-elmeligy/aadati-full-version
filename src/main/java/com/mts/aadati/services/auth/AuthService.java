package com.mts.aadati.services.auth;

import com.mts.aadati.configs.security.jwt.JwtBlocklistService;
import com.mts.aadati.configs.security.jwt.JwtUtils;
import com.mts.aadati.dto.mapper.UserMapper;
import com.mts.aadati.dto.request.LoginRequest;
import com.mts.aadati.dto.request.UserRequest;
import com.mts.aadati.dto.response.AuthResponse;
import com.mts.aadati.dto.response.UserResponse;
import com.mts.aadati.exceptions.exception.InvalidCredentialsException;
import com.mts.aadati.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtBlocklistService jwtBlocklistService;
    private final UserService userService;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(UserRequest request) {
        return userMapper.toResponse(
                userService.addUser(
                        userMapper.toEntity(request)
                )
        );
    }

    public AuthResponse login(LoginRequest login){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        login.username(),
                        login.password()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String accessToken = jwtUtils.generateAccessToken(userDetails);
        String refreshToken = jwtUtils.generateRefreshToken(userDetails);
        return new AuthResponse(accessToken,refreshToken);
    }

    public AuthResponse refresh(String refreshToken){
        String username = jwtUtils.extractUsername(refreshToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtUtils.isRefreshTokenValid(refreshToken, userDetails))
            throw new InvalidCredentialsException("Invalid refresh token");

        return new AuthResponse(jwtUtils.generateAccessToken(userDetails),refreshToken);
    }

    public void
    logout(String accessToken, String refreshToken){
        jwtBlocklistService.block(
                jwtUtils.extractJti(accessToken),
                jwtUtils.extractExpiration(accessToken).getTime()
        );
        jwtBlocklistService.block(
                jwtUtils.extractJti(refreshToken),
                jwtUtils.extractExpiration(refreshToken).getTime()
        );
    }
}
