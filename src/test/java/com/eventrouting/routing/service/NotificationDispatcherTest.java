package com.eventrouting.routing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.eventrouting.routing.entity.NotificationLog;
import com.eventrouting.routing.enums.NotificationChannel;
import com.eventrouting.routing.enums.NotificationStatus;
import com.eventrouting.routing.enums.PaymentStatus;
import com.eventrouting.routing.repository.NotificationLogRepository;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = { "events" })
@TestPropertySource(properties = "app.notification.dispatcher.enabled=true")
class NotificationDispatcherTest {

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @MockitoBean
    private TaskScheduler taskScheduler;

    @BeforeEach
    void setUp() {
        notificationLogRepository.deleteAll();
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
    }

    @Test
    void completeNotificationMarksProcessingRowAsSentOrFailed() {
        NotificationLog processing = saveProcessingNotification("txn-complete-1");

        notificationDispatcher.completeNotification(processing.getId());

        NotificationStatus finalStatus =
                notificationLogRepository.findById(processing.getId()).orElseThrow().getNotificationStatus();
        assertThat(finalStatus).isIn(NotificationStatus.SENT, NotificationStatus.FAILED);
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

    private NotificationLog savePendingNotification(String transactionId) {
        return notificationLogRepository.save(buildNotification(transactionId, NotificationStatus.PENDING));
    }

    private NotificationLog saveProcessingNotification(String transactionId) {
        return notificationLogRepository.save(buildNotification(transactionId, NotificationStatus.PROCESSING));
    }

    private NotificationLog buildNotification(String transactionId, NotificationStatus status) {
        NotificationLog notification = new NotificationLog();
        notification.setEventId("evt-" + transactionId);
        notification.setTransactionId(transactionId);
        notification.setPaymentStatus(PaymentStatus.SUCCESS);
        notification.setNotificationChannel(NotificationChannel.SMS);
        notification.setNotificationStatus(status);
        notification.setMessage("test message");
        return notification;
    }
}
