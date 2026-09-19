package com.mts.aadati.configs.security.details;

import com.mts.aadati.entities.User;
import com.mts.aadati.exceptions.exception.ResourceNotFoundException;
import com.mts.aadati.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService ;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user ;

        try { user = userService.findByUsername(username); }
        catch (ResourceNotFoundException e)
        { throw new UsernameNotFoundException("User not found by username: " + username);}

        Set<GrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(role-> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        return new CustomUserDetails(
                user.getUserId(),
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.isEmailVerified(),
                authorities
        );

    }
}
