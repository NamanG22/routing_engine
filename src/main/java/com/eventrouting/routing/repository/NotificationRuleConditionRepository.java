package com.eventrouting.routing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventrouting.routing.entity.NotificationRuleCondition;

public interface NotificationRuleConditionRepository
        extends JpaRepository<NotificationRuleCondition, Long> {

    List<NotificationRuleCondition> findByNotificationRuleId(Long notificationRuleId);
}
