package com.eventrouting.routing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_rule_conditions")
@Getter
@Setter
@NoArgsConstructor
public class NotificationRuleCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_rule_id", nullable = false)
    private NotificationRule notificationRule;

    @Column(name = "field", nullable = false, length = 100)
    private String field;

    @Column(name = "operator", nullable = false, length = 50)
    private String operator;

    @Column(name = "value", nullable = false, length = 255)
    private String value;
}
