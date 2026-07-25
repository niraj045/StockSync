package com.stocksync.auth.service;

import com.stocksync.auth.dto.*;
import com.stocksync.auth.entity.Role;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.mapper.UserMapper;
import com.stocksync.auth.repository.RoleRepository;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.audit.service.UserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;
    private final UserActivityLogService auditLogService;
    private final HttpServletRequest request;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, SessionRegistry sessionRegistry,
                       UserActivityLogService auditLogService, HttpServletRequest request) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
        this.auditLogService = auditLogService;
        this.request = request;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String search, Boolean active, String role, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), searchPattern),
                        cb.like(cb.lower(root.get("username")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern)
                ));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (role != null && !role.trim().isEmpty()) {
                predicates.add(cb.equal(root.join("roles").get("id"), role.trim()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "User not found with ID: " + id));
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest dto) {
        // Enforce validations
        validatePasswordPolicy(dto.password());

        // Validate uniqueness case-insensitively
        if (userRepository.existsByUsernameIgnoreCase(dto.username())) {
            throw new BusinessRuleException("USERNAME_ALREADY_EXISTS", "Username already exists: " + dto.username());
        }
        if (userRepository.existsByEmailIgnoreCase(dto.email())) {
            throw new BusinessRuleException("EMAIL_ALREADY_EXISTS", "Email already exists: " + dto.email());
        }

        // Cross-field collision check
        if (userRepository.existsByEmailIgnoreCase(dto.username())) {
            throw new BusinessRuleException("USERNAME_ALREADY_EXISTS", "Username conflicts with an existing email: " + dto.username());
        }
        if (userRepository.existsByUsernameIgnoreCase(dto.email())) {
            throw new BusinessRuleException("EMAIL_ALREADY_EXISTS", "Email conflicts with an existing username: " + dto.email());
        }

        User user = new User();
        user.setFullName(dto.fullName());
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setActive(true);

        String currentAuditor = getCurrentAuditorUsername();
        user.setCreatedBy(currentAuditor);
        user.setUpdatedBy(currentAuditor);

        Set<Role> roles = dto.roles().stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> new BusinessRuleException("ROLE_NOT_FOUND", "Role not found: " + roleId)))
                .collect(Collectors.toSet());
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        auditLogService.log(savedUser.getId(), savedUser.getUsername(), "USER_CREATED", "User", savedUser.getId().toString(),
                "User created: " + savedUser.getUsername() + " by " + currentAuditor, request);

        return UserMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "User not found with ID: " + id));

        // Optimistic locking check
        if (!Objects.equals(user.getVersion(), dto.version())) {
            throw new ObjectOptimisticLockingFailureException(User.class, id);
        }

        // Case-insensitive uniqueness validations for email if changed
        if (!user.getEmail().equalsIgnoreCase(dto.email())) {
            if (userRepository.existsByEmailIgnoreCase(dto.email())) {
                throw new BusinessRuleException("EMAIL_ALREADY_EXISTS", "Email already exists: " + dto.email());
            }
            if (userRepository.existsByUsernameIgnoreCase(dto.email())) {
                throw new BusinessRuleException("EMAIL_ALREADY_EXISTS", "Email conflicts with an existing username: " + dto.email());
            }
            user.setEmail(dto.email());
        }

        user.setFullName(dto.fullName());
        String currentAuditor = getCurrentAuditorUsername();
        user.setUpdatedBy(currentAuditor);

        User savedUser = userRepository.save(user);

        auditLogService.log(savedUser.getId(), savedUser.getUsername(), "USER_UPDATED", "User", savedUser.getId().toString(),
                "User details updated: " + savedUser.getUsername() + " by " + currentAuditor, request);

        return UserMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse updateRoles(Long id, UserRolesUpdateRequest dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "User not found with ID: " + id));

        // Optimistic locking check
        if (!Objects.equals(user.getVersion(), dto.version())) {
            throw new ObjectOptimisticLockingFailureException(User.class, id);
        }

        // Last Admin Check (cannot remove ROLE_ADMIN if last active admin)
        boolean hasAdminRole = user.getRoles().stream().anyMatch(r -> r.getId().equals("ROLE_ADMIN"));
        boolean newHasAdminRole = dto.roles().contains("ROLE_ADMIN");

        if (hasAdminRole && !newHasAdminRole && user.isActive()) {
            long activeAdmins = userRepository.countByRolesIdAndActiveTrue("ROLE_ADMIN");
            if (activeAdmins <= 1) {
                throw new BusinessRuleException("LAST_ACTIVE_ADMIN_REQUIRED", "Cannot remove ADMIN role from the last active ADMIN");
            }
        }

        Set<Role> roles = dto.roles().stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> new BusinessRuleException("ROLE_NOT_FOUND", "Role not found: " + roleId)))
                .collect(Collectors.toSet());
        user.setRoles(roles);

        String currentAuditor = getCurrentAuditorUsername();
        user.setUpdatedBy(currentAuditor);

        User savedUser = userRepository.save(user);

        auditLogService.log(savedUser.getId(), savedUser.getUsername(), "USER_ROLES_UPDATED", "User", savedUser.getId().toString(),
                "User roles updated: " + savedUser.getUsername() + " roles: " + dto.roles() + " by " + currentAuditor, request);

        return UserMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "User not found with ID: " + id));

        user.setActive(true);
        String currentAuditor = getCurrentAuditorUsername();
        user.setUpdatedBy(currentAuditor);

        User savedUser = userRepository.save(user);

        auditLogService.log(savedUser.getId(), savedUser.getUsername(), "USER_ACTIVATED", "User", savedUser.getId().toString(),
                "User activated: " + savedUser.getUsername() + " by " + currentAuditor, request);

        return UserMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "User not found with ID: " + id));

        // Enforce self-deactivation rule
        String currentAuditor = getCurrentAuditorUsername();
        if (user.getUsername().equalsIgnoreCase(currentAuditor)) {
            throw new BusinessRuleException("CANNOT_DEACTIVATE_CURRENT_USER", "A user must not deactivate their own account");
        }

        // Last Admin Check
        boolean hasAdminRole = user.getRoles().stream().anyMatch(r -> r.getId().equals("ROLE_ADMIN"));
        if (hasAdminRole && user.isActive()) {
            long activeAdmins = userRepository.countByRolesIdAndActiveTrue("ROLE_ADMIN");
            if (activeAdmins <= 1) {
                throw new BusinessRuleException("LAST_ACTIVE_ADMIN_REQUIRED", "Cannot deactivate the last active ADMIN");
            }
        }

        user.setActive(false);
        user.setUpdatedBy(currentAuditor);

        User savedUser = userRepository.save(user);

        auditLogService.log(savedUser.getId(), savedUser.getUsername(), "USER_DEACTIVATED", "User", savedUser.getId().toString(),
                "User deactivated: " + savedUser.getUsername() + " by " + currentAuditor, request);

        // Invalidate all active sessions of this user
        invalidateUserSessions(user.getUsername());

        return UserMapper.toResponse(savedUser);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest dto) {
        String username = getCurrentAuditorUsername();
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Current user not found"));

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("CURRENT_PASSWORD_INVALID", "Current password is invalid");
        }

        validatePasswordPolicy(dto.newPassword());

        user.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
        user.setUpdatedBy(username);
        userRepository.save(user);

        auditLogService.log(user.getId(), user.getUsername(), "PASSWORD_CHANGED", "User", user.getId().toString(),
                "Password changed for user: " + user.getUsername(), request);

        // Invalidate other sessions
        invalidateOtherUserSessions(user.getUsername());
    }

    @Transactional
    public void updateLastLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        });
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessRuleException("PASSWORD_POLICY_VIOLATION", "Password must be at least 8 characters");
        }
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUppercase = true;
            else if (Character.isLowerCase(c)) hasLowercase = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasUppercase || !hasLowercase || !hasDigit) {
            throw new BusinessRuleException("PASSWORD_POLICY_VIOLATION",
                    "Password must contain at least one uppercase letter, one lowercase letter, and one digit");
        }
    }

    private String getCurrentAuditorUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "system";
        }
        return auth.getName();
    }

    private void invalidateUserSessions(String username) {
        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if (principal instanceof UserDetails userDetails) {
                if (userDetails.getUsername().equalsIgnoreCase(username)) {
                    List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                    for (SessionInformation sessionInfo : sessions) {
                        sessionInfo.expireNow();
                    }
                }
            }
        }
    }

    private void invalidateOtherUserSessions(String username) {
        String currentSessionId = null;
        try {
            RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
            currentSessionId = attributes.getSessionId();
        } catch (IllegalStateException e) {
            // Context not bound
        }

        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if (principal instanceof UserDetails userDetails) {
                if (userDetails.getUsername().equalsIgnoreCase(username)) {
                    List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                    for (SessionInformation sessionInfo : sessions) {
                        if (currentSessionId == null || !sessionInfo.getSessionId().equals(currentSessionId)) {
                            sessionInfo.expireNow();
                        }
                    }
                }
            }
        }
    }
}
