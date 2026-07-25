package com.stocksync.audit.service;

import com.stocksync.audit.entity.UserActivityLog;
import com.stocksync.audit.repository.UserActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserActivityLogService {

    private final UserActivityLogRepository logRepository;

    public UserActivityLogService(UserActivityLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String usernameSnapshot, String action, String entityType, String entityId, String description, HttpServletRequest request) {
        UserActivityLog log = new UserActivityLog();
        log.setUserId(userId);
        log.setUsernameSnapshot(usernameSnapshot != null ? usernameSnapshot : "anonymous");
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);

        if (request != null) {
            log.setRequestMethod(request.getMethod());
            log.setRequestPath(request.getRequestURI());
            log.setIpAddress(getIpAddress(request));
        }

        logRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<UserActivityLog> searchLogs(Long userId, String username, String action, Instant from, Instant to, Pageable pageable) {
        Specification<UserActivityLog> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }

            if (username != null && !username.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("usernameSnapshot")), "%" + username.trim().toLowerCase() + "%"));
            }

            if (action != null && !action.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return logRepository.findAll(spec, pageable);
    }

    private String getIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
