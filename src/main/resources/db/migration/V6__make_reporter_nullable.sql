-- V6: Make reporter optional to support anonymous unauthenticated reporting
ALTER TABLE issues ALTER COLUMN reporter_id DROP NOT NULL;
