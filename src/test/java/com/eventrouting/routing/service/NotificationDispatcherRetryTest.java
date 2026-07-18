package com.eventrouting.routing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import com.eventrouting.routing.constants.TimeZones;

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
@TestPropertySource(properties = {
        "app.notification.dispatcher.enabled=true",
        "app.notification.dispatcher.success-ratio=0.0",
        "app.notification.retry.enabled=true",
        "app.notification.retry.delays=1m,5m,15m"
})
class NotificationDispatcherRetryTest {

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Autowired
    private NotificationRetryScheduler notificationRetryScheduler;

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
    }

    @Test
    void completeNotificationSchedulesFirstRetryOnFailure() {
        LocalDateTime before = LocalDateTime.now(TimeZones.APP);
        NotificationLog processing = saveProcessingNotification("txn-retry-1");

        notificationDispatcher.completeNotification(processing.getId());

        NotificationLog updated = notificationLogRepository.findById(processing.getId()).orElseThrow();
        assertThat(updated.getNotificationStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(updated.getRetryCount()).isEqualTo(1);
        assertThat(updated.getLastAttemptAt()).isNotNull();
        assertThat(updated.getNextRetryAt()).isAfter(before.plus(Duration.ofSeconds(50)));
        assertThat(updated.getNextRetryAt()).isBefore(before.plus(Duration.ofSeconds(70)));
    }

    @Test
    void completeNotificationMarksPermanentlyFailedAfterRetriesExhausted() {
        NotificationLog processing = saveProcessingNotification("txn-retry-exhausted");
        processing.setRetryCount(3);
        notificationLogRepository.save(processing);

        notificationDispatcher.completeNotification(processing.getId());

        NotificationLog updated = notificationLogRepository.findById(processing.getId()).orElseThrow();
        assertThat(updated.getNotificationStatus()).isEqualTo(NotificationStatus.FAILED_PERMANENTLY);
        assertThat(updated.getNextRetryAt()).isNull();
        assertThat(updated.getRetryCount()).isEqualTo(3);
    }

    @Test
    void retrySchedulerRequeuesDueFailedNotificationsAsPending() {
        NotificationLog due = saveProcessingNotification("txn-requeue-due");
        due.setNotificationStatus(NotificationStatus.FAILED);
        due.setRetryCount(1);
        due.setNextRetryAt(LocalDateTime.now(TimeZones.APP).minusSeconds(10));
        notificationLogRepository.save(due);

        NotificationLog notDue = saveProcessingNotification("txn-requeue-future");
        notDue.setNotificationStatus(NotificationStatus.FAILED);
        notDue.setRetryCount(1);
        notDue.setNextRetryAt(LocalDateTime.now(TimeZones.APP).plus(Duration.ofMinutes(5)));
        notificationLogRepository.save(notDue);

        var requeuedIds = notificationRetryScheduler.requeueDueRetries();

        assertThat(requeuedIds).containsExactly(due.getId());
        NotificationLog requeued = notificationLogRepository.findById(due.getId()).orElseThrow();
        assertThat(requeued.getNotificationStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(requeued.getNextRetryAt()).isNull();
        assertThat(requeued.getRetryCount()).isEqualTo(1);

        NotificationLog stillWaiting = notificationLogRepository.findById(notDue.getId()).orElseThrow();
        assertThat(stillWaiting.getNotificationStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(stillWaiting.getNextRetryAt()).isNotNull();
    }

    private Long seedTemplateVersion() {
        NotificationTemplate template = new NotificationTemplate();
        template.setUpstreamId("upstream-retry-test");
        template.setTemplateKey("PAYMENT_STATUS_UPDATED");
        template.setChannel(NotificationChannel.SMS);
        template.setAttribute1("SUCCESS");
        template = notificationTemplateRepository.save(template);

        NotificationTemplateVersion version = new NotificationTemplateVersion();
        version.setTemplate(template);
        version.setVersion(1);
        version.setBody("Payment of {{amount}} {{currency}} for {{transactionId}}");
        return notificationTemplateVersionRepository.save(version).getId();
    }

    private NotificationLog saveProcessingNotification(String transactionId) {
        NotificationLog notification = new NotificationLog();
        notification.setEventId("evt-" + transactionId);
        notification.setTransactionId(transactionId);
        notification.setNotificationStatus(NotificationStatus.PROCESSING);
        notification.setTemplateVersionId(testTemplateVersionId);
        notification.setMetadata(Map.of(
                "amount", "100.00",
                "currency", "INR",
                "transactionId", transactionId,
                "processingStartedAt", Instant.now().minusSeconds(1).toString()));
        return notificationLogRepository.save(notification);
    }
}
