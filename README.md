# Routing Engine

Kafka consumer service for the Event Routing Engine. Consumes payment-related events from a Kafka topic, persists them to MySQL, matches DB-driven notification rules (with optional conditions), and dispatches outbound notifications from a `notification_log` work queue.

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

If you already have an older schema, recreate the database or migrate manually to the tables in `docs/schema.sql`.

### Local seed data

Seed SQL for templates/rules is local-only and gitignored (`docs/seed_notification_templates.sql`). Create your own file for local development, for example:

```bash
# create docs/seed_notification_templates.sql with inserts for
# notification_templates, notification_template_versions,
# notification_rules, and notification_rule_conditions
mysql -u root -p routing_engine < docs/seed_notification_templates.sql
```

### Notification rules

Routing is driven by two tables:

| Table | Purpose |
| --- | --- |
| `notification_rules` | Match `(event, mode, status)` → `channel` (plus `upstream_id`) |
| `notification_rule_conditions` | Optional predicates on a rule (`field`, `operator`, `value`) |

For each matching rule:

1. If the rule has **no** conditions → create a notification for that channel.
2. If it has conditions → **all** must pass (AND). Example: `field=amount`, `operator=GTE`, `value=1000` means amount ≥ 1000.

Supported operators today: `GTE`, `GT`, `LTE`, `LT`, `EQ`. Supported fields today: `amount`. Add new fields via an `EventFieldExtractor` bean; add operators in `ConditionOperator`.

### Notification templates

Templates are split across two tables:

| Table | Purpose |
| --- | --- |
| `notification_templates` | Logical template lookup: `(upstream_id, template_key, channel, attribute1)` |
| `notification_template_versions` | Versioned body text; latest `version` is used at create time |

When creating a notification, the service resolves the template using:

- `upstreamId` from the event
- `metadata.templateKey` from the event
- `channel` from the matched notification rule
- `attribute1` from `paymentStatus` when present (e.g. `SUCCESS`, `FAILED`); omitted templates use `attribute1 IS NULL`

The latest version id is stored on `notification_log.template_version_id` so dispatch always renders the body that was pinned at create time.

To update a template body, insert a new row in `notification_template_versions` with a higher `version`. Existing notification logs keep pointing at the version they were created with.

Placeholders use `{{key}}` syntax (e.g. `{{amount}}`, `{{transactionId}}`).

Duplicate notifications are skipped per `(transaction_id, template_id)` — a new template version does not bypass dedupe for the same logical template.

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
| `PAYMENT_STATUS_UPDATED` | Validates payload, saves to `payment_events`, looks up `notification_rules` by event/mode/status, evaluates optional conditions, and creates `notification_log` entries for matching channels |
| `NOTIFICATION_DELIVERY_STATUS` | Placeholder — not yet implemented |

Duplicate events are skipped using idempotency tracking in `processed_events`.

### Notification routing

Flow for each event:

```
save payment_events
  → find notification_rules (event + mode + status)
  → for each rule, evaluate notification_rule_conditions (if any)
  → create notification_log (PENDING) for the rule channel
```

Duplicate notifications for the same `(transaction_id, template)` are skipped.

### Notification dispatcher

A single-instance background dispatcher polls `notification_log` and simulates sending notifications:

```
PENDING  →  PROCESSING  →  SENT (90%) or FAILED (10%)
```

1. Every `poll-interval-ms`, fetch up to `batch-size` oldest `PENDING` rows.
2. Mark them `PROCESSING` and record `processingStartedAt` in metadata.
3. Schedule completion after a random delay between `processing-delay-min-ms` and `processing-delay-max-ms`.
4. Render the template version body, simulate send, and mark each row `SENT` or `FAILED` based on `success-ratio`.
5. Append `dispatchDurationMs` to metadata on completion.

Check dispatch progress:

```sql
SELECT id, notification_status, template_version_id, created_at, updated_at
FROM notification_log
ORDER BY id DESC;
```

Set `app.notification.dispatcher.enabled=false` to disable polling (e.g. during tests).

### Sample Kafka message

```json
{
  "eventId": "1001",
  "upstreamId": "upstream-1001",
  "eventType": "PAYMENT_STATUS_UPDATED",
  "paymentMode": "UPI_QR",
  "paymentStatus": "SUCCESS",
  "merchantId": "1001",
  "customerId": "1001",
  "transactionId": "1001",
  "amount": "250",
  "currency": "INR",
  "metadata": {
    "source": "mobile-app",
    "templateKey": "PAYMENT_STATUS_UPDATED"
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
├── condition/    # Rule condition evaluation (operators + field extractors)
├── config/       # Kafka consumer and notification dispatcher configuration
├── constants/    # Shared constants
├── dto/          # Incoming event payload
├── entity/       # JPA entities (payment_events, processed_events, notification_*)
├── enums/        # Event, payment, notification, and currency types
├── kafka/        # Kafka consumer
├── repository/   # Spring Data JPA repositories
└── service/      # Event processing, template rendering, notification creation, and dispatch logic
```

## Build

```bash
./mvnw clean package
```
