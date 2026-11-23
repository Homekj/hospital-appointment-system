ALTER TABLE appointment ADD COLUMN refund_status VARCHAR(24) DEFAULT NULL AFTER cancellation_reason;
