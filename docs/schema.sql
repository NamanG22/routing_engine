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

CREATE TABLE IF NOT EXISTS notification_log (
  id                   BIGINT       NOT NULL AUTO_INCREMENT,
  event_id             VARCHAR(100) NOT NULL,
  transaction_id       VARCHAR(100) NOT NULL,
  payment_status       ENUM('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED') NOT NULL,
  notification_channel ENUM('SMS', 'EMAIL', 'IN_APP', 'PUSH', 'WEB') NOT NULL,
  notification_status  ENUM('PENDING', 'SENT', 'FAILED') NOT NULL DEFAULT 'PENDING',
  message              VARCHAR(500) NULL,
  created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_transaction_id (transaction_id),
  INDEX idx_event_id (event_id),
  UNIQUE KEY uk_notification_dedupe (transaction_id, payment_status, notification_channel)
);
