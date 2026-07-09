package com.eventrouting.routing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class TemplateServiceTest {

    private final TemplateService templateService = new TemplateService(null, null);

    @Test
    void renderSubstitutesPlaceholders() {
        String template = "Payment of {{amount}} {{currency}} is {{paymentStatus}} for {{transactionId}}";

        String rendered = templateService.render(
                template,
                Map.of(
                        "amount", "1500.00",
                        "currency", "INR",
                        "paymentStatus", "SUCCESS",
                        "transactionId", "TXN-1"));

        assertThat(rendered).isEqualTo("Payment of 1500.00 INR is SUCCESS for TXN-1");
    }

    @Test
    void renderReturnsTemplateWhenMetadataIsEmpty() {
        String template = "Hello {{name}}";

        assertThat(templateService.render(template, Map.of())).isEqualTo(template);
        assertThat(templateService.render(template, null)).isEqualTo(template);
    }
}
