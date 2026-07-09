package com.eventrouting.routing.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.eventrouting.routing.constants.NotificationConstants;
import com.eventrouting.routing.dto.EventReceived;
import com.eventrouting.routing.entity.NotificationLog;
import com.eventrouting.routing.entity.NotificationTemplateVersion;
import com.eventrouting.routing.enums.NotificationChannel;
import com.eventrouting.routing.enums.NotificationStatus;
import com.eventrouting.routing.repository.NotificationLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository notificationLogRepository;
    private final TemplateService templateService;

    public void createSMSNotification(EventReceived event) {
        createNotificationLog(event, NotificationChannel.SMS);
    }

    public void createEmailNotification(EventReceived event) {
        createNotificationLog(event, NotificationChannel.EMAIL);
    }

    public void createInAppNotification(EventReceived event) {
        createNotificationLog(event, NotificationChannel.IN_APP);
    }

    public void createWebsiteNotification(EventReceived event) {
        createNotificationLog(event, NotificationChannel.WEB);
    }

    public void createPushNotification(EventReceived event) {
        createNotificationLog(event, NotificationChannel.PUSH);
    }

    private void createNotificationLog(EventReceived event, NotificationChannel channel) {
        if (!validateNotification(event, channel)) {
            return;
        }

        String templateKey = resolveTemplateKey(event);
        if (!StringUtils.hasText(templateKey)) {
            log.error(
                    "Skipping {} notification with missing metadata.templateKey for eventId={}",
                    channel,
                    event.getEventId());
            return;
        }

        String attribute1 = resolveAttribute1(event);

        Optional<NotificationTemplateVersion> templateVersion = templateService.resolveLatestVersion(
                event.getUpstreamId(), templateKey, channel, attribute1);
        if (templateVersion.isEmpty()) {
            log.error(
                    "Skipping {} notification — no template for upstreamId={}, templateKey={}, attribute1={}",
                    channel,
                    event.getUpstreamId(),
                    templateKey,
                    attribute1);
            return;
        }

        Long templateId = templateVersion.get().getTemplate().getId();
        if (notificationLogRepository.existsByTransactionIdAndTemplateId(
                event.getTransactionId(), templateId)) {
            log.info(
                    "Skipping duplicate {} notification for transactionId={}, templateId={}",
                    channel,
                    event.getTransactionId(),
                    templateId);
            return;
        }

        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setEventId(event.getEventId());
        notificationLog.setTransactionId(event.getTransactionId());
        notificationLog.setNotificationStatus(NotificationStatus.PENDING);
        notificationLog.setTemplateVersionId(templateVersion.get().getId());
        notificationLog.setMetadata(buildMetadata(event));

        notificationLogRepository.save(notificationLog);
        log.info(
                "Created {} notification log for eventId={}, transactionId={}, templateVersionId={}",
                channel,
                event.getEventId(),
                event.getTransactionId(),
                templateVersion.get().getId());
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
        if (!StringUtils.hasText(event.getUpstreamId())) {
            log.error("Skipping {} notification with missing upstreamId: {}", channel, event);
            return false;
        }
        return true;
    }

    private String resolveTemplateKey(EventReceived event) {
        if (event.getMetadata() == null) {
            return null;
        }
        return event.getMetadata().get(NotificationConstants.METADATA_TEMPLATE_KEY);
    }

    private String resolveAttribute1(EventReceived event) {
        if (event.getPaymentStatus() != null) {
            return event.getPaymentStatus().name();
        }
        if (event.getMetadata() != null && StringUtils.hasText(event.getMetadata().get("attribute1"))) {
            return event.getMetadata().get("attribute1");
        }
        return null;
    }

    private Map<String, String> buildMetadata(EventReceived event) {
        Map<String, String> metadata = new HashMap<>(3);
        metadata.put("amount", event.getAmount());
        metadata.put("currency", event.getCurrency() != null ? event.getCurrency().name() : null);
        metadata.put("transactionId", event.getTransactionId());
        return metadata;
    }
}
