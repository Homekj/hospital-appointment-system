ALTER TABLE appointment
    ADD COLUMN check_in_time DATETIME(6) DEFAULT NULL AFTER pay_time,
    ADD INDEX idx_appt_queue (schedule_id, status, check_in_time, id);

-- 已签到的旧记录没有原始签到时间，以最后更新时间作为迁移时的排序依据。
UPDATE appointment SET check_in_time = update_time, update_time = update_time
WHERE status = 'checked_in' AND check_in_time IS NULL;
