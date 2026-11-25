package com.daybasket.config;

import com.daybasket.entity.Role;
import com.daybasket.entity.User;
import com.daybasket.repository.RoleRepository;
import com.daybasket.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
@ConditionalOnProperty(
    name = "app.admin.bootstrap.enabled",
    havingValue = "true"
)
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_BOOTSTRAP_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_BOOTSTRAP_PASSWORD:}")
    private String adminPassword;

    @Value("${ADMIN_BOOTSTRAP_NAME:}")
    private String adminName;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {

        if (adminEmail.isBlank()
                || adminPassword.isBlank()
                || adminName.isBlank()) {
            throw new IllegalStateException(
                "Admin bootstrap credentials are missing"
            );
        }

        String email = adminEmail.trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                "This email already exists. No account was modified."
            );
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() ->
                    new IllegalStateException(
                        "ADMIN role does not exist"
                    )
                );

        LocalDateTime now = LocalDateTime.now();

        User admin = new User();

        admin.setFullName(adminName.trim());
        admin.setEmail(email);
        admin.setPhone(null);
        admin.setPasswordHash(
            passwordEncoder.encode(adminPassword)
        );
        admin.setRole(adminRole);
        admin.setIsActive(true);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);

        userRepository.save(admin);

        System.out.println(
            "ADMIN ACCOUNT CREATED SUCCESSFULLY"
        );
    }
}