# 10. Security, Audit, and Backup

## 10.1 Authentication

- Secure login
- Session-based authentication
- HTTP-only cookie
- Secure cookie in HTTPS
- SameSite cookie
- CSRF protection
- Password hashing
- Session expiration
- Account deactivation
- Register session lifecycle events so deactivation and password changes can invalidate tracked sessions.
- Password changes invalidate the user's other active sessions while preserving the current session.

## 10.2 Authorization

Use method-level authorization for critical operations.

Examples:

- Only authorized users can post stock.
- Only accounts users can issue or cancel invoices.
- Only admins can create users.
- View-only users cannot change data.

## 10.3 Audit Logging

Log:

- User
- Time
- Action
- Entity
- Entity ID
- Before and after summary where appropriate
- IP address, if useful
- Request ID
- Reason for cancellation or reversal

Do not allow audit records to be edited through the normal application.

Audit username snapshots remain populated even when the referenced user is unavailable. Audit `user_id` values are indexed but deliberately have no foreign key so immutable history survives user lifecycle changes.

## 10.4 Data Protection

- HTTPS required
- Database not publicly exposed
- Secrets in environment variables
- Uploaded files outside public path
- Minimal server ports
- Regular dependency updates
- Input validation
- SQL injection protection through parameterized JPA queries
- File size and type validation

## 10.5 Backup Scope

Back up:

1. MySQL database
2. Uploaded files
3. Generated reports if regeneration is not guaranteed
4. Deployment configuration
5. Environment configuration stored securely

## 10.6 Backup Schedule

Recommended:

- Daily database backup
- Daily uploaded-file backup
- Retain daily backups for 7 days
- Retain weekly backups for 4 weeks
- Keep one off-server backup
- Test restore monthly

## 10.7 Important Rule

Docker volumes provide persistence, not backup.

A volume may survive container recreation but can still be lost due to disk failure, accidental deletion, or server compromise.

## 10.8 Recovery Objectives

Suggested initial targets:

- Recovery Point Objective: 24 hours
- Recovery Time Objective: 4–8 hours

These should be confirmed with the client.

## 10.9 Data Retention

- Posted challans: retain
- Invoices: retain
- Payments: retain
- Audit logs: retain
- Cancelled records: retain with status
- Temporary processing files: delete automatically
