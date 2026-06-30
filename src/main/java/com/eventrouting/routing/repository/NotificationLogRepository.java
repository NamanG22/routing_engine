package com.eventrouting.routing.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eventrouting.routing.entity.NotificationLog;
import com.eventrouting.routing.enums.NotificationChannel;
import com.eventrouting.routing.enums.NotificationStatus;
import com.eventrouting.routing.enums.PaymentStatus;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsByTransactionIdAndPaymentStatusAndNotificationChannel(
            String transactionId,
            PaymentStatus paymentStatus,
            NotificationChannel notificationChannel);

    List<NotificationLog> findByNotificationStatusOrderByCreatedAtAsc(
            NotificationStatus notificationStatus,
            Pageable pageable);
}
