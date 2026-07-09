package com.eventrouting.routing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventrouting.routing.entity.NotificationTemplate;
import com.eventrouting.routing.enums.NotificationChannel;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByUpstreamIdAndTemplateKeyAndChannelAndAttribute1(
            String upstreamId, String templateKey, NotificationChannel channel, String attribute1);

    Optional<NotificationTemplate> findByUpstreamIdAndTemplateKeyAndChannelAndAttribute1IsNull(
            String upstreamId, String templateKey, NotificationChannel channel);
}
