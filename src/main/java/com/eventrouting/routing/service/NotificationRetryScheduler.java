package com.eventrouting.routing.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventrouting.routing.config.NotificationRetryProperties;
import com.eventrouting.routing.constants.TimeZones;
import com.eventrouting.routing.entity.NotificationLog;
import com.eventrouting.routing.enums.NotificationStatus;
import com.eventrouting.routing.repository.NotificationLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryScheduler.class);

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationRetryProperties properties;

    @Scheduled(fixedDelayString = "${app.notification.retry.poll-interval-ms:10000}")
    public void pollDueRetries() {
        if (!properties.isEnabled()) {
            return;
        }

        List<Long> requeuedIds = requeueDueRetries();
        if (!requeuedIds.isEmpty()) {
            log.info("Requeued {} notification(s) for retry: {}", requeuedIds.size(), requeuedIds);
        }
    }

    @Transactional
    public List<Long> requeueDueRetries() {
        List<NotificationLog> due = notificationLogRepository.findDueForRetry(
                NotificationStatus.FAILED,
                LocalDateTime.now(TimeZones.APP),
                PageRequest.of(0, properties.getBatchSize()));

        if (due.isEmpty()) {
            return List.of();
        }

        for (NotificationLog notification : due) {
            notification.setNotificationStatus(NotificationStatus.PENDING);
            notification.setNextRetryAt(null);
        }
        notificationLogRepository.saveAll(due);

        return due.stream().map(NotificationLog::getId).toList();
    }
}
