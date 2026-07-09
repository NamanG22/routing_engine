package com.eventrouting.routing.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventrouting.routing.entity.NotificationLog;
import com.eventrouting.routing.enums.NotificationStatus;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    @Query("""
            SELECT COUNT(nl) > 0
            FROM NotificationLog nl
            JOIN NotificationTemplateVersion ntv ON nl.templateVersionId = ntv.id
            WHERE nl.transactionId = :transactionId
              AND ntv.template.id = :templateId
            """)
    boolean existsByTransactionIdAndTemplateId(
            @Param("transactionId") String transactionId, @Param("templateId") Long templateId);

    List<NotificationLog> findByNotificationStatusOrderByCreatedAtAsc(
            NotificationStatus notificationStatus, Pageable pageable);
}
