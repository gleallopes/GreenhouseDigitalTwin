ALTER TABLE measurement
    ADD COLUMN measurement_id VARCHAR(100);

UPDATE measurement
SET measurement_id = 'MEAS-LEGACY-' || id
WHERE measurement_id IS NULL;

ALTER TABLE measurement
    ALTER COLUMN measurement_id SET NOT NULL;

ALTER TABLE measurement
    ADD CONSTRAINT uq_measurement_domain_id
        UNIQUE (measurement_id);