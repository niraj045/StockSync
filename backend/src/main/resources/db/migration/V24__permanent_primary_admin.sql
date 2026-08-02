-- Ensure a fresh production database has the same primary administrator as
-- upgraded and local client-baseline databases.
INSERT INTO users (
    full_name, username, email, password_hash, active,
    created_by, updated_by, version
)
SELECT
    'SteelFab Administrator',
    'admin',
    'admin@stocksync.local',
    '$2b$12$k4UwvkcBKvJZ1ct9ffOoXuXRMEr5r1gDg3BXdabYxf/rAvP.cvKXe',
    TRUE,
    'deployment-seed',
    'deployment-seed',
    0
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE LOWER(username) = 'admin'
);

UPDATE users
SET full_name = 'SteelFab Administrator',
    email = 'admin@stocksync.local',
    password_hash = '$2b$12$k4UwvkcBKvJZ1ct9ffOoXuXRMEr5r1gDg3BXdabYxf/rAvP.cvKXe',
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'deployment-seed'
WHERE LOWER(username) = 'admin';

DELETE assignment
FROM user_roles assignment
JOIN users user ON user.id = assignment.user_id
WHERE LOWER(user.username) = 'admin';

INSERT INTO user_roles (user_id, role_id)
SELECT id, 'ROLE_ADMIN'
FROM users
WHERE LOWER(username) = 'admin';
