package com.mts.aadati.seed.seeders;

import com.mts.aadati.entities.Role;
import com.mts.aadati.repository.RoleRepository;
import com.mts.aadati.seed.Seeder;
import com.mts.aadati.services.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleSeeder implements Seeder {

    private final RoleRepository roleRepository;
    private final RoleService roleService;

    private static final List<Role> DEFAULT_ROLES = List.of(
            new Role(
                    "ROLE_VIEWER",
                    "Read-only access. Can view data but cannot modify anything."
            ),new Role(
                    "ROLE_USER",
                    "Regular user role. Can only log in, create, update, and track their own habits."
            ),new Role(
                    "ROLE_ADMIN",
                    "System administrator. Can manage all users and categories and view global analytics."
            )
    );

    @Override
    public void seed() {

        if (roleRepository.count() > 0) {
            log.debug("Roles already seeded ({} existing), skipping", roleRepository.count());
            return;
        }

        log.debug("Seeding default roles");
        for (Role role : DEFAULT_ROLES)
            roleService.addRole(role);

        log.info("Default roles seeded");
    }

    @Override
    public int order() {
        return 10;
    }
}
