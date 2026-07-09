package com.eventrouting.routing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.eventrouting.routing.entity.NotificationLog;
import com.eventrouting.routing.entity.NotificationTemplate;
import com.eventrouting.routing.entity.NotificationTemplateVersion;
import com.eventrouting.routing.enums.NotificationChannel;
import com.eventrouting.routing.enums.NotificationStatus;
import com.eventrouting.routing.repository.NotificationLogRepository;
import com.eventrouting.routing.repository.NotificationTemplateRepository;
import com.eventrouting.routing.repository.NotificationTemplateVersionRepository;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = { "events" })
@TestPropertySource(properties = "app.notification.dispatcher.enabled=true")
class NotificationDispatcherTest {

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private NotificationTemplateVersionRepository notificationTemplateVersionRepository;

    @MockitoBean
    private TaskScheduler taskScheduler;

    private Long testTemplateVersionId;

    @BeforeEach
    void setUp() {
        notificationLogRepository.deleteAll();
        notificationTemplateVersionRepository.deleteAll();
        notificationTemplateRepository.deleteAll();
        testTemplateVersionId = seedTemplateVersion();

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void claimPendingNotificationsMarksRowsAsProcessing() {
        NotificationLog pending = savePendingNotification("txn-claim-1");

        var claimedIds = notificationDispatcher.claimPendingNotifications();

        assertThat(claimedIds).containsExactly(pending.getId());
        NotificationLog updated = notificationLogRepository.findById(pending.getId()).orElseThrow();
        assertThat(updated.getNotificationStatus()).isEqualTo(NotificationStatus.PROCESSING);
        assertThat(updated.getMetadata()).containsKey("processingStartedAt");
    }

    @Test
    void completeNotificationMarksProcessingRowAsSentOrFailed() {
        NotificationLog processing = saveProcessingNotification("txn-complete-1");

        notificationDispatcher.completeNotification(processing.getId());

        NotificationLog updated = notificationLogRepository.findById(processing.getId()).orElseThrow();
        assertThat(updated.getNotificationStatus()).isIn(NotificationStatus.SENT, NotificationStatus.FAILED);
        assertThat(updated.getMetadata()).containsKey("dispatchDurationMs");
    }

    @Test
    void pollPendingNotificationsClaimsAndSchedulesCompletion() {
        NotificationLog pending = savePendingNotification("txn-poll-1");

        notificationDispatcher.pollPendingNotifications();

        assertThat(notificationLogRepository.findById(pending.getId()).orElseThrow().getNotificationStatus())
                .isIn(NotificationStatus.SENT, NotificationStatus.FAILED);
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void successRatioSkewsTowardSent() {
        for (int i = 0; i < 100; i++) {
            NotificationLog processing = saveProcessingNotification("txn-ratio-" + i);
            notificationDispatcher.completeNotification(processing.getId());
        }

        long sentCount = notificationLogRepository.findAll().stream()
                .filter(notification -> notification.getNotificationStatus() == NotificationStatus.SENT)
                .count();

        assertThat(sentCount).isGreaterThan(50);
    }

    @Test
    void completeNotificationFailsWhenTemplateVersionMissing() {
        NotificationLog processing = saveProcessingNotification("txn-missing-template");
        processing.setTemplateVersionId(99999L);
        notificationLogRepository.save(processing);

        notificationDispatcher.completeNotification(processing.getId());

        NotificationLog updated = notificationLogRepository.findById(processing.getId()).orElseThrow();
        assertThat(updated.getNotificationStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(updated.getMetadata()).containsEntry("failureReason", "TEMPLATE_VERSION_NOT_FOUND");
    }

    private Long seedTemplateVersion() {
        NotificationTemplate template = new NotificationTemplate();
        template.setUpstreamId("upstream-test");
        template.setTemplateKey("PAYMENT_STATUS_UPDATED");
        template.setChannel(NotificationChannel.SMS);
        template.setAttribute1("SUCCESS");
        template = notificationTemplateRepository.save(template);

        NotificationTemplateVersion version = new NotificationTemplateVersion();
        version.setTemplate(template);
        version.setVersion(1);
        version.setBody("Payment of {{amount}} {{currency}} is {{paymentStatus}} for {{transactionId}}");
        return notificationTemplateVersionRepository.save(version).getId();
    }

    private NotificationLog savePendingNotification(String transactionId) {
        return notificationLogRepository.save(buildNotification(transactionId, NotificationStatus.PENDING));
    }

    private NotificationLog saveProcessingNotification(String transactionId) {
        NotificationLog notification = buildNotification(transactionId, NotificationStatus.PROCESSING);
        notification.setMetadata(Map.of("processingStartedAt", Instant.now().minusSeconds(1).toString()));
        return notificationLogRepository.save(notification);
    }

    private NotificationLog buildNotification(String transactionId, NotificationStatus status) {
        NotificationLog notification = new NotificationLog();
        notification.setEventId("evt-" + transactionId);
        notification.setTransactionId(transactionId);
        notification.setNotificationStatus(status);
        notification.setTemplateVersionId(testTemplateVersionId);
        notification.setMetadata(Map.of(
                "amount", "100.00",
                "currency", "INR",
                "paymentStatus", "SUCCESS",
                "transactionId", transactionId));
        return notification;
    }
}
