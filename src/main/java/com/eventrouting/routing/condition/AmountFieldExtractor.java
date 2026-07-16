package com.eventrouting.routing.condition;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eventrouting.routing.dto.EventReceived;

@Component
public class AmountFieldExtractor implements EventFieldExtractor {

    @Override
    public String fieldName() {
        return "amount";
    }

    @Override
    public Comparable<?> extract(EventReceived event) {
        if (event == null || !StringUtils.hasText(event.getAmount())) {
            return null;
        }
        return parseDecimal(event.getAmount());
    }

    @Override
    public Comparable<?> parseExpected(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseDecimal(value);
    }

    private BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
