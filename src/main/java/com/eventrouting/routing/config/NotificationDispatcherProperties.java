package com.eventrouting.routing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.notification.dispatcher")
public class NotificationDispatcherProperties {

    private boolean enabled = true;

    /** Delay between poll cycles after the previous run completes. */
    private long pollIntervalMs = 5_000;

    private int batchSize = 10;

    private long processingDelayMinMs = 10_000;

    private long processingDelayMaxMs = 20_000;

    /** Fraction of notifications that complete as SENT (0.0–1.0). */
    private double successRatio = 0.9;
}
