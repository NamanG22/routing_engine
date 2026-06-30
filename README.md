# Routing Engine

Kafka consumer service for the Event Routing Engine. Consumes payment-related events from a Kafka topic, persists them to MySQL, routes processing by event type, payment status, and payment mode, and dispatches outbound notifications from a `notification_log` work queue.

Part of the Event Routing Engine system — pairs with the [upstream service](https://github.com/NamanG22/upstream_service) that publishes events via HTTP.

## Prerequisites

- Java 17+
- Apache Kafka (default: `localhost:9092`)
- MySQL 8+ (default: `localhost:3306`)

## Database setup

Create the database and tables before running the service:

```bash
mysql -u root -p < docs/schema.sql
```

Or run the SQL in `docs/schema.sql` manually. Update the user password to match your local configuration.

If you already have a `notification_log` table from an earlier version, apply the migration at the bottom of `docs/schema.sql` to add the `PROCESSING` status and polling index.

## Configuration

Settings are in `src/main/resources/application.properties`:

| Property | Default | Description |
| --- | --- | --- |
| `server.port` | `9091` | HTTP port |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker |
| `spring.kafka.consumer.group-id` | `routing-engine-group` | Consumer group |
| `app.kafka.events-topic` | `events` | Topic to consume |
| `DB_URL` | `jdbc:mysql://localhost:3306/routing_engine?serverTimezone=Asia/Kolkata` | MySQL JDBC URL |
| `DB_USERNAME` | `routing_user` | Database user |
| `DB_PASSWORD` | *(required)* | Database password |
| `app.notification.dispatcher.enabled` | `true` | Enable the background notification dispatcher |
| `app.notification.dispatcher.poll-interval-ms` | `5000` | Delay between poll cycles (ms) |
| `app.notification.dispatcher.batch-size` | `10` | Max pending notifications claimed per poll |
| `app.notification.dispatcher.processing-delay-min-ms` | `10000` | Min simulated send duration (ms) |
| `app.notification.dispatcher.processing-delay-max-ms` | `20000` | Max simulated send duration (ms) |
| `app.notification.dispatcher.success-ratio` | `0.9` | Fraction of notifications marked `SENT` (rest are `FAILED`) |

Override locally by copying the example file (loaded automatically — no profile flag needed):

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
# edit application-local.properties and set your MySQL password
```

Or export an environment variable:

```bash
export DB_PASSWORD=your-local-password
```

## Run locally

Start Kafka and MySQL, then:

```bash
./mvnw spring-boot:run
```

The service starts on `http://localhost:9091` and consumes from the `events` topic.

## Event processing

Supported event types:

| `eventType` | Behavior |
| --- | --- |
| `PAYMENT_STATUS_UPDATED` | Validates payload, saves to `payment_events`, routes by status (`SUCCESS`, `FAILED`, `PENDING`) and mode (`UPI_QR`, `ONLINE_CHECKOUT`), and creates `notification_log` entries where applicable |
| `NOTIFICATION_DELIVERY_STATUS` | Placeholder — not yet implemented |

Duplicate events are skipped using idempotency tracking in `processed_events`.

### Notification routing

When a payment event is processed, the service writes rows to `notification_log` with status `PENDING`. Duplicate notifications for the same `(transaction_id, payment_status, channel)` are skipped.

| Payment status | Mode | Channels |
| --- | --- | --- |
| `SUCCESS` | `ONLINE_CHECKOUT` | SMS, EMAIL, WEB |
| `SUCCESS` | `UPI_QR` | SMS; EMAIL if amount > 1000 |
| `FAILED` | `ONLINE_CHECKOUT` | WEB, EMAIL |
| `FAILED` | `UPI_QR` | IN_APP |
| `PENDING` | — | *(scheduled notifications — not yet implemented)* |

### Notification dispatcher

A single-instance background dispatcher polls `notification_log` and simulates sending notifications:

```
PENDING  →  PROCESSING  →  SENT (90%) or FAILED (10%)
```

1. Every `poll-interval-ms`, fetch up to `batch-size` oldest `PENDING` rows.
2. Mark them `PROCESSING` in a single transaction.
3. Schedule completion after a random delay between `processing-delay-min-ms` and `processing-delay-max-ms`.
4. Mark each row `SENT` or `FAILED` based on `success-ratio`.

Check dispatch progress:

```sql
SELECT id, notification_channel, notification_status, created_at, updated_at
FROM notification_log
ORDER BY id DESC;
```

Set `app.notification.dispatcher.enabled=false` to disable polling (e.g. during tests).

### Sample Kafka message

```json
{
  "eventId": "1001",
  "eventType": "PAYMENT_STATUS_UPDATED",
  "paymentMode": "UPI_QR",
  "paymentStatus": "SUCCESS",
  "merchantId": "1001",
  "customerId": "1001",
  "transactionId": "1001",
  "amount": "250",
  "currency": "INR",
  "metadata": {
    "source": "mobile-app"
  },
  "timestamp": "2026-06-28 12:00:00"
}
```

## Tests

```bash
./mvnw test
```

Tests use embedded Kafka and an in-memory H2 database — no external Kafka or MySQL required.

## Project structure

```
src/main/java/com/eventrouting/routing/
├── config/       # Kafka consumer and notification dispatcher configuration
├── constants/    # Shared constants
├── dto/          # Incoming event payload
├── entity/       # JPA entities (payment_events, processed_events, notification_log)
├── enums/        # Event, payment, notification, and currency types
├── kafka/        # Kafka consumer
├── repository/   # Spring Data JPA repositories
└── service/      # Event processing, notification creation, and dispatch logic
```

## Build

```bash
./mvnw clean package
```
