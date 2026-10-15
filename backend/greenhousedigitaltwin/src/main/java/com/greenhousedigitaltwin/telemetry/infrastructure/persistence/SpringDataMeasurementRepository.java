package com.greenhousedigitaltwin.telemetry.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataMeasurementRepository extends JpaRepository<MeasurementEntity, Long> {
    boolean existsByMessageId(String messageId);

    Optional<MeasurementEntity> findByMessageId(String messageId);
}
