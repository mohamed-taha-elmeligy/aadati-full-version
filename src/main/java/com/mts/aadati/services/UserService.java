package com.mts.aadati.services;

import com.mts.aadati.dto.mapper.UserMapper;
import com.mts.aadati.dto.request.UserRequest;
import com.mts.aadati.entities.Role;
import com.mts.aadati.entities.User;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public User addUser(User user) {
        if (userRepository.existsByEmailOrUsername(user.getEmail(), user.getUsername()))
            throw new DuplicateResourceException("Email or username already exists");

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(UUID userID, UserRequest request) {

        if (userRepository.existsByEmailOrUsernameExcludingUser(request.email(), request.username(),userID))
            throw new DuplicateResourceException("Email or username already exists");

        User existUser = getUserOrThrow(userID);

        if (StringUtils.hasText(request.email()) && !request.email().equals(existUser.getEmail()) )
            existUser.setEmailVerified(false);

        userMapper.update(request, existUser);

        if (StringUtils.hasText(request.password()))
            existUser.setPassword(passwordEncoder.encode(request.password()));

        return userRepository.save(existUser);
    }

    @Transactional
    public void removeUserById(UUID userId) {
        userRepository.delete(getUserOrThrow(userId));
    }

    public boolean isEmailActive(String email) {
        if (!StringUtils.hasText(email))
            throw new InvalidRequestException("Email is required to check activation status");

        if (!userRepository.existsByEmail(email))
            throw new ResourceNotFoundException("Not found user by email");

        return userRepository.getEmailIsActive(email);
    }

    public User findByUsername(String username) {
        if (!StringUtils.hasText(username))
            throw new InvalidRequestException("Username is required");

        return userRepository.findByUsername(username)
                .orElseThrow(()->
                        new ResourceNotFoundException("Not found user by username")
                );
    }

    public User findByEmail(String email) {
        if (!StringUtils.hasText(email))
            throw new InvalidRequestException("Email is required");

        return userRepository.findByEmail(email)
                .orElseThrow(()->
                        new  ResourceNotFoundException("Not found user by email")
                );
    }

    public long countEmailVerified() {
        return userRepository.countByEmailVerifiedTrue();
    }

    public long countEmailUnverified() {
        return userRepository.countByEmailVerifiedFalse();
    }

    public long countCreatedBetween(Instant start, Instant end) {
        if (start == null || end == null)
            throw new InvalidRequestException("Start and end dates cannot be null");

        return userRepository.countUsersCreatedBetween(start, end);
    }

    public long countByRoleName(String roleName) {
        if (!StringUtils.hasText(roleName))
            throw new InvalidRequestException("Role name cannot be null or blank");

        return userRepository.countUsersByRoleName(roleName);
    }

    public List<Role> getRolesByUserId(UUID userId) {
        return getUserOrThrow(userId).getRoles();
    }

    public boolean hasRole(UUID userId, String roleName) {
        if (!StringUtils.hasText(roleName))
            throw new InvalidRequestException("Role name cannot be null or blank");

        return getUserOrThrow(userId).getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
    }

    private User getUserOrThrow(UUID userId) {
        if(userId == null)
            throw new InvalidRequestException("User ID can't be null");

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found by id"));
    }

}