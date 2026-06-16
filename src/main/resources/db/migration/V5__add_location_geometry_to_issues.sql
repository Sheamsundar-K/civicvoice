-- V5: No-op migration.
-- The Issue entity was refactored to use plain latitude/longitude DOUBLE PRECISION columns
-- (already present from V1) instead of a PostGIS GEOMETRY column.
-- PostGIS is not installed on this PostgreSQL instance.
SELECT 1;
