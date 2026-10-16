CREATE TABLE twin_state
(
    sensor_id        VARCHAR(50) PRIMARY KEY,

    measurement_type VARCHAR(30)      NOT NULL,

    value            DOUBLE PRECISION NOT NULL,

    unit             VARCHAR(20)      NOT NULL,

    measured_at      TIMESTAMPTZ      NOT NULL,

    received_at      TIMESTAMPTZ      NOT NULL,

    CONSTRAINT fk_twin_state_sensor
        FOREIGN KEY (sensor_id)
            REFERENCES sensor (id),

    CONSTRAINT ck_twin_state_finite_value
        CHECK (
            value != 'Infinity'::float8
                AND value != '-Infinity'::float8
                AND value != 'NaN'::float8
            )
);