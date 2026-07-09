package com.eventrouting.routing.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.eventrouting.routing.enums.NotificationChannel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "notification_templates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_templates_lookup",
                columnNames = {"upstream_id", "template_key", "channel", "attribute1"}))
@Getter
@Setter
@NoArgsConstructor
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upstream_id", nullable = false, length = 100)
    private String upstreamId;

    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "attribute1", length = 50)
    private String attribute1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
