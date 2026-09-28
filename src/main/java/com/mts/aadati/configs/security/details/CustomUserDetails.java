package com.mts.aadati.configs.security.details;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.UUID;

public record CustomUserDetails(
        UUID id,
        String username,
        String password,
        String email,
        boolean emailVerified,
        Collection<? extends GrantedAuthority> authorities

) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @JsonIgnore
    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isEnabled() { return emailVerified; }

    @Override
    @NonNull
    public String toString() {
        return "CustomUserDetails{id=%s, username='%s', email='%s', emailVerified=%s, authorities=%s}"
                .formatted(id, username, email, emailVerified, authorities);
    }
}
