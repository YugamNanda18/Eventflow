package com.eventflow.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventSchemaRepository extends JpaRepository<EventSchema, String> {
    Optional<EventSchema> findByEventTypeIdAndVersion(String eventTypeId, String version);
    List<EventSchema> findByEventTypeId(String eventTypeId);
}
