package com.eventflow.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventTypeRepository extends JpaRepository<EventType, String> {
    Optional<EventType> findByOrganizationIdAndName(String organizationId, String name);
    List<EventType> findByOrganizationId(String organizationId);
}
