package com.eventrouting.routing.condition;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.eventrouting.routing.dto.EventReceived;
import com.eventrouting.routing.entity.NotificationRuleCondition;
import com.eventrouting.routing.repository.NotificationRuleConditionRepository;

@Component
public class RuleConditionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(RuleConditionEvaluator.class);

    private final NotificationRuleConditionRepository notificationRuleConditionRepository;
    private final Map<String, EventFieldExtractor> fieldExtractors;

    public RuleConditionEvaluator(
            NotificationRuleConditionRepository notificationRuleConditionRepository,
            List<EventFieldExtractor> fieldExtractors) {
        this.notificationRuleConditionRepository = notificationRuleConditionRepository;
        this.fieldExtractors = fieldExtractors.stream()
                .collect(Collectors.toMap(
                        extractor -> extractor.fieldName().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> {
                            throw new IllegalStateException(
                                    "Duplicate EventFieldExtractor for field: " + left.fieldName());
                        }));
    }

    /**
     * Returns true when the rule has no conditions, or every condition matches the event (AND).
     */
    public boolean matches(EventReceived event, Long ruleId) {
        List<NotificationRuleCondition> conditions =
                notificationRuleConditionRepository.findByNotificationRuleId(ruleId);
        if (conditions.isEmpty()) {
            return true;
        }

        for (NotificationRuleCondition condition : conditions) {
            if (!evaluate(event, condition)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluate(EventReceived event, NotificationRuleCondition condition) {
        Optional<ConditionOperator> operator = ConditionOperator.from(condition.getOperator());
        if (operator.isEmpty()) {
            log.error(
                    "Unsupported condition operator id={}, operator={}",
                    condition.getId(),
                    condition.getOperator());
            return false;
        }

        EventFieldExtractor extractor = resolveExtractor(condition.getField());
        if (extractor == null) {
            log.error(
                    "Unsupported condition field id={}, field={}",
                    condition.getId(),
                    condition.getField());
            return false;
        }

        Comparable<?> actual = extractor.extract(event);
        Comparable<?> expected = extractor.parseExpected(condition.getValue());
        if (actual == null || expected == null) {
            log.error(
                    "Cannot evaluate condition id={}, field={}, operator={}, value={}, eventAmount={}",
                    condition.getId(),
                    condition.getField(),
                    condition.getOperator(),
                    condition.getValue(),
                    event.getAmount());
            return false;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        int comparison = ((Comparable) actual).compareTo(expected);
        return operator.get().matches(comparison);
    }

    private EventFieldExtractor resolveExtractor(String field) {
        if (field == null || field.isBlank()) {
            return null;
        }
        return fieldExtractors.get(field.trim().toLowerCase(Locale.ROOT));
    }
}
