package com.eventrouting.routing.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.eventrouting.routing.entity.NotificationTemplate;
import com.eventrouting.routing.entity.NotificationTemplateVersion;
import com.eventrouting.routing.enums.NotificationChannel;
import com.eventrouting.routing.repository.NotificationTemplateRepository;
import com.eventrouting.routing.repository.NotificationTemplateVersionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationTemplateVersionRepository notificationTemplateVersionRepository;

    public Optional<NotificationTemplateVersion> resolveLatestVersion(
            String upstreamId, String templateKey, NotificationChannel channel, String attribute1) {
        Optional<NotificationTemplate> template = attribute1 == null
                ? notificationTemplateRepository.findByUpstreamIdAndTemplateKeyAndChannelAndAttribute1IsNull(
                        upstreamId, templateKey, channel)
                : notificationTemplateRepository.findByUpstreamIdAndTemplateKeyAndChannelAndAttribute1(
                        upstreamId, templateKey, channel, attribute1);
        if (template.isEmpty()) {
            return Optional.empty();
        }
        return notificationTemplateVersionRepository.findTopByTemplateIdOrderByVersionDesc(
                template.get().getId());
    }

    public Optional<NotificationTemplateVersion> findVersionById(Long id) {
        return notificationTemplateVersionRepository.findById(id);
    }

    public String render(String body, Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return body;
        }

        String rendered = body;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String replacement = entry.getValue() != null ? entry.getValue() : "";
            rendered = rendered.replace(placeholder, replacement);
        }
        return rendered;
    }
}
