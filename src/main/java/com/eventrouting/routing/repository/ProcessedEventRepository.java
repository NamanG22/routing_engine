package com.eventrouting.routing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventrouting.routing.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

	boolean existsByEventId(String eventId);
}
