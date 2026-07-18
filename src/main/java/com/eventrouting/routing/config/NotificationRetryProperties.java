package com.eventrouting.routing.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.notification.retry")
public class NotificationRetryProperties {

    private boolean enabled = true;

    /** Delay between poll cycles after the previous run completes. */
    private long pollIntervalMs = 10_000;

    private int batchSize = 10;

    /**
     * Backoff delays between retries after each failure.
     * Length of this list is the maximum number of retries.
     */
    private List<Duration> delays = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15));
}
