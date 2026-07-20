package com.eventrouting.routing.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.eventrouting.routing.dto.EventReceived;
import com.eventrouting.routing.service.EventsProcessor;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EventConsumer {

	private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

	private final ObjectMapper objectMapper;

	private final EventsProcessor eventsProcessor;

	@KafkaListener(
			topics = "${app.kafka.events-topic}",
			groupId = "${spring.kafka.consumer.group-id}")
	public void consume(ConsumerRecord<String, String> record) {
		processRecord(record);
	}

	@KafkaListener(
			topics = "${app.kafka.order-events-topic}",
			groupId = "${spring.kafka.consumer.group-id}")
	public void consumeOrderEvents(ConsumerRecord<String, String> record) {
		processRecord(record);
	}

	private void processRecord(ConsumerRecord<String, String> record) {
		try {
			EventReceived event = objectMapper.readValue(record.value(), EventReceived.class);
			log.info(
					"Received event [topic={}, partition={}, offsetʼ={}, key={}]: {}",
					record.topic(),
					record.partition(),
					record.offset(),
					record.key(),
					event);
			eventsProcessor.processEvent(event);
		} catch (Exception e) {
			log.error(
					"Failed to process event [topic={}, partition={}, offset={}, key={}]",
					record.topic(),
					record.partition(),
					record.offset(),
					record.key(),
					e);
		}
	}
}
