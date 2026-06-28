# Routing Engine

Kafka consumer service for the Event Routing Engine. Consumes payment-related events from a Kafka topic, persists them to MySQL, and routes processing by event type, payment status, and payment mode.

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
| `PAYMENT_STATUS_UPDATED` | Validates payload, saves to `payment_events`, routes by status (`SUCCESS`, `FAILED`, `PENDING`) and mode (`UPI_QR`, `ONLINE_CHECKOUT`) |
| `NOTIFICATION_DELIVERY_STATUS` | Placeholder — not yet implemented |

Duplicate events are skipped using idempotency tracking in `processed_events`.

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
├── config/       # Kafka consumer configuration
├── constants/    # Shared constants
├── dto/          # Incoming event payload
├── entity/       # JPA entities (payment_events, processed_events)
├── enums/        # Event, payment, and currency types
├── kafka/        # Kafka consumer
├── repository/   # Spring Data JPA repositories
└── service/      # Event processing logic
```

## Build

```bash
./mvnw clean package
```
