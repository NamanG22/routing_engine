package com.eventrouting.routing.condition;

import java.util.Locale;
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * Comparison operators for notification rule conditions.
 * Add new operators here as needed.
 */
public enum ConditionOperator {
    GTE(cmp -> cmp >= 0),
    GT(cmp -> cmp > 0),
    LTE(cmp -> cmp <= 0),
    LT(cmp -> cmp < 0),
    EQ(cmp -> cmp == 0);

    private final IntPredicate matcher;

    ConditionOperator(IntPredicate matcher) {
        this.matcher = matcher;
    }

    public boolean matches(int comparisonResult) {
        return matcher.test(comparisonResult);
    }

    public static Optional<ConditionOperator> from(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(code.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
