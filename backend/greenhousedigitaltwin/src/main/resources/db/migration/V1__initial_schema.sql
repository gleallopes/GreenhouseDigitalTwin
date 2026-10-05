CREATE TABLE greenhouse
(
    id   VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE device
(
    id            VARCHAR(50) PRIMARY KEY,
    greenhouse_id VARCHAR(50)  NOT NULL,
    name          VARCHAR(100) NOT NULL,

    CONSTRAINT fk_device_greenhouse
        FOREIGN KEY (greenhouse_id)
            REFERENCES greenhouse (id)
);

CREATE TABLE sensor
(
    id               VARCHAR(50) PRIMARY KEY,
    device_id        VARCHAR(50) NOT NULL,
    measurement_type VARCHAR(30) NOT NULL,
    unit             VARCHAR(20) NOT NULL,

    CONSTRAINT fk_sensor_device
        FOREIGN KEY (device_id)
            REFERENCES device (id)
);

CREATE TABLE measurement
(
    id               BIGSERIAL PRIMARY KEY,

    message_id       VARCHAR(100)     NOT NULL,
    sensor_id        VARCHAR(50)      NOT NULL,
    measurement_type VARCHAR(30)      NOT NULL,
    value            DOUBLE PRECISION NOT NULL,
    unit             VARCHAR(20)      NOT NULL,
    measured_at      TIMESTAMPTZ      NOT NULL,
    received_at      TIMESTAMPTZ      NOT NULL,

    CONSTRAINT uq_measurement_message_id
        UNIQUE (message_id),

    CONSTRAINT fk_measurement_sensor
        FOREIGN KEY (sensor_id)
            REFERENCES sensor (id)
);

CREATE INDEX idx_measurement_sensor_measured_at
    ON measurement (sensor_id, measured_at);