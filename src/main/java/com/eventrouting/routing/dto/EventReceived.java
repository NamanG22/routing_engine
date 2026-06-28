package com.eventrouting.routing.dto;

import java.util.Map;

import com.eventrouting.routing.enums.Currency;
import com.eventrouting.routing.enums.EventType;
import com.eventrouting.routing.enums.PaymentMode;
import com.eventrouting.routing.enums.PaymentStatus;

import lombok.Data;

@Data
public class EventReceived {

	private String eventId;
    private EventType eventType;
    private PaymentMode paymentMode;
    private PaymentStatus paymentStatus;
    private String merchantId;
    private String customerId;
    private String transactionId;
    private String amount;
    private Currency currency;
    private Map<String, String> metadata;
    private String timestamp;

	@Override
	public String toString() {
		return "EventReceived{eventType='" + eventType + "', paymentMode='" + paymentMode + "', paymentStatus='" + paymentStatus + "', merchantId='" + merchantId + "', customerId='" + customerId + "', transactionId='" + transactionId + "', amount='" + amount + "', currency='" + currency + "', metadata='" + metadata + "', timestamp='" + timestamp + "'}";
	}
}
