package com.stocksync.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.auth.dto.UserCreateRequest;
import com.stocksync.auth.dto.UserRolesUpdateRequest;
import com.stocksync.auth.dto.UserUpdateRequest;
import com.stocksync.auth.entity.Role;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.RoleRepository;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.audit.repository.UserActivityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserActivityLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User viewerUser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        Role adminRole = roleRepository.findById("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Admin")));
        Role viewerRole = roleRepository.findById("ROLE_VIEWER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_VIEWER", "Viewer")));

        adminUser = new User();
        adminUser.setFullName("Admin User");
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@stocksync.com");
        adminUser.setPasswordHash("hashedpassword");
        adminUser.setActive(true);
        adminUser.setCreatedBy("test");
        adminUser.setUpdatedBy("test");
        adminUser.setRoles(Set.of(adminRole));
        adminUser = userRepository.save(adminUser);

        viewerUser = new User();
        viewerUser.setFullName("Viewer User");
        viewerUser.setUsername("viewer");
        viewerUser.setEmail("viewer@stocksync.com");
        viewerUser.setPasswordHash("hashedpassword");
        viewerUser.setActive(true);
        viewerUser.setCreatedBy("test");
        viewerUser.setUpdatedBy("test");
        viewerUser.setRoles(Set.of(viewerRole));
        viewerUser = userRepository.save(viewerUser);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanCreateAndFetchUsers() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
                "Jane Doe",
                "janedoe",
                "jane@stocksync.com",
                "Password123",
                Set.of("ROLE_VIEWER")
        );

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("janedoe"))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_VIEWER"));

        // Fetch User
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void viewerCannotAdministerUsers() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
                "Unauthorized User",
                "unauthorized",
                "unauth@stocksync.com",
                "Password123",
                Set.of("ROLE_VIEWER")
        );

        // Viewer should receive 403 Forbidden for User management
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void crossFieldCollisionsAreBlocked() throws Exception {
        // 1. Create a user with username matching an existing email
        UserCreateRequest overlapUsername = new UserCreateRequest(
                "Overlap User",
                "viewer@stocksync.com", // username overlaps existing email
                "overlap@stocksync.com",
                "Password123",
                Set.of("ROLE_VIEWER")
        );

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlapUsername)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));

        // 2. Create a user with email matching an existing username
        User emailUser = new User();
        emailUser.setFullName("Collision User");
        emailUser.setUsername("collision@stocksync.com");
        emailUser.setEmail("collision_user@stocksync.com");
        emailUser.setPasswordHash("hashed");
        emailUser.setActive(true);
        emailUser.setCreatedBy("test");
        emailUser.setUpdatedBy("test");
        emailUser.setRoles(Set.of(roleRepository.findById("ROLE_VIEWER").get()));
        userRepository.save(emailUser);

        UserCreateRequest overlapEmail = new UserCreateRequest(
                "Overlap User 2",
                "overlap2",
                "collision@stocksync.com", // email overlaps existing username
                "Password123",
                Set.of("ROLE_VIEWER")
        );

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlapEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void optimisticLockingBlocksStaleUpdates() throws Exception {
        UserUpdateRequest staleUpdate = new UserUpdateRequest(
                "Updated Name",
                "admin@stocksync.com",
                999L // Incorrect version
        );

        mockMvc.perform(put("/api/v1/users/" + adminUser.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleUpdate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void lastAdminProtectionSafeguards() throws Exception {
        mockMvc.perform(post("/api/v1/users/" + adminUser.getId() + "/deactivate")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_DEACTIVATE_CURRENT_USER"));

        mockMvc.perform(post("/api/v1/users/" + adminUser.getId() + "/deactivate")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("external-admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LAST_ACTIVE_ADMIN_REQUIRED"));

        UserRolesUpdateRequest rolesUpdate = new UserRolesUpdateRequest(
                Set.of("ROLE_VIEWER"),
                adminUser.getVersion()
        );
        mockMvc.perform(put("/api/v1/users/" + adminUser.getId() + "/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolesUpdate))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("external-admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LAST_ACTIVE_ADMIN_REQUIRED"));
    }
}
