package com.eventrouting.routing.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
    NotificationDispatcherProperties.class,
    NotificationRetryProperties.class
})
public class NotificationDispatcherConfig {
}
