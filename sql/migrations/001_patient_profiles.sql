CREATE TABLE IF NOT EXISTS patient_profile (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    name          VARCHAR(50) NOT NULL,
    phone         VARCHAR(20) NOT NULL,
    relationship  VARCHAR(12) NOT NULL,
    is_default    BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_patient_profile (user_id, name, phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='常用就诊人';

ALTER TABLE appointment ADD COLUMN patient_profile_id BIGINT DEFAULT NULL AFTER schedule_id;
