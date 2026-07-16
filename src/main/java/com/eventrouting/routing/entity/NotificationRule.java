package com.eventrouting.routing.entity;

import com.eventrouting.routing.enums.EventType;
import com.eventrouting.routing.enums.NotificationChannel;
import com.eventrouting.routing.enums.PaymentMode;
import com.eventrouting.routing.enums.PaymentStatus;

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
@Table(name = "notification_rules")
@Getter
@Setter
@NoArgsConstructor
public class NotificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upstream_id", nullable = false, length = 100)
    private String upstreamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, length = 50)
    private EventType event;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 50)
    private PaymentMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;
}
