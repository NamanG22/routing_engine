package com.eventrouting.routing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eventrouting.routing.entity.NotificationTemplateVersion;

public interface NotificationTemplateVersionRepository
        extends JpaRepository<NotificationTemplateVersion, Long> {

    @EntityGraph(attributePaths = "template")
    Optional<NotificationTemplateVersion> findTopByTemplateIdOrderByVersionDesc(Long templateId);
}
