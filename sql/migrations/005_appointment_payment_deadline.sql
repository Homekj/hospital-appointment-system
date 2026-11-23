ALTER TABLE appointment
    ADD COLUMN payment_deadline DATETIME DEFAULT NULL AFTER pay_time,
    ADD COLUMN cancellation_type VARCHAR(24) DEFAULT NULL,
    ADD COLUMN cancellation_reason VARCHAR(200) DEFAULT NULL,
    ADD INDEX idx_appt_payment_deadline (status, payment_deadline, id);

-- 原有未支付订单以创建时间起 15 分钟为保留期。
UPDATE appointment
SET payment_deadline = TIMESTAMPADD(MINUTE, 15, create_time), update_time = update_time
WHERE status = 'unpaid' AND payment_deadline IS NULL;

-- 修正历史初始化脚本未计入待支付预约导致的号源计数差异。
UPDATE schedule s SET s.booked_count = (
    SELECT COUNT(*) FROM appointment a
    WHERE a.schedule_id = s.id AND a.status <> 'cancelled'
);
