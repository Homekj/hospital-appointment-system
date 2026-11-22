ALTER TABLE prescription
    ADD COLUMN refund_reason VARCHAR(200) DEFAULT NULL,
    ADD COLUMN refund_time DATETIME DEFAULT NULL,
    ADD COLUMN refund_operator_id BIGINT DEFAULT NULL;

INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, roles)
SELECT 0, '处方与发药', '/admin/prescriptions', 'FirstAidKit', 6, 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/prescriptions');
