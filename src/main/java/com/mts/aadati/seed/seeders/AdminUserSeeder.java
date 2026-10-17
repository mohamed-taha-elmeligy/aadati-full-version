package com.mts.aadati.seed.seeders;

import com.mts.aadati.entities.Role;
import com.mts.aadati.entities.User;
import com.mts.aadati.repository.UserRepository;
import com.mts.aadati.seed.Seeder;
import com.mts.aadati.services.RoleService;
import com.mts.aadati.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements Seeder {

    private final UserRepository userRepository;
    private final UserService userService;
    private final RoleService roleService;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public void seed() {

        if (userRepository.existsByUsername(adminUsername)) {
            log.debug("Admin user '{}' already exists, skipping", adminUsername);
            return;
        }

        log.debug("Seeding default admin user");

        List<Role> managedRoles = roleService.findAllDeletedFalse();
        if (managedRoles.isEmpty()) {
            log.error("Cannot seed admin user — no roles found. " +
                    "Make sure RoleSeeder runs before AdminUserSeeder.");
            return;
        }

        User admin = new User(
                "Mohamed",
                "Taha Abdo Taha",
                adminUsername,
                adminPassword,
                adminEmail,
                true
        );

        admin.setRoles(managedRoles);

        if (userService.addUser(admin) != null) {
            log.info("Admin user '{}' seeded with roles: {}", adminUsername, managedRoles);
        } else {
            log.error("Failed to seed admin user");
        }
    }

    @Override
    public int order() {
        return 40;
    }
}
