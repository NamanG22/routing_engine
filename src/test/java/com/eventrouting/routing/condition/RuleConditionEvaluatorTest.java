package com.eventrouting.routing.condition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventrouting.routing.dto.EventReceived;
import com.eventrouting.routing.entity.NotificationRuleCondition;
import com.eventrouting.routing.repository.NotificationRuleConditionRepository;

@ExtendWith(MockitoExtension.class)
class RuleConditionEvaluatorTest {

    @Mock
    private NotificationRuleConditionRepository conditionRepository;

    private RuleConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RuleConditionEvaluator(conditionRepository, List.of(new AmountFieldExtractor()));
    }

    @Test
    void matchesWhenNoConditions() {
        when(conditionRepository.findByNotificationRuleId(1L)).thenReturn(List.of());

        assertTrue(evaluator.matches(eventWithAmount("500"), 1L));
    }

    @Test
    void matchesAmountGteWhenAmountMeetsThreshold() {
        when(conditionRepository.findByNotificationRuleId(1L))
                .thenReturn(List.of(condition("amount", "GTE", "1000")));

        assertTrue(evaluator.matches(eventWithAmount("1000"), 1L));
        assertTrue(evaluator.matches(eventWithAmount("1500"), 1L));
    }

    @Test
    void doesNotMatchAmountGteWhenAmountBelowThreshold() {
        when(conditionRepository.findByNotificationRuleId(1L))
                .thenReturn(List.of(condition("amount", "GTE", "1000")));

        assertFalse(evaluator.matches(eventWithAmount("999"), 1L));
    }

    @Test
    void doesNotMatchUnsupportedOperator() {
        when(conditionRepository.findByNotificationRuleId(1L))
                .thenReturn(List.of(condition("amount", "CONTAINS", "1000")));

        assertFalse(evaluator.matches(eventWithAmount("1000"), 1L));
    }

    private static EventReceived eventWithAmount(String amount) {
        EventReceived event = new EventReceived();
        event.setAmount(amount);
        return event;
    }

    private static NotificationRuleCondition condition(String field, String operator, String value) {
        NotificationRuleCondition condition = new NotificationRuleCondition();
        condition.setId(10L);
        condition.setField(field);
        condition.setOperator(operator);
        condition.setValue(value);
        return condition;
    }
}
