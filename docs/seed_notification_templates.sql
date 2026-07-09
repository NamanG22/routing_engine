-- Sample notification templates for local development
-- Run after schema.sql: mysql -u root -p routing_engine < docs/seed_notification_templates.sql

USE routing_engine;

INSERT INTO notification_templates (upstream_id, template_key, channel, attribute1) VALUES
  ('10001', '1001', 'SMS', 'SUCCESS'),
  ('10001', '1001', 'EMAIL', 'SUCCESS'),
  ('10001', '1001', 'WEB', 'SUCCESS'),
  ('10001', '1001', 'IN_APP', 'SUCCESS'),
  ('10001', '1001', 'PUSH', 'SUCCESS'), 
  ('10001', '1001', 'SMS', 'FAILED'),
  ('10001', '1001', 'EMAIL', 'FAILED'),
  ('10001', '1001', 'WEB', 'FAILED'),
  ('10001', '1001', 'IN_APP', 'FAILED'),
  ('10001', '1001', 'PUSH', 'FAILED');

INSERT INTO notification_template_versions (template_id, version, body) VALUES
  -- ((SELECT id FROM notification_templates WHERE upstream_id = '10001' AND template_key = '1001' AND channel = 'SMS' AND attribute1 = 'SUCCESS'), 1, 'Your payment of {{amount}} {{currency}} was successful. Transaction ID: {{transactionId}}. Thank you for your payment.'),
  ((SELECT id FROM notification_templates WHERE upstream_id = '10001' AND template_key = '1001' AND channel = 'EMAIL' AND attribute1 = 'SUCCESS'), 1, 'Hello,\nYour payment of {{amount}} {{currency}} was successful.\nTransaction ID: {{transactionId}}.\n\nThank you for your payment.'),
  -- ((SELECT id FROM notification_templates WHERE upstream_id = '10001' AND template_key = '1001' AND channel = 'WEB' AND attribute1 = 'SUCCESS'), 1, 'Payment successful: {{amount}} {{currency}} ({{transactionId}}).'),
  -- ((SELECT id FROM notification_templates WHERE upstream_id = '10001' AND template_key = '1001' AND channel = 'IN_APP' AND attribute1 = 'SUCCESS'), 1, 'Payment of {{amount}} {{currency}} was successful for transaction {{transactionId}}.'),
  -- ((SELECT id FROM notification_templates WHERE upstream_id = '10001' AND template_key = '1001' AND channel = 'PUSH' AND attribute1 = 'SUCCESS'), 1, 'Payment of {{amount}} {{currency}} was successful for transaction {{transactionId}}.'),
  ((SELECT id FROM notification_templates WHERE upstream_id = '10001' AND template_key = '1001' AND channel = 'SMS' AND attribute1 = 'FAILED'), 1, 'Your payment of {{amount}} {{currency}} could not be processed. Please try again. If the amount was deducted, it will be refunded as per your bank’s policy. Transaction ID: [Txn ID].'),
  -- ((SELECT id FROM notification_templates WHERE upstream_id = '10001' AND template_key = '1001' AND channel = 'EMAIL' AND attribute1 = 'FAILED'), 1, 'Your payment of {{amount}} {{currency}} for transaction {{transactionId}} failed.'),
  -- ((SELECT id FROM notification_templates WHERE upstream_id = '10001' AND template_key = '1001' AND channel = 'WEB' AND attribute1 = 'FAILED'), 1, 'Payment failed for transaction {{transactionId}}.'),
  ((SELECT id FROM notification_templates WHERE upstream_id = '10001' AND template_key = '1001' AND channel = 'IN_APP' AND attribute1 = 'FAILED'), 1, 'Unable to complete the payment. Please try again or use a different payment method.'),
  -- ((SELECT id FROM notification_templates WHERE upstream_id = '10001' AND template_key = '1001' AND channel = 'PUSH' AND attribute1 = 'FAILED'), 1, 'Payment of {{amount}} {{currency}} failed for {{transactionId}}.');