package com.greenhousedigitaltwin.telemetry.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMeasurementRepository extends JpaRepository<MeasurementEntity, Long> {
    boolean existsByMessageId(String messageId);
}
