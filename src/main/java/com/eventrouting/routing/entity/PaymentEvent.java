package com.eventrouting.routing.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import com.eventrouting.routing.enums.EventType;
import com.eventrouting.routing.enums.PaymentMode;
import com.eventrouting.routing.enums.PaymentStatus;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_events")
@Getter
@Setter
@NoArgsConstructor
public class PaymentEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false, unique = true, length = 100)
	private String eventId;

	@Column(name = "transaction_id", nullable = false, length = 100)
	private String transactionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 50)
	private EventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_mode", nullable = false, length = 50)
	private PaymentMode paymentMode;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false, length = 50)
	private PaymentStatus paymentStatus;

	@Column(name = "merchant_id", nullable = false, length = 100)
	private String merchantId;

	@Column(name = "customer_id", nullable = false, length = 100)
	private String customerId;

	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "currency", nullable = false, length = 10)
	private String currency;

	@Column(name = "event_timestamp")
	private LocalDateTime eventTimestamp;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata", columnDefinition = "json")
	private Map<String, String> metadata;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;
}
