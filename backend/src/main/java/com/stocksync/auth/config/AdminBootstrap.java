package com.stocksync.auth.config;

import com.stocksync.auth.entity.Role;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.RoleRepository;
import com.stocksync.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${BOOTSTRAP_ADMIN_ENABLED:false}")
    private boolean enabled;

    @Value("${BOOTSTRAP_ADMIN_FULL_NAME:}")
    private String fullName;

    @Value("${BOOTSTRAP_ADMIN_USERNAME:}")
    private String username;

    @Value("${BOOTSTRAP_ADMIN_EMAIL:}")
    private String email;

    @Value("${BOOTSTRAP_ADMIN_PASSWORD:}")
    private String password;

    public AdminBootstrap(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            logger.info("Admin bootstrap is disabled.");
            return;
        }

        // Only bootstrap if no admin user exists
        long adminCount = userRepository.countByRolesIdAndActiveTrue("ROLE_ADMIN");
        if (adminCount > 0) {
            logger.info("Admin bootstrap skipped because active ADMIN(s) already exist.");
            return;
        }

        logger.info("Attempting to bootstrap system admin...");

        validateFields();

        // Check if there is collision with existing users (e.g. if we disable admin count check but have duplicate check)
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            logger.error("Admin bootstrap failed: Username '{}' already exists.", username);
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            logger.error("Admin bootstrap failed: Email '{}' already exists.", email);
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(username)) {
            logger.error("Admin bootstrap failed: Username '{}' conflicts with an existing email.", username);
            return;
        }
        if (userRepository.existsByUsernameIgnoreCase(email)) {
            logger.error("Admin bootstrap failed: Email '{}' conflicts with an existing username.", email);
            return;
        }

        Role adminRole = roleRepository.findById("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "System Administrator with full access")));

        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setActive(true);
        user.setCreatedBy("bootstrap");
        user.setUpdatedBy("bootstrap");
        user.setRoles(Set.of(adminRole));

        userRepository.save(user);

        logger.info("SUCCESS: Bootstrap admin '{}' created successfully (username: {}).", fullName, username);
    }

    private void validateFields() {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("BOOTSTRAP_ADMIN_FULL_NAME must not be empty");
        }
        if (username == null || username.trim().isEmpty() || username.length() < 3) {
            throw new IllegalArgumentException("BOOTSTRAP_ADMIN_USERNAME must be at least 3 characters");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("BOOTSTRAP_ADMIN_EMAIL must be a valid email address");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("BOOTSTRAP_ADMIN_PASSWORD must be at least 8 characters");
        }

        // Validate password complexity
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUppercase = true;
            else if (Character.isLowerCase(c)) hasLowercase = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasUppercase || !hasLowercase || !hasDigit) {
            throw new IllegalArgumentException("BOOTSTRAP_ADMIN_PASSWORD must contain at least one uppercase letter, one lowercase letter, and one digit");
        }
    }
}
