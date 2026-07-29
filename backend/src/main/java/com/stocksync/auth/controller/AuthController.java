package com.stocksync.auth.controller;

import com.stocksync.auth.dto.*;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.mapper.UserMapper;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.auth.service.CustomUserDetails;
import com.stocksync.auth.service.UserService;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.audit.service.UserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserActivityLogService auditLogService;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public AuthController(AuthenticationManager authenticationManager, UserService userService,
                          UserRepository userRepository, UserActivityLogService auditLogService,
                          SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            String token = csrfToken.getToken();
            return ResponseEntity.noContent()
                    .header("X-CSRF-TOKEN", token)
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-CSRF-TOKEN")
                    .build();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest loginDto,
                                              HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.usernameOrEmail(), loginDto.password())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

            // Bind security context to HTTP session for state persistence
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            // Update last login timestamp
            userService.updateLastLogin(user.getId());

            // Log successful login audit
            auditLogService.log(user.getId(), user.getUsername(), "LOGIN_SUCCESS", "Auth", user.getId().toString(),
                    "Successful login for user: " + user.getUsername(), request);

            return ResponseEntity.ok(UserMapper.toResponse(user));
        } catch (BadCredentialsException ex) {
            // Log failure without password details
            auditLogService.log(null, loginDto.usernameOrEmail(), "LOGIN_FAILURE", "Auth", null,
                    "Failed login attempt: invalid credentials", request);
            throw ex;
        } catch (DisabledException ex) {
            User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(loginDto.usernameOrEmail(), loginDto.usernameOrEmail()).orElse(null);
            Long userId = user != null ? user.getId() : null;
            auditLogService.log(userId, loginDto.usernameOrEmail(), "LOGIN_FAILURE", "Auth", null,
                    "Failed login attempt: account is deactivated", request);
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            User user = userRepository.findByUsernameIgnoreCase(auth.getName()).orElse(null);
            Long userId = user != null ? user.getId() : null;

            // Capture identity before session invalidation
            auditLogService.log(userId, auth.getName(), "LOGOUT", "Auth", userId != null ? userId.toString() : null,
                    "User logged out: " + auth.getName(), request);
        }

        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByUsernameIgnoreCase(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));

        if (!user.isActive()) {
            throw new DisabledException("Account is disabled");
        }

        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordDto) {
        userService.changePassword(changePasswordDto);
        return ResponseEntity.noContent().build();
    }
}
