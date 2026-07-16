-- Routing Engine database schema
-- Run: mysql -u root -p < docs/schema.sql

CREATE DATABASE IF NOT EXISTS routing_engine
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'routing_user'@'localhost' IDENTIFIED BY 'changeme';
GRANT ALL PRIVILEGES ON routing_engine.* TO 'routing_user'@'localhost';
FLUSH PRIVILEGES;

USE routing_engine;

CREATE TABLE IF NOT EXISTS processed_events (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  event_id     VARCHAR(100) NOT NULL,
  processed_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_processed_events_event_id (event_id)
);

CREATE TABLE IF NOT EXISTS payment_events (
  id              BIGINT         NOT NULL AUTO_INCREMENT,
  event_id        VARCHAR(100)   NOT NULL,
  transaction_id  VARCHAR(100)   NOT NULL,
  event_type      VARCHAR(50)    NOT NULL,
  payment_mode    VARCHAR(50)    NOT NULL,
  payment_status  VARCHAR(50)    NOT NULL,
  merchant_id     VARCHAR(100)   NOT NULL,
  customer_id     VARCHAR(100)   NOT NULL,
  amount          DECIMAL(12, 2) NOT NULL,
  currency        VARCHAR(10)    NOT NULL,
  event_timestamp DATETIME       NULL,
  metadata        JSON           NULL,
  created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_events_event_id (event_id)
);

CREATE TABLE IF NOT EXISTS notification_rules (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  upstream_id  VARCHAR(100) NOT NULL,
  event        VARCHAR(50)  NOT NULL,
  mode         VARCHAR(50)  NOT NULL,
  status       VARCHAR(50)  NOT NULL,
  channel      ENUM('SMS', 'EMAIL', 'IN_APP', 'PUSH', 'WEB') NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS notification_rule_conditions (
  id                   BIGINT       NOT NULL AUTO_INCREMENT,
  notification_rule_id BIGINT       NOT NULL,
  field                VARCHAR(100) NOT NULL,
  operator             VARCHAR(50)  NOT NULL,
  value                VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_notification_rule_conditions_rule
    FOREIGN KEY (notification_rule_id) REFERENCES notification_rules (id)
);

CREATE TABLE IF NOT EXISTS notification_templates (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  upstream_id  VARCHAR(100) NOT NULL,
  template_key VARCHAR(100) NOT NULL,
  channel      ENUM('SMS', 'EMAIL', 'IN_APP', 'PUSH', 'WEB') NOT NULL,
  attribute1   VARCHAR(50)  NULL,
  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_templates_lookup (upstream_id, template_key, channel, attribute1)
);

CREATE TABLE IF NOT EXISTS notification_template_versions (
  id          BIGINT    NOT NULL AUTO_INCREMENT,
  template_id BIGINT    NOT NULL,
  version     INT       NOT NULL,
  body        TEXT      NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_version (template_id, version),
  CONSTRAINT fk_template_versions_template
    FOREIGN KEY (template_id) REFERENCES notification_templates (id)
);

CREATE TABLE IF NOT EXISTS notification_log (
  id                   BIGINT       NOT NULL AUTO_INCREMENT,
  event_id             VARCHAR(100) NOT NULL,
  transaction_id       VARCHAR(100) NOT NULL,
  notification_status  ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED') NOT NULL DEFAULT 'PENDING',
  template_version_id  BIGINT       NOT NULL,
  metadata             JSON         NULL,
  created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_transaction_id (transaction_id),
  INDEX idx_notification_status_created (notification_status, created_at),
  CONSTRAINT fk_notification_log_template_version
    FOREIGN KEY (template_version_id) REFERENCES notification_template_versions (id)
);
