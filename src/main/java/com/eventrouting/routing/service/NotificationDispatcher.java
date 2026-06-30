package com.eventrouting.routing.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventrouting.routing.config.NotificationDispatcherProperties;
import com.eventrouting.routing.entity.NotificationLog;
import com.eventrouting.routing.enums.NotificationStatus;
import com.eventrouting.routing.repository.NotificationLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationDispatcherProperties properties;
    private final TaskScheduler taskScheduler;

    @Scheduled(fixedDelayString = "${app.notification.dispatcher.poll-interval-ms:5000}")
    public void pollPendingNotifications() {
        if (!properties.isEnabled()) {
            return;
        }

        List<Long> claimedIds = claimPendingNotifications();
        for (Long notificationId : claimedIds) {
            scheduleCompletion(notificationId);
        }
    }

    @Transactional
    public List<Long> claimPendingNotifications() {
        List<NotificationLog> pending = notificationLogRepository.findByNotificationStatusOrderByCreatedAtAsc(
                NotificationStatus.PENDING,
                PageRequest.of(0, properties.getBatchSize()));

        if (pending.isEmpty()) {
            return List.of();
        }

        for (NotificationLog notification : pending) {
            notification.setNotificationStatus(NotificationStatus.PROCESSING);
        }
        notificationLogRepository.saveAll(pending);

        List<Long> claimedIds = pending.stream().map(NotificationLog::getId).toList();
        log.info("Claimed {} notification(s) for processing: {}", claimedIds.size(), claimedIds);
        return claimedIds;
    }

    void scheduleCompletion(Long notificationId) {
        long delayMs = randomProcessingDelayMs();
        taskScheduler.schedule(
                () -> completeNotification(notificationId),
                Instant.now().plusMillis(delayMs));
        log.debug("Scheduled completion for notification id={} in {} ms", notificationId, delayMs);
    }

    @Transactional
    public void completeNotification(Long notificationId) {
        NotificationLog notification = notificationLogRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            log.warn("Notification id={} not found during completion", notificationId);
            return;
        }
        if (notification.getNotificationStatus() != NotificationStatus.PROCESSING) {
            log.warn(
                    "Skipping completion for notification id={} with status={}",
                    notificationId,
                    notification.getNotificationStatus());
            return;
        }

        boolean success = ThreadLocalRandom.current().nextDouble() < properties.getSuccessRatio();
        NotificationStatus finalStatus = success ? NotificationStatus.SENT : NotificationStatus.FAILED;
        notification.setNotificationStatus(finalStatus);
        notificationLogRepository.save(notification);

        log.info(
                "Notification id={} channel={} transactionId={} marked as {}",
                notificationId,
                notification.getNotificationChannel(),
                notification.getTransactionId(),
                finalStatus);
    }

    private long randomProcessingDelayMs() {
        long min = properties.getProcessingDelayMinMs();
        long max = properties.getProcessingDelayMaxMs();
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }
}
