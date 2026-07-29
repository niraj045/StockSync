-- Idempotent client-handover accounts for both existing and fresh deployments.
-- Passwords are BCrypt hashes; no plaintext credentials are stored in source or the database.
INSERT INTO users (
    full_name, username, email, password_hash, active,
    created_by, updated_by, version
)
SELECT seed.full_name, seed.username, seed.email, seed.password_hash, TRUE,
       'deployment-seed', 'deployment-seed', 0
FROM (
    SELECT 'Rohit Kulkarni' AS full_name, 'rohit.admin' AS username,
           'rohit.kulkarni@stocksync-demo.local' AS email,
           '$2y$12$pyZms79tnYCrasIV2Oj/N.LomRStJWiCEe7fihyyDeZKAzliv68ze' AS password_hash
    UNION ALL
    SELECT 'Karan Malhotra', 'karan.admin',
           'karan.malhotra@stocksync-demo.local',
           '$2y$12$8EcmFmf0Sni2UmiW9iQKKeqbHS65EHzWhxHLvw4LlzZ.ehIGyHZcq'
    UNION ALL
    SELECT 'Vikram Rao', 'vikram.admin',
           'vikram.admin@stocksync-demo.local',
           '$2y$12$A4YesC9dW6Hzj6GOXg5Df.1NlN8ar23qipd0wk02sk8z.bPfNtsjq'
    UNION ALL
    SELECT 'Aman Verma', 'aman.operations',
           'aman.verma@stocksync-demo.local',
           '$2y$12$dMHk1S3lcQITTXDCWgFUK.J0ZkjxyJ/GuY2P1D2P8YOQ3gzwHCAIi'
    UNION ALL
    SELECT 'Suresh Patil', 'suresh.store',
           'suresh.patil@stocksync-demo.local',
           '$2y$12$LyqAFFg2Mo75DkVBAfM1le91JPqM3jZv.55sXCXqHQYM/.SRXWO4W'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM users existing
    WHERE LOWER(existing.username) = LOWER(seed.username)
       OR LOWER(existing.email) = LOWER(seed.email)
);

INSERT INTO user_roles (user_id, role_id)
SELECT user.id, assignment.role_id
FROM (
    SELECT 'rohit.admin' AS username, 'ROLE_ADMIN' AS role_id
    UNION ALL SELECT 'karan.admin', 'ROLE_ADMIN'
    UNION ALL SELECT 'vikram.admin', 'ROLE_ADMIN'
    UNION ALL SELECT 'aman.operations', 'ROLE_OPERATIONS'
    UNION ALL SELECT 'suresh.store', 'ROLE_OPERATIONS'
) assignment
JOIN users user ON LOWER(user.username) = LOWER(assignment.username)
WHERE NOT EXISTS (
    SELECT 1
    FROM user_roles existing
    WHERE existing.user_id = user.id
      AND existing.role_id = assignment.role_id
);
