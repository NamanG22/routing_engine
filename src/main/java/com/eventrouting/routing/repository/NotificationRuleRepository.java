package com.eventrouting.routing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventrouting.routing.entity.NotificationRule;
import com.eventrouting.routing.enums.EventType;
import com.eventrouting.routing.enums.PaymentMode;
import com.eventrouting.routing.enums.PaymentStatus;

public interface NotificationRuleRepository extends JpaRepository<NotificationRule, Long> {

    List<NotificationRule> findByEventAndModeAndStatus(
            EventType event, PaymentMode mode, PaymentStatus status);
}
