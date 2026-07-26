ALTER TABLE quotations
    ADD COLUMN party_name_snapshot VARCHAR(150) NULL AFTER party_id,
    ADD COLUMN site_name_snapshot VARCHAR(150) NULL AFTER site_id;

UPDATE quotations q
JOIN parties p ON p.id = q.party_id
JOIN sites s ON s.id = q.site_id
SET q.party_name_snapshot = p.legal_name,
    q.site_name_snapshot = s.site_name;

ALTER TABLE quotations
    MODIFY party_name_snapshot VARCHAR(150) NOT NULL,
    MODIFY site_name_snapshot VARCHAR(150) NOT NULL;
