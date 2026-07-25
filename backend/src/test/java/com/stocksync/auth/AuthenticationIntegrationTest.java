package com.stocksync.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.auth.dto.ChangePasswordRequest;
import com.stocksync.auth.dto.LoginRequest;
import com.stocksync.auth.entity.Role;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.RoleRepository;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.auth.service.CustomUserDetails;
import com.stocksync.audit.entity.UserActivityLog;
import com.stocksync.audit.repository.UserActivityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthenticationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserActivityLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher;

    private User adminUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        Role adminRole = roleRepository.findById("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Admin")));

        adminUser = new User();
        adminUser.setFullName("System Admin");
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@stocksync.com");
        adminUser.setPasswordHash(passwordEncoder.encode("Password123"));
        adminUser.setActive(true);
        adminUser.setCreatedBy("test");
        adminUser.setUpdatedBy("test");
        adminUser.setRoles(Set.of(adminRole));
        adminUser = userRepository.save(adminUser);

        disabledUser = new User();
        disabledUser.setFullName("Disabled User");
        disabledUser.setUsername("disabled");
        disabledUser.setEmail("disabled@stocksync.com");
        disabledUser.setPasswordHash(passwordEncoder.encode("Password123"));
        disabledUser.setActive(false);
        disabledUser.setCreatedBy("test");
        disabledUser.setUpdatedBy("test");
        disabledUser.setRoles(Set.of(adminRole));
        disabledUser = userRepository.save(disabledUser);
    }

    @Test
    void loginSuccessfulWithUsername() throws Exception {
        LoginRequest request = new LoginRequest("admin", "Password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.fullName").value("System Admin"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Verify successful login audit
        List<UserActivityLog> logs = auditLogRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs.stream().anyMatch(log -> log.getAction().equals("LOGIN_SUCCESS")
                && log.getUsernameSnapshot().equals("admin"))).isTrue();
    }

    @Test
    void sessionLifecyclePublisherIsRegistered() {
        assertThat(httpSessionEventPublisher.getListener()).isInstanceOf(HttpSessionEventPublisher.class);
    }

    @Test
    void loginSuccessfulWithEmail() throws Exception {
        LoginRequest request = new LoginRequest("admin@stocksync.com", "Password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void loginFailsWithInvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest("admin", "WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        // Verify failed login audit
        List<UserActivityLog> logs = auditLogRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs.stream().anyMatch(log -> log.getAction().equals("LOGIN_FAILURE")
                && log.getUsernameSnapshot().equals("admin"))).isTrue();
    }

    @Test
    void loginFailsForDisabledUser() throws Exception {
        LoginRequest request = new LoginRequest("disabled", "Password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));

        // Verify failed login audit
        List<UserActivityLog> logs = auditLogRepository.findAll();
        assertThat(logs.stream().anyMatch(log -> log.getAction().equals("LOGIN_FAILURE")
                && log.getUsernameSnapshot().equals("disabled")
                && log.getDescription().contains("account is deactivated"))).isTrue();
    }

    @Test
    void getMeEndpoint() throws Exception {
        // First authenticate programmatically and get session
        LoginRequest loginRequest = new LoginRequest("admin", "Password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        mockMvc.perform(get("/api/v1/auth/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void logoutClearsSession() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "Password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // Perform logout (requires CSRF)
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());

        // Perform GET /me with same session, should fail
        mockMvc.perform(get("/api/v1/auth/me")
                        .session(session))
                .andExpect(status().isUnauthorized());

        // Verify logout audit
        List<UserActivityLog> logs = auditLogRepository.findAll();
        assertThat(logs.stream().anyMatch(log -> log.getAction().equals("LOGOUT")
                && log.getUsernameSnapshot().equals("admin"))).isTrue();
    }

    @Test
    void csrfProtectionBlocksStateChangingRequests() throws Exception {
        LoginRequest request = new LoginRequest("admin", "Password123");

        // Request without csrf() should fail with 403 Forbidden
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePasswordValidatesCurrentPasswordAndComplexity() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "Password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // 1. Invalid current password
        ChangePasswordRequest invalidCurrent = new ChangePasswordRequest("WrongPass", "NewPassword123");
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCurrent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_INVALID"));

        // 2. Password complexity violation (short password)
        ChangePasswordRequest shortPassword = new ChangePasswordRequest("Password123", "Short1");
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_POLICY_VIOLATION"));

        // 3. Password complexity violation (no digit)
        ChangePasswordRequest noDigit = new ChangePasswordRequest("Password123", "NoDigitHere");
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noDigit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_POLICY_VIOLATION"));

        // 4. Successful change
        ChangePasswordRequest successRequest = new ChangePasswordRequest("Password123", "NewPassword123");
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(successRequest)))
                .andExpect(status().isNoContent());

        // Verify successful audit
        List<UserActivityLog> logs = auditLogRepository.findAll();
        assertThat(logs.stream().anyMatch(log -> log.getAction().equals("PASSWORD_CHANGED")
                && log.getUsernameSnapshot().equals("admin"))).isTrue();
    }

    @Test
    void passwordChangeInvalidatesOtherSessionsButKeepsCurrentSession() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "Password123");

        MvcResult firstLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession currentSession = (MockHttpSession) firstLogin.getRequest().getSession();

        MvcResult secondLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession otherSession = (MockHttpSession) secondLogin.getRequest().getSession();

        List<SessionInformation> sessionsBeforeChange = sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> principal instanceof CustomUserDetails details
                        && details.getUsername().equalsIgnoreCase("admin"))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, true).stream())
                .toList();
        assertThat(sessionsBeforeChange).hasSize(2);

        ChangePasswordRequest request = new ChangePasswordRequest("Password123", "NewPassword123");
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(csrf())
                        .session(currentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        SessionInformation currentSessionInformation = sessionRegistry.getSessionInformation(currentSession.getId());
        SessionInformation otherSessionInformation = sessionRegistry.getSessionInformation(otherSession.getId());
        assertThat(currentSessionInformation).isNotNull();
        assertThat(currentSessionInformation.isExpired()).isFalse();
        assertThat(otherSessionInformation).isNotNull();
        assertThat(otherSessionInformation.isExpired()).isTrue();

        mockMvc.perform(get("/api/v1/auth/me").session(currentSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/me").session(otherSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
    }
}
