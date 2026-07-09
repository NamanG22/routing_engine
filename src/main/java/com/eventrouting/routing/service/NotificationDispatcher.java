package com.eventrouting.routing.service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.eventrouting.routing.entity.NotificationTemplateVersion;
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
    private final TemplateService templateService;

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

        Instant processingStartedAt = Instant.now();
        for (NotificationLog notification : pending) {
            notification.setNotificationStatus(NotificationStatus.PROCESSING);
            Map<String, String> metadata = new HashMap<>(
                    notification.getMetadata() != null ? notification.getMetadata() : Map.of());
            metadata.put("processingStartedAt", processingStartedAt.toString());
            notification.setMetadata(metadata);
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

        Map<String, String> metadata = new HashMap<>(
                notification.getMetadata() != null ? notification.getMetadata() : Map.of());
        appendDispatchDuration(metadata);

        NotificationTemplateVersion templateVersion = templateService
                .findVersionById(notification.getTemplateVersionId())
                .orElse(null);
        if (templateVersion == null) {
            metadata.put("failureReason", "TEMPLATE_VERSION_NOT_FOUND");
            notification.setMetadata(metadata);
            notification.setNotificationStatus(NotificationStatus.FAILED);
            notificationLogRepository.save(notification);
            log.warn(
                    "Notification id={} failed — template version not found: {}",
                    notificationId,
                    notification.getTemplateVersionId());
            return;
        }

        String renderedBody = templateService.render(templateVersion.getBody(), metadata);
        boolean success = ThreadLocalRandom.current().nextDouble() < properties.getSuccessRatio();
        NotificationStatus finalStatus = success ? NotificationStatus.SENT : NotificationStatus.FAILED;
        notification.setNotificationStatus(finalStatus);
        notification.setMetadata(metadata);
        notificationLogRepository.save(notification);

        log.info(
                "Notification id={} transactionId={} templateVersionId={} marked as {} body={}",
                notificationId,
                notification.getTransactionId(),
                notification.getTemplateVersionId(),
                finalStatus,
                renderedBody);
    }

    private void appendDispatchDuration(Map<String, String> metadata) {
        String processingStartedAt = metadata.get("processingStartedAt");
        if (processingStartedAt == null) {
            return;
        }

        try {
            Instant start = Instant.parse(processingStartedAt);
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            metadata.put("dispatchDurationMs", String.valueOf(durationMs));
        } catch (DateTimeParseException e) {
            log.warn("Unable to parse processingStartedAt={}", processingStartedAt, e);
        }
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
