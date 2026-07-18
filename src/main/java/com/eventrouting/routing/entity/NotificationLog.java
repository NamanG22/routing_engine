package com.eventrouting.routing.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import com.eventrouting.routing.enums.NotificationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 20)
    private NotificationStatus notificationStatus = NotificationStatus.PENDING;

    @Column(name = "template_version_id", nullable = false)
    private Long templateVersionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, String> metadata;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    /** Wall-clock time in Asia/Kolkata — same pattern as payment_events.event_timestamp. */
    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    /** Wall-clock time in Asia/Kolkata — same pattern as payment_events.event_timestamp. */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /** DB-managed — same pattern as payment_events.created_at. */
    @Generated(event = EventType.INSERT)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** DB-managed via ON UPDATE CURRENT_TIMESTAMP. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;
}
