package com.stocksync.reporting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.reporting.dto.ReportDtos.ReportFilterRequest;
import com.stocksync.reporting.dto.ReportDtos.SavedFilterRequest;
import com.stocksync.reporting.dto.ReportDtos.SavedFilterResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedReportFilterService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ReportCatalogService catalog;

    public SavedReportFilterService(JdbcTemplate jdbc, ObjectMapper mapper, ReportCatalogService catalog) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public List<SavedFilterResponse> list(Authentication auth) {
        return jdbc.query("""
                SELECT * FROM saved_report_filters
                WHERE shared_flag = TRUE OR owner_username = ?
                ORDER BY updated_at DESC
                """, (rs, rowNum) -> map(rs), username(auth));
    }

    @Transactional
    public SavedFilterResponse create(SavedFilterRequest request, Authentication auth) {
        String type = catalog.normalize(request.reportType());
        catalog.require(type, auth);
        if (request.shared() && !has(auth, "ROLE_ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Only admins can create shared report filters");
        }
        jdbc.update("""
                INSERT INTO saved_report_filters(name, report_type, filter_json, owner_user_id, owner_username, shared_flag)
                VALUES (?, ?, ?, NULL, ?, ?)
                """, request.name().trim(), type, json(request.filters()), username(auth), request.shared());
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return get(id, auth);
    }

    @Transactional
    public SavedFilterResponse update(Long id, SavedFilterRequest request, Authentication auth) {
        SavedFilterResponse existing = get(id, auth);
        if (existing.shared() && !has(auth, "ROLE_ADMIN") || (!existing.shared() && !existing.ownerUsername().equals(username(auth)))) {
            throw new org.springframework.security.access.AccessDeniedException("Saved filter not permitted");
        }
        String type = catalog.normalize(request.reportType());
        catalog.require(type, auth);
        jdbc.update("""
                UPDATE saved_report_filters SET name=?, report_type=?, filter_json=?, shared_flag=? WHERE id=?
                """, request.name().trim(), type, json(request.filters()), request.shared() && has(auth, "ROLE_ADMIN"), id);
        return get(id, auth);
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        SavedFilterResponse existing = get(id, auth);
        if (existing.shared() && !has(auth, "ROLE_ADMIN") || (!existing.shared() && !existing.ownerUsername().equals(username(auth)))) {
            throw new org.springframework.security.access.AccessDeniedException("Saved filter not permitted");
        }
        jdbc.update("DELETE FROM saved_report_filters WHERE id=?", id);
    }

    private SavedFilterResponse get(Long id, Authentication auth) {
        return jdbc.query("SELECT * FROM saved_report_filters WHERE id=?", rs -> {
            if (!rs.next()) throw new BusinessRuleException("SAVED_REPORT_FILTER_NOT_FOUND", "Saved report filter not found");
            SavedFilterResponse response = map(rs);
            if (!response.shared() && !response.ownerUsername().equals(username(auth)) && !has(auth, "ROLE_ADMIN")) {
                throw new org.springframework.security.access.AccessDeniedException("Saved filter not permitted");
            }
            return response;
        }, id);
    }

    private SavedFilterResponse map(ResultSet rs) throws SQLException {
        return new SavedFilterResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("report_type"),
                filters(rs.getString("filter_json")),
                rs.getString("owner_username"),
                rs.getBoolean("shared_flag"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private ReportFilterRequest filters(String json) {
        try { return mapper.readValue(json, ReportFilterRequest.class); }
        catch (Exception e) { return new ReportFilterRequest(null, null, null, null, null, null, null, null, null, null, null, 0, 25); }
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception e) { return "{}"; }
    }

    private boolean has(Authentication auth, String role) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    private String username(Authentication auth) {
        return auth == null ? "anonymous" : auth.getName();
    }
}
