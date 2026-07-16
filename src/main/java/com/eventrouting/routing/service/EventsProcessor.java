package com.eventrouting.routing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.eventrouting.routing.condition.RuleConditionEvaluator;
import com.eventrouting.routing.dto.EventReceived;
import com.eventrouting.routing.entity.NotificationRule;
import com.eventrouting.routing.entity.PaymentEvent;
import com.eventrouting.routing.entity.ProcessedEvent;
import com.eventrouting.routing.repository.NotificationRuleRepository;
import com.eventrouting.routing.repository.PaymentEventRepository;
import com.eventrouting.routing.repository.ProcessedEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventsProcessor {

    private static final ZoneId EVENT_TIMESTAMP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter LOCAL_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Logger log = LoggerFactory.getLogger(EventsProcessor.class);

    private final ProcessedEventRepository processedEventRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final NotificationRuleRepository notificationRuleRepository;
    private final RuleConditionEvaluator ruleConditionEvaluator;
    private final NotificationService notificationService;
    
    public void processEvent(EventReceived event) {
        if (!StringUtils.hasText(event.getEventId())) {
            log.error("Skipping event with missing eventId: {}", event);
            return;
        }

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.info("Skipping already processed event: {}", event.getEventId());
            return;
        }

        // boolean processedSuccessfully = switch (event.getEventType()) {
        //     case PAYMENT_STATUS_UPDATED -> processPaymentStatusUpdatedEvent(event);
        //     case NOTIFICATION_DELIVERY_STATUS -> {
        //         processNotificationDeliveryStatusEvent(event);
        //         yield true;
        //     }
        // };

        boolean processedSuccessfully = processEventAndSendNotification(event);

        if (processedSuccessfully) {
            markEventAsProcessed(event);
        }
    }

    private boolean processEventAndSendNotification(EventReceived event) {
        if (!savePaymentEvent(event)) {
            return false;
        }

        List<NotificationRule> rules = notificationRuleRepository.findByEventAndModeAndStatus(
                event.getEventType(), event.getPaymentMode(), event.getPaymentStatus());
        if (rules.isEmpty()) {
            log.info(
                    "No notification rules for eventType={}, mode={}, status={}",
                    event.getEventType(),
                    event.getPaymentMode(),
                    event.getPaymentStatus());
            return true;
        }

        for (NotificationRule rule : rules) {
            if (!ruleConditionEvaluator.matches(event, rule.getId())) {
                log.info(
                        "Skipping notification for ruleId={}, channel={} — conditions not met",
                        rule.getId(),
                        rule.getChannel());
                continue;
            }
            notificationService.createNotification(event, rule.getChannel());
        }
        return true;
    }

    private void markEventAsProcessed(EventReceived event) {
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(event.getEventId());
        processedEventRepository.save(processedEvent);
        log.info("Marked event as processed: {}", event.getEventId());
    }

    // private void processNotificationDeliveryStatusEvent(EventReceived event) {
    //     // later we will implement this
    // }

    // private boolean processPaymentStatusUpdatedEvent(EventReceived event) {
    //     if (!savePaymentEvent(event)) {
    //         return false;
    //     }

    //     switch (event.getPaymentStatus()) {
    //         case SUCCESS:
    //             processPaymentSuccessEvent(event);
    //             break;
    //         case FAILED:
    //             processPaymentFailedEvent(event);
    //             break;
    //         case PENDING:
    //             processPaymentPendingEvent(event);
    //             break;
    //         default:
    //             log.error("Unknown payment status: {}", event.getPaymentStatus());
    //             return false;
    //     }
    //     return true;
    // }

    // private void processPaymentPendingEvent(EventReceived event) {
    //     log.info("Payment pending event received: {}", event);

    //     // make a entry in job scheduler to check the status after 5 minitess if still pending then add a new entry with status EXPIRED

    //     switch (event.getPaymentMode()) {
    //         case UPI_QR:
    //             processPaymentPendingEventUPIQR(event);
    //             break;
    //         case ONLINE_CHECKOUT:
    //             processPaymentPendingEventOnlineCheckout(event);
    //             break;
    //         default:
    //             log.error("Unknown payment mode: {}", event.getPaymentMode());
    //             break;
    //     }

    //     // store every notification send to notification_log table 
    // }

    // private void processPaymentPendingEventUPIQR(EventReceived event) {
    //     // make a entry in job scheduler to check the status after 30 seconds if still pending then trigger the in app notification
    // }
    
    // private void processPaymentPendingEventOnlineCheckout(EventReceived event) {
    //     // make a entry in job scheduler to check the status after 1 minute if still pending then trigger the in app notification
    // }

    // private void processPaymentSuccessEvent(EventReceived event) {
    //     log.info("Payment success event received: {}", event);
    //     switch (event.getPaymentMode()) {
    //         case UPI_QR:
    //             processPaymentSuccessEventUPIQR(event);
    //             break;
    //         case ONLINE_CHECKOUT:
    //             processPaymentSuccessEventOnlineCheckout(event);
    //             break;
    //         default:
    //             log.error("Unknown payment mode: {}", event.getPaymentMode());
    //             break;
    //     }
    // }

    // private void processPaymentSuccessEventOnlineCheckout(EventReceived event) {
    //     notificationService.createSMSNotification(event);
    //     notificationService.createEmailNotification(event);
    //     notificationService.createWebsiteNotification(event);
    // }

    // private void processPaymentSuccessEventUPIQR(EventReceived event) {
    //     notificationService.createSMSNotification(event);

    //     BigDecimal amount = new BigDecimal(event.getAmount());
    //     if (amount.compareTo(BigDecimal.valueOf(1000)) >= 0) {
    //         notificationService.createEmailNotification(event);
    //     }
    //     // merchant push notification
    // }

    // private void processPaymentFailedEvent(EventReceived event) {
    //     log.info("Payment failed event received: {}", event);

    //     switch (event.getPaymentMode()) {
    //         case UPI_QR:
    //             processPaymentFailedEventUPIQR(event);
    //             break;
    //         case ONLINE_CHECKOUT:
    //             processPaymentFailedEventOnlineCheckout(event);
    //             break;
    //         default:
    //             log.error("Unknown payment mode: {}", event.getPaymentMode());
    //     }
    // }

    // private void processPaymentFailedEventOnlineCheckout(EventReceived event) {
    //     notificationService.createWebsiteNotification(event);
    //     notificationService.createEmailNotification(event);
    // }
    
    // private void processPaymentFailedEventUPIQR(EventReceived event) {
    //     notificationService.createInAppNotification(event);
    // }

    private boolean savePaymentEvent(EventReceived event) {
        if (!validatePaymentEvent(event)) {
            return false;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(event.getAmount());
        } catch (NumberFormatException e) {
            log.error("Skipping payment event with invalid amount: {}", event.getAmount(), e);
            return false;
        }

        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setEventId(event.getEventId());
        paymentEvent.setTransactionId(event.getTransactionId());
        paymentEvent.setEventType(event.getEventType());
        paymentEvent.setPaymentMode(event.getPaymentMode());
        paymentEvent.setPaymentStatus(event.getPaymentStatus());
        paymentEvent.setMerchantId(event.getMerchantId());
        paymentEvent.setCustomerId(event.getCustomerId());
        paymentEvent.setAmount(amount);
        paymentEvent.setCurrency(event.getCurrency().name());
        if (StringUtils.hasText(event.getTimestamp())) {
            Instant eventTimestamp = parseEventTimestamp(event.getTimestamp());
            if (eventTimestamp == null) {
                log.error("Skipping payment event with invalid timestamp: {}", event.getTimestamp());
                return false;
            }
            paymentEvent.setEventTimestamp(
                    eventTimestamp.atZone(EVENT_TIMESTAMP_ZONE).toLocalDateTime());
        }
        paymentEvent.setMetadata(event.getMetadata());

        paymentEventRepository.save(paymentEvent);
        log.info("Saved payment event for eventId={}, transactionId={}, status={}",
                event.getEventId(), event.getTransactionId(), event.getPaymentStatus());
        return true;
    }

    private boolean validatePaymentEvent(EventReceived event) {
        if (!StringUtils.hasText(event.getTransactionId())) {
            log.error("Skipping payment event with missing transactionId: {}", event);
            return false;
        }
        if (event.getPaymentMode() == null) {
            log.error("Skipping payment event with missing paymentMode: {}", event);
            return false;
        }
        if (event.getPaymentStatus() == null) {
            log.error("Skipping payment event with missing paymentStatus: {}", event);
            return false;
        }
        if (!StringUtils.hasText(event.getMerchantId())) {
            log.error("Skipping payment event with missing merchantId: {}", event);
            return false;
        }
        if (!StringUtils.hasText(event.getCustomerId())) {
            log.error("Skipping payment event with missing customerId: {}", event);
            return false;
        }
        if (event.getCurrency() == null) {
            log.error("Skipping payment event with missing currency: {}", event);
            return false;
        }
        if (!StringUtils.hasText(event.getAmount())) {
            log.error("Skipping payment event with missing amount: {}", event);
            return false;
        }
        return true;
    }

    private Instant parseEventTimestamp(String timestamp) {
        String value = timestamp.trim();

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // try next format
        }

        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            // try next format
        }

        try {
            return LocalDateTime.parse(value, LOCAL_TIMESTAMP_FORMAT)
                    .atZone(EVENT_TIMESTAMP_ZONE)
                    .toInstant();
        } catch (DateTimeParseException e) {
            log.error("Unable to parse event timestamp: {}", value, e);
            return null;
        }
    }
}
