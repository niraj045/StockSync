-- Permanent client access baseline. Passwords are stored as BCrypt hashes only.
INSERT INTO users (
    full_name, username, email, password_hash, active,
    created_by, updated_by, version
)
SELECT seed.full_name, seed.username, seed.email, seed.password_hash, TRUE,
       'deployment-seed', 'deployment-seed', 0
FROM (
    SELECT 'Rohit Kulkarni' AS full_name, 'rohit.admin' AS username,
           'rohit.kulkarni@stocksync.local' AS email,
           '$2b$12$7YJDiDy7P4.Tzm2J8h0uMeiAnhkvpljg2QnArFWe2t5/F/ufdjpP.' AS password_hash
    UNION ALL
    SELECT 'Karan Malhotra', 'karan.admin', 'karan.malhotra@stocksync.local',
           '$2b$12$I1DYdmKHVJ5ZUKu0cjE06eGcE3CZlThxl9vSOBtbbyY/CkWoQfgli'
    UNION ALL
    SELECT 'Vikram Rao', 'vikram.admin', 'vikram.rao@stocksync.local',
           '$2b$12$DUpyxrRVAsunfxK9l5.77eQlWJc.6EqJLPmgYR2ac3CCAoCezofhy'
    UNION ALL
    SELECT 'Aman Verma', 'aman.operations', 'aman.verma@stocksync.local',
           '$2b$12$c2dA2iVtG4bX0khPV6yqI.aOyTcLHz76G.DkkHfczrMRa5J7/3KAq'
    UNION ALL
    SELECT 'Suresh Patil', 'suresh.store', 'suresh.patil@stocksync.local',
           '$2b$12$sWCCyT/iLxSbb16KBkggle4fXOVwpMjRNpCF0tmvcLDllid7DqhdW'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM users existing WHERE LOWER(existing.username) = LOWER(seed.username)
);

UPDATE users user
JOIN (
    SELECT 'Rohit Kulkarni' AS full_name, 'rohit.admin' AS username,
           'rohit.kulkarni@stocksync.local' AS email,
           '$2b$12$7YJDiDy7P4.Tzm2J8h0uMeiAnhkvpljg2QnArFWe2t5/F/ufdjpP.' AS password_hash
    UNION ALL
    SELECT 'Karan Malhotra', 'karan.admin', 'karan.malhotra@stocksync.local',
           '$2b$12$I1DYdmKHVJ5ZUKu0cjE06eGcE3CZlThxl9vSOBtbbyY/CkWoQfgli'
    UNION ALL
    SELECT 'Vikram Rao', 'vikram.admin', 'vikram.rao@stocksync.local',
           '$2b$12$DUpyxrRVAsunfxK9l5.77eQlWJc.6EqJLPmgYR2ac3CCAoCezofhy'
    UNION ALL
    SELECT 'Aman Verma', 'aman.operations', 'aman.verma@stocksync.local',
           '$2b$12$c2dA2iVtG4bX0khPV6yqI.aOyTcLHz76G.DkkHfczrMRa5J7/3KAq'
    UNION ALL
    SELECT 'Suresh Patil', 'suresh.store', 'suresh.patil@stocksync.local',
           '$2b$12$sWCCyT/iLxSbb16KBkggle4fXOVwpMjRNpCF0tmvcLDllid7DqhdW'
) seed ON LOWER(user.username) = LOWER(seed.username)
SET user.full_name = seed.full_name,
    user.email = seed.email,
    user.password_hash = seed.password_hash,
    user.active = TRUE,
    user.updated_at = CURRENT_TIMESTAMP,
    user.updated_by = 'deployment-seed';

DELETE assignment
FROM user_roles assignment
JOIN users user ON user.id = assignment.user_id
WHERE LOWER(user.username) IN (
    'rohit.admin', 'karan.admin', 'vikram.admin', 'aman.operations', 'suresh.store'
);

INSERT INTO user_roles (user_id, role_id)
SELECT user.id, 'ROLE_ADMIN'
FROM users user
WHERE LOWER(user.username) IN (
    'rohit.admin', 'karan.admin', 'vikram.admin', 'aman.operations', 'suresh.store'
);

INSERT INTO quotation_templates (
    template_code, name, description, company_name, company_address,
    header_text, footer_text, default_terms, default_notes, active,
    created_at, created_by, updated_at, updated_by, version
)
VALUES (
    'STEELFAB_EXACT_HIRE_V1',
    'SteelFab Exact Hire Quotation & Agreement',
    'Exact five-page client quotation and hire agreement PDF',
    'Steel-Fab Scaffoldings & Engineering Private Limited',
    'Mumbai, Maharashtra',
    NULL, NULL, NULL, NULL, TRUE,
    CURRENT_TIMESTAMP, 'deployment-seed', CURRENT_TIMESTAMP, 'deployment-seed', 0
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    company_name = VALUES(company_name),
    company_address = VALUES(company_address),
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'deployment-seed';
