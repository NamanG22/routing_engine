package com.eventrouting.routing.condition;

import com.eventrouting.routing.dto.EventReceived;

/**
 * Resolves a named event field into a comparable value for rule evaluation.
 * Implement one bean per supported field (e.g. amount, currency).
 */
public interface EventFieldExtractor {

    /** Field name as stored in notification_rule_conditions.field (case-insensitive). */
    String fieldName();

    /** Actual value from the event, or null if missing/invalid. */
    Comparable<?> extract(EventReceived event);

    /** Parses the expected threshold from notification_rule_conditions.value. */
    Comparable<?> parseExpected(String value);
}
