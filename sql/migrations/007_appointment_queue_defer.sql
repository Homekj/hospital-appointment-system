ALTER TABLE appointment
    ADD COLUMN queue_joined_at DATETIME(6) DEFAULT NULL AFTER check_in_time,
    ADD COLUMN queue_deferred_at DATETIME(6) DEFAULT NULL AFTER queue_joined_at,
    DROP INDEX idx_appt_queue,
    ADD INDEX idx_appt_queue (schedule_id, status, queue_deferred_at, queue_joined_at, id);

UPDATE appointment
SET check_in_time = COALESCE(check_in_time, update_time),
    queue_joined_at = COALESCE(check_in_time, update_time), update_time = update_time
WHERE status = 'checked_in' AND queue_joined_at IS NULL;
