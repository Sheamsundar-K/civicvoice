-- Fix polls table schema to match JPA entity mapping

ALTER TABLE polls RENAME COLUMN title TO question;
ALTER TABLE polls RENAME COLUMN ends_at TO expires_at;
ALTER TABLE polls RENAME COLUMN created_by TO created_by_id;

-- Add is_closed and drop status
ALTER TABLE polls ADD COLUMN is_closed BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE polls SET is_closed = TRUE WHERE status = 'CLOSED';
ALTER TABLE polls DROP COLUMN status;

-- Drop constraints that the JPA entity doesn't populate
ALTER TABLE polls ALTER COLUMN city DROP NOT NULL;
