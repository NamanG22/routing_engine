package com.eventrouting.routing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.eventrouting.routing.dto.EventReceived;
import com.eventrouting.routing.entity.NotificationLog;
import com.eventrouting.routing.enums.NotificationChannel;
import com.eventrouting.routing.enums.NotificationStatus;
import com.eventrouting.routing.repository.NotificationLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository notificationLogRepository;

    public void createSMSNotification(EventReceived event) {
        createNotificationLog(event, NotificationChannel.SMS);
    }

    public void createEmailNotification(EventReceived event) {
        createNotificationLog(event, NotificationChannel.EMAIL);
    }

    private void createNotificationLog(EventReceived event, NotificationChannel channel) {
        if (!validateNotification(event, channel)) {
            return;
        }

        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setEventId(event.getEventId());
        notificationLog.setTransactionId(event.getTransactionId());
        notificationLog.setPaymentStatus(event.getPaymentStatus());
        notificationLog.setNotificationChannel(channel);
        notificationLog.setNotificationStatus(NotificationStatus.PENDING);
        notificationLog.setMessage(buildMessage(event, channel));

        notificationLogRepository.save(notificationLog);
        log.info("Created {} notification log for eventId={}, transactionId={}, paymentStatus={}",
                channel, event.getEventId(), event.getTransactionId(), event.getPaymentStatus());
    }

    private boolean validateNotification(EventReceived event, NotificationChannel channel) {
        if (!StringUtils.hasText(event.getEventId())) {
            log.error("Skipping {} notification with missing eventId: {}", channel, event);
            return false;
        }
        if (!StringUtils.hasText(event.getTransactionId())) {
            log.error("Skipping {} notification with missing transactionId: {}", channel, event);
            return false;
        }
        if (event.getPaymentStatus() == null) {
            log.error("Skipping {} notification with missing paymentStatus: {}", channel, event);
            return false;
        }

        if (notificationLogRepository.existsByTransactionIdAndPaymentStatusAndNotificationChannel(
                event.getTransactionId(), event.getPaymentStatus(), channel)) {
            log.info("Skipping duplicate {} notification for transactionId={}, paymentStatus={}",
                    channel, event.getTransactionId(), event.getPaymentStatus());
            return false;
        }

        return true;
    }

    private String buildMessage(EventReceived event, NotificationChannel channel) {
        return String.format(
                "%s notification for transaction %s (payment %s, amount %s %s)",
                channel,
                event.getTransactionId(),
                event.getPaymentStatus(),
                event.getAmount(),
                event.getCurrency());
    }
}
