-- =============================================================================
-- 管理系统数据库全量脚本（唯一维护入口）
-- 学生/新环境导入（项目根目录）：
--   mysql -u root -p --default-character-set=utf8mb4 < sql/system.sql
-- 说明：全量会 DROP 库后重建；日常改库请直接维护本文件
-- =============================================================================

CREATE DATABASE IF NOT EXISTS hospital_booking DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE hospital_booking;

-- -----------------------------------------------------------------------------
-- 表结构
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS balance_log;
DROP TABLE IF EXISTS user_notification;
DROP TABLE IF EXISTS prescription_item;
DROP TABLE IF EXISTS prescription;
DROP TABLE IF EXISTS consultation_record;
DROP TABLE IF EXISTS medicine;
DROP TABLE IF EXISTS appointment;
DROP TABLE IF EXISTS patient_profile;
DROP TABLE IF EXISTS appointment_review;
DROP TABLE IF EXISTS schedule;
DROP TABLE IF EXISTS doctor;
DROP TABLE IF EXISTS department;
DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;

CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30)  NOT NULL COMMENT '角色编码',
    name        VARCHAR(50)  NOT NULL COMMENT '角色名称',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像访问路径，如 /uploads/avatar/xxx.jpg',
    openid      VARCHAR(64)  DEFAULT NULL COMMENT '微信小程序 openid',
    balance     DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    doctor_id   BIGINT       DEFAULT NULL COMMENT '医生角色绑定的 doctor.id',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    role        VARCHAR(30)  NOT NULL DEFAULT 'USER' COMMENT '角色编码，对应 sys_role.code',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE sys_menu (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单，0为顶级',
    name        VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    path        VARCHAR(100) NOT NULL COMMENT '前端路由',
    icon        VARCHAR(50)  DEFAULT NULL COMMENT '图标名（Element Plus Icons）',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    roles       VARCHAR(100) NOT NULL DEFAULT 'ADMIN' COMMENT '固定 ADMIN（仅后台菜单）',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

CREATE TABLE sys_login_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    ip          VARCHAR(50)  DEFAULT NULL COMMENT 'IP',
    status      TINYINT      NOT NULL COMMENT '0失败 1成功',
    message     VARCHAR(200) DEFAULT NULL COMMENT '说明',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

CREATE TABLE sys_operation_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  DEFAULT NULL COMMENT '操作人',
    module      VARCHAR(50)  DEFAULT NULL COMMENT '模块',
    action      VARCHAR(50)  DEFAULT NULL COMMENT '操作',
    method      VARCHAR(10)  DEFAULT NULL COMMENT '请求方式',
    url         VARCHAR(200) DEFAULT NULL COMMENT '请求地址',
    ip          VARCHAR(50)  DEFAULT NULL COMMENT 'IP',
    cost_ms     INT          DEFAULT NULL COMMENT '耗时毫秒',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0失败 1成功',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

CREATE TABLE department (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL COMMENT '科室名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '科室简介',
    location    VARCHAR(200) DEFAULT NULL COMMENT '所在楼层/位置',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_dept_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室';

CREATE TABLE doctor (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    department_id BIGINT       NOT NULL COMMENT '所属科室',
    name          VARCHAR(50)  NOT NULL COMMENT '姓名',
    title         VARCHAR(50)  DEFAULT NULL COMMENT '职称',
    gender        TINYINT      NOT NULL DEFAULT 1 COMMENT '1男 2女',
    avatar        VARCHAR(255) DEFAULT NULL COMMENT '头像路径',
    intro         VARCHAR(1000) DEFAULT NULL COMMENT '简介',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_doctor_dept (department_id),
    KEY idx_doctor_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生';

CREATE TABLE schedule (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    doctor_id     BIGINT      NOT NULL COMMENT '医生',
    schedule_date DATE        NOT NULL COMMENT '出诊日期',
    time_slot     VARCHAR(20) NOT NULL COMMENT '时段',
    registration_fee DECIMAL(10,2) NOT NULL DEFAULT 20.00 COMMENT '挂号费',
    total_quota   INT         NOT NULL COMMENT '总号源',
    booked_count  INT         NOT NULL DEFAULT 0 COMMENT '已预约数',
    status        TINYINT     NOT NULL DEFAULT 1 COMMENT '0停诊 1正常',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_doctor_date_slot (doctor_id, schedule_date, time_slot),
    KEY idx_schedule_date (schedule_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班号源';

CREATE TABLE balance_log (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    user_id       BIGINT         NOT NULL COMMENT '用户',
    type          VARCHAR(20)    NOT NULL COMMENT 'recharge/order_pay/order_refund',
    amount        DECIMAL(10, 2) NOT NULL COMMENT '变动金额，充值为正，支付为负',
    balance_after DECIMAL(10, 2) NOT NULL COMMENT '变动后余额',
    order_id      BIGINT         DEFAULT NULL COMMENT '关联预约 ID',
    pay_method    VARCHAR(20)    DEFAULT NULL COMMENT 'wechat/alipay/balance',
    remark        VARCHAR(100)   DEFAULT NULL COMMENT '备注',
    create_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_balance_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额流水';

CREATE TABLE user_notification (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '接收用户',
    type        VARCHAR(50)  NOT NULL COMMENT '通知类型',
    title       VARCHAR(100) NOT NULL COMMENT '标题',
    content     VARCHAR(500) NOT NULL COMMENT '内容',
    biz_type    VARCHAR(30)  DEFAULT NULL COMMENT 'appointment/prescription',
    biz_id      BIGINT       DEFAULT NULL COMMENT '业务主键',
    read_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notify_user (user_id, read_flag, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息通知';

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

CREATE TABLE appointment (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL COMMENT '预约用户',
    schedule_id   BIGINT      NOT NULL COMMENT '关联排班',
    patient_profile_id BIGINT DEFAULT NULL COMMENT '常用就诊人引用，历史以预约快照为准',
    patient_name  VARCHAR(50) NOT NULL COMMENT '就诊人姓名',
    patient_phone VARCHAR(20) NOT NULL COMMENT '就诊人手机',
    id_card       VARCHAR(30) DEFAULT NULL COMMENT '身份证号',
    amount        DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '挂号费用',
    pay_method    VARCHAR(20) DEFAULT NULL COMMENT 'balance/wechat/alipay',
    pay_time      DATETIME    DEFAULT NULL COMMENT '支付时间',
    payment_deadline DATETIME DEFAULT NULL COMMENT '待支付截止时间',
    check_in_time DATETIME(6) DEFAULT NULL COMMENT '到院签到时间',
    queue_joined_at DATETIME(6) DEFAULT NULL COMMENT '本次进入候诊队列时间',
    queue_deferred_at DATETIME(6) DEFAULT NULL COMMENT '过号等待重新排队时间',
    cancellation_type VARCHAR(24) DEFAULT NULL,
    cancellation_reason VARCHAR(200) DEFAULT NULL,
    refund_status VARCHAR(24) DEFAULT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'unpaid' COMMENT 'unpaid/pending/checked_in/in_consultation/pending_rx_pay/completed/cancelled',
    remark        VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_appt_user (user_id),
    KEY idx_appt_schedule (schedule_id),
    KEY idx_appt_queue (schedule_id, status, queue_deferred_at, queue_joined_at, id),
    KEY idx_appt_payment_deadline (status, payment_deadline, id),
    KEY idx_appt_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录';

CREATE TABLE appointment_review (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    appointment_id  BIGINT       NOT NULL COMMENT '预约',
    user_id         BIGINT       NOT NULL COMMENT '评价用户',
    doctor_id       BIGINT       NOT NULL COMMENT '医生',
    rating          TINYINT      NOT NULL COMMENT '评分1-5',
    tags            VARCHAR(200) DEFAULT NULL COMMENT '快捷标签，逗号分隔',
    comment         VARCHAR(500) DEFAULT NULL COMMENT '文字评价',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment (appointment_id),
    KEY idx_doctor (doctor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='就诊评价';

CREATE TABLE medicine (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)   NOT NULL COMMENT '药品名称',
    spec        VARCHAR(100)   DEFAULT NULL COMMENT '规格',
    unit        VARCHAR(20)    NOT NULL DEFAULT '盒' COMMENT '单位',
    price       DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    stock       INT            NOT NULL DEFAULT 0 COMMENT '库存',
    usage_desc  VARCHAR(200)   DEFAULT NULL COMMENT '用法说明',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_medicine_name (name),
    KEY idx_medicine_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品库';

CREATE TABLE consultation_record (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    appointment_id   BIGINT       NOT NULL COMMENT '预约',
    doctor_id        BIGINT       NOT NULL COMMENT '接诊医生',
    chief_complaint  VARCHAR(500) DEFAULT NULL COMMENT '主诉',
    present_illness  VARCHAR(1000) DEFAULT NULL COMMENT '现病史',
    diagnosis        VARCHAR(500) DEFAULT NULL COMMENT '诊断',
    treatment_plan   VARCHAR(1000) DEFAULT NULL COMMENT '治疗方案',
    doctor_advice    VARCHAR(500) DEFAULT NULL COMMENT '医嘱',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_consultation_appt (appointment_id),
    KEY idx_consultation_doctor (doctor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊病历';

CREATE TABLE prescription (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT         NOT NULL COMMENT '预约',
    user_id        BIGINT         NOT NULL COMMENT '患者用户',
    doctor_id      BIGINT         NOT NULL COMMENT '开方医生',
    total_amount   DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '处方总金额',
    status         VARCHAR(20)    NOT NULL DEFAULT 'unpaid' COMMENT 'unpaid/paid/dispensed/cancelled',
    pay_method     VARCHAR(20)    DEFAULT NULL COMMENT 'balance/wechat/alipay',
    pay_time       DATETIME       DEFAULT NULL COMMENT '支付时间',
    refund_reason  VARCHAR(200)   DEFAULT NULL,
    refund_time    DATETIME       DEFAULT NULL,
    refund_operator_id BIGINT     DEFAULT NULL,
    create_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prescription_appt (appointment_id),
    KEY idx_prescription_user (user_id),
    KEY idx_prescription_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方单';

CREATE TABLE prescription_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    prescription_id BIGINT         NOT NULL COMMENT '处方',
    medicine_id     BIGINT         NOT NULL COMMENT '药品',
    medicine_name   VARCHAR(100)   NOT NULL COMMENT '药品名称快照',
    spec            VARCHAR(100)   DEFAULT NULL COMMENT '规格快照',
    dosage          VARCHAR(50)    DEFAULT NULL COMMENT '单次剂量',
    frequency       VARCHAR(50)    DEFAULT NULL COMMENT '频次',
    days            INT            DEFAULT NULL COMMENT '天数',
    quantity        INT            NOT NULL DEFAULT 1 COMMENT '数量',
    unit_price      DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '单价快照',
    amount          DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '小计',
    PRIMARY KEY (id),
    KEY idx_rx_item_prescription (prescription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方明细';

-- -----------------------------------------------------------------------------
-- 初始数据：角色（方案 A，用户表 role 存 code，一人一角）
-- 扩展：INSERT INTO sys_role (code, name, sort_order, status) VALUES ('TEACHER', '教师', 3, 1);
--       并在路由 meta.roles、@PreAuthorize 中配置对应 code（菜单表仅后台 ADMIN）
-- -----------------------------------------------------------------------------

INSERT INTO sys_role (code, name, sort_order, status) VALUES
('ADMIN',  '管理员', 1, 1),
('USER',   '用户',   2, 1),
('DOCTOR', '医生',   3, 1);

-- -----------------------------------------------------------------------------
-- 初始数据：用户（admin 密码 admin；患者仅微信小程序登录，演示数据绑定 openid 用户）
-- -----------------------------------------------------------------------------

INSERT INTO sys_user (username, password, nickname, email, phone, balance, status, role, doctor_id, openid) VALUES
('admin',  '$2a$10$B4MRmEk2dlZGVJpSjYYlduf2mJA3VbiaZXECIv4gHVdhBWprShqEm', '系统管理员', 'admin@system.com', '13800000001', 0.00, 1, 'ADMIN',  NULL, NULL),
('wx_ogHVa5P9lsJ4o0rAE2EKJeCD-OAw', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '青哥哥', NULL, '13988000000', 750.00, 1, 'USER', NULL, 'ogHVa5P9lsJ4o0rAE2EKJeCD-OAw');

-- -----------------------------------------------------------------------------
-- 初始数据：后台菜单（仅 /admin/*；用户前台路由写死在前端，不入库）
-- -----------------------------------------------------------------------------

INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, roles) VALUES
(0, '数据概览', '/admin/dashboard', 'HomeFilled', 1, 'ADMIN'),
(0, '科室管理', '/admin/departments',   'OfficeBuilding', 2, 'ADMIN'),
(0, '医生管理', '/admin/doctors',       'Avatar',         3, 'ADMIN'),
(0, '排班管理', '/admin/schedules',     'Calendar',       4, 'ADMIN'),
(0, '预约管理', '/admin/appointments',  'Tickets',        5, 'ADMIN'),
(0, '处方与发药', '/admin/prescriptions', 'FirstAidKit', 6, 'ADMIN'),
(0, '药品管理', '/admin/medicines',     'FirstAidKit',    6, 'ADMIN'),
(0, '用户管理', '/admin/users',     'User',       7, 'ADMIN'),
(0, '系统日志', '/admin/logs',      'Document',   8, 'ADMIN'),
(0, '菜单管理', '/admin/menus',     'Menu',       9, 'ADMIN'),
(0, '工作概览', '/admin/doctor/dashboard',    'HomeFilled', 1, 'DOCTOR'),
(0, '我的排班', '/admin/doctor/schedules',    'Calendar',   2, 'DOCTOR'),
(0, '我的预约', '/admin/doctor/appointments', 'Tickets',    3, 'DOCTOR');

-- -----------------------------------------------------------------------------
-- 初始数据：业务（科室 / 医生 / 排班 / 预约）
-- -----------------------------------------------------------------------------

INSERT INTO department (id, name, description, location, sort_order, status) VALUES
(1,  '内科', '呼吸、消化、内分泌等',     '门诊楼3楼', 1,  1),
(2,  '外科', '普外、微创等',           '门诊楼2楼', 2,  1),
(3,  '儿科', '儿童常见病',             '门诊楼2楼', 3,  1),
(4,  '骨科', '骨关节疾病',             '门诊楼4楼', 4,  1),
(5,  '眼科', '视力与眼病',             '门诊楼5楼', 5,  1),
(6,  '口腔', '口腔疾病',               '门诊楼1楼', 6,  1),
(7,  '皮肤', '皮肤病诊疗',             '门诊楼3楼', 7,  1),
(8,  '妇科', '妇科常见病',             '门诊楼3楼', 8,  1),
(9,  '泌尿', '泌尿系统',               '门诊楼2楼', 9,  1),
(10, '传染', '感染性疾病',             '门诊楼1楼', 10, 1),
(11, '康复', '康复理疗',               '门诊楼4楼', 11, 1),
(12, '全科', '综合诊疗',               '门诊楼1楼', 12, 1);

INSERT INTO doctor (id, department_id, name, title, gender, avatar, intro, status) VALUES
(1,  1,  '张明华', '主任医师',   1, '/uploads/common/doctor_m_1.jpg', '擅长呼吸系统疾病诊治，从业20年',     1),
(2,  1,  '李晓芳', '副主任医师', 2, '/uploads/common/doctor_f_1.jpg', '擅长消化内科常见病',               1),
(3,  1,  '周德华', '主任医师',   1, '/uploads/common/doctor_m_2.jpg', '内分泌与代谢疾病诊疗专家',         1),
(4,  2,  '王建国', '主任医师',   1, '/uploads/common/doctor_m_3.jpg', '普外科手术经验丰富',               1),
(5,  2,  '赵志强', '主治医师',   1, '/uploads/common/doctor_m_1.jpg', '擅长微创手术',                     1),
(6,  2,  '马小军', '副主任医师', 1, '/uploads/common/doctor_m_2.jpg', '肝胆胰脾外科，腹腔镜手术',         1),
(7,  3,  '陈美玲', '副主任医师', 2, '/uploads/common/doctor_f_2.jpg', '儿科常见病及过敏诊治',             1),
(8,  3,  '林小燕', '主治医师',   2, '/uploads/common/doctor_f_3.jpg', '小儿呼吸系统疾病',                 1),
(9,  3,  '何俊杰', '主治医师',   1, '/uploads/common/doctor_m_3.jpg', '儿童保健与生长发育',               1),
(10, 4,  '刘德伟', '主任医师',   1, '/uploads/common/doctor_m_1.jpg', '骨关节疾病专家',                   1),
(11, 4,  '郑伟',   '副主任医师', 1, '/uploads/common/doctor_m_2.jpg', '运动损伤与关节镜治疗',             1),
(12, 4,  '黄志强', '主治医师',   1, '/uploads/common/doctor_m_3.jpg', '脊柱外科与颈腰椎病',               1),
(13, 5,  '周文静', '主治医师',   2, '/uploads/common/doctor_f_4.jpg', '白内障及眼底病',                   1),
(14, 5,  '吴晓明', '主任医师',   1, '/uploads/common/doctor_m_1.jpg', '青光眼与角膜病诊治',               1),
(15, 5,  '徐丽华', '主治医师',   2, '/uploads/common/doctor_f_1.jpg', '屈光不正与视觉矫正',               1),
(16, 6,  '杨帆',   '主任医师',   1, '/uploads/common/doctor_m_2.jpg', '口腔颌面外科与种植牙',             1),
(17, 6,  '冯磊',   '副主任医师', 1, '/uploads/common/doctor_m_3.jpg', '牙体牙髓病与根管治疗',             1),
(18, 6,  '曹颖',   '主治医师',   2, '/uploads/common/doctor_f_2.jpg', '牙周病与口腔黏膜病',               1),
(19, 7,  '谢芳',   '副主任医师', 2, '/uploads/common/doctor_f_3.jpg', '湿疹、荨麻疹等过敏性皮肤病',       1),
(20, 7,  '邓建军', '主任医师',   1, '/uploads/common/doctor_m_1.jpg', '银屑病与皮肤肿瘤诊治',             1),
(21, 7,  '韩雪',   '主治医师',   2, '/uploads/common/doctor_f_4.jpg', '痤疮与皮肤美容治疗',               1),
(22, 8,  '罗敏',   '主任医师',   2, '/uploads/common/doctor_f_1.jpg', '妇科肿瘤与微创手术',               1),
(23, 8,  '梁静',   '副主任医师', 2, '/uploads/common/doctor_f_2.jpg', '月经失调与内分泌疾病',             1),
(24, 8,  '宋娜',   '主治医师',   2, '/uploads/common/doctor_f_3.jpg', '孕产期保健与高危妊娠',             1),
(25, 9,  '彭刚',   '主任医师',   1, '/uploads/common/doctor_m_2.jpg', '泌尿系结石与微创碎石',             1),
(26, 9,  '董伟',   '副主任医师', 1, '/uploads/common/doctor_m_3.jpg', '前列腺疾病诊治',                   1),
(27, 9,  '袁莉',   '主治医师',   2, '/uploads/common/doctor_f_4.jpg', '尿路感染与女性泌尿疾病',           1),
(28, 10, '蒋涛',   '主任医师',   1, '/uploads/common/doctor_m_1.jpg', '病毒性肝炎与肝硬化',               1),
(29, 10, '潘虹',   '副主任医师', 2, '/uploads/common/doctor_f_1.jpg', '结核病与呼吸道感染',               1),
(30, 10, '杜明',   '主治医师',   1, '/uploads/common/doctor_m_2.jpg', '发热待查与感染性疾病',             1),
(31, 11, '程远',   '主任医师',   1, '/uploads/common/doctor_m_3.jpg', '脑卒中康复与神经康复',             1),
(32, 11, '傅琳',   '副主任医师', 2, '/uploads/common/doctor_f_2.jpg', '骨关节术后康复训练',               1),
(33, 11, '沈浩',   '主治医师',   1, '/uploads/common/doctor_m_1.jpg', '运动损伤康复与理疗',               1),
(34, 12, '孙海涛', '副主任医师', 1, '/uploads/common/doctor_m_2.jpg', '全科医学，慢性病管理',             1),
(35, 12, '余婷',   '主治医师',   2, '/uploads/common/doctor_f_3.jpg', '常见病多发病的综合诊治',           1),
(36, 12, '谭刚',   '副主任医师', 1, '/uploads/common/doctor_m_3.jpg', '健康评估与双向转诊',               1);

-- -----------------------------------------------------------------------------
-- 初始数据：医生登录账号（用户名 doctor+姓名拼音首字母，默认密码 123）
-- -----------------------------------------------------------------------------

INSERT INTO sys_user (username, password, nickname, email, phone, balance, status, role, doctor_id, openid) VALUES
('doctorzmh', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '张明华', NULL, '13800000001', 0.00, 1, 'DOCTOR', 1, NULL),
('doctorlxf', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '李晓芳', NULL, '13800000002', 0.00, 1, 'DOCTOR', 2, NULL),
('doctorzdh', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '周德华', NULL, '13800000003', 0.00, 1, 'DOCTOR', 3, NULL),
('doctorwjg', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '王建国', NULL, '13800000004', 0.00, 1, 'DOCTOR', 4, NULL),
('doctorzzq', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '赵志强', NULL, '13800000005', 0.00, 1, 'DOCTOR', 5, NULL),
('doctormxj', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '马小军', NULL, '13800000006', 0.00, 1, 'DOCTOR', 6, NULL),
('doctorcml', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '陈美玲', NULL, '13800000007', 0.00, 1, 'DOCTOR', 7, NULL),
('doctorlxy', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '林小燕', NULL, '13800000008', 0.00, 1, 'DOCTOR', 8, NULL),
('doctorhjj', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '何俊杰', NULL, '13800000009', 0.00, 1, 'DOCTOR', 9, NULL),
('doctorldw', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '刘德伟', NULL, '13800000010', 0.00, 1, 'DOCTOR', 10, NULL),
('doctorzw', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '郑伟', NULL, '13800000011', 0.00, 1, 'DOCTOR', 11, NULL),
('doctorhzq', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '黄志强', NULL, '13800000012', 0.00, 1, 'DOCTOR', 12, NULL),
('doctorzwj', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '周文静', NULL, '13800000013', 0.00, 1, 'DOCTOR', 13, NULL),
('doctorwxm', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '吴晓明', NULL, '13800000014', 0.00, 1, 'DOCTOR', 14, NULL),
('doctorxlh', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '徐丽华', NULL, '13800000015', 0.00, 1, 'DOCTOR', 15, NULL),
('doctoryf', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '杨帆', NULL, '13800000016', 0.00, 1, 'DOCTOR', 16, NULL),
('doctorfl', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '冯磊', NULL, '13800000017', 0.00, 1, 'DOCTOR', 17, NULL),
('doctorcy', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '曹颖', NULL, '13800000018', 0.00, 1, 'DOCTOR', 18, NULL),
('doctorxf', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '谢芳', NULL, '13800000019', 0.00, 1, 'DOCTOR', 19, NULL),
('doctordjj', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '邓建军', NULL, '13800000020', 0.00, 1, 'DOCTOR', 20, NULL),
('doctorhx', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '韩雪', NULL, '13800000021', 0.00, 1, 'DOCTOR', 21, NULL),
('doctorlm', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '罗敏', NULL, '13800000022', 0.00, 1, 'DOCTOR', 22, NULL),
('doctorlj', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '梁静', NULL, '13800000023', 0.00, 1, 'DOCTOR', 23, NULL),
('doctorsn', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '宋娜', NULL, '13800000024', 0.00, 1, 'DOCTOR', 24, NULL),
('doctorpg', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '彭刚', NULL, '13800000025', 0.00, 1, 'DOCTOR', 25, NULL),
('doctordw', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '董伟', NULL, '13800000026', 0.00, 1, 'DOCTOR', 26, NULL),
('doctoryl', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '袁莉', NULL, '13800000027', 0.00, 1, 'DOCTOR', 27, NULL),
('doctorjt', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '蒋涛', NULL, '13800000028', 0.00, 1, 'DOCTOR', 28, NULL),
('doctorph', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '潘虹', NULL, '13800000029', 0.00, 1, 'DOCTOR', 29, NULL),
('doctordm', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '杜明', NULL, '13800000030', 0.00, 1, 'DOCTOR', 30, NULL),
('doctorcy31', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '程远', NULL, '13800000031', 0.00, 1, 'DOCTOR', 31, NULL),
('doctorfl32', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '傅琳', NULL, '13800000032', 0.00, 1, 'DOCTOR', 32, NULL),
('doctorsh', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '沈浩', NULL, '13800000033', 0.00, 1, 'DOCTOR', 33, NULL),
('doctorsht', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '孙海涛', NULL, '13800000034', 0.00, 1, 'DOCTOR', 34, NULL),
('doctoryt', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '余婷', NULL, '13800000035', 0.00, 1, 'DOCTOR', 35, NULL),
('doctortg', '$2a$10$dskSVl8JS.wg923Vre3gy.XxM/fh3c0.OoIoETv85PQsJRuaLq30C', '谭刚', NULL, '13800000036', 0.00, 1, 'DOCTOR', 36, NULL);

INSERT INTO schedule (doctor_id, schedule_date, time_slot, total_quota, booked_count, status)
SELECT d.id, DATE_ADD(CURDATE(), INTERVAL day.n DAY), slot.t, 5, 0, 1
FROM doctor d
CROSS JOIN (
    SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
    UNION SELECT 5 UNION SELECT 6 UNION SELECT 7
) day
CROSS JOIN (
    SELECT '08:00' AS t UNION SELECT '09:00' UNION SELECT '10:00'
    UNION SELECT '14:00' UNION SELECT '15:00' UNION SELECT '16:00'
) slot
WHERE d.status = 1;

-- -----------------------------------------------------------------------------
-- 初始数据：药品库
-- -----------------------------------------------------------------------------

INSERT INTO medicine (name, spec, unit, price, stock, usage_desc, status) VALUES
('阿莫西林胶囊', '0.25g*24粒', '盒', 18.50, 200, '口服，遵医嘱', 1),
('布洛芬缓释胶囊', '0.3g*20粒', '盒', 22.00, 150, '口服，饭后服用', 1),
('头孢克肟分散片', '0.1g*6片', '盒', 35.00, 120, '口服，每日2次', 1),
('氯雷他定片', '10mg*6片', '盒', 16.80, 180, '口服，每日1次', 1),
('奥美拉唑肠溶胶囊', '20mg*14粒', '盒', 28.00, 100, '口服，早餐前', 1),
('蒙脱石散', '3g*10袋', '盒', 25.50, 160, '口服，温水冲服', 1),
('复方甘草片', '100片', '瓶', 12.00, 80, '含服，遵医嘱', 1),
('维生素C片', '0.1g*100片', '瓶', 8.50, 300, '口服', 1),
('盐酸氨溴索口服液', '100ml', '瓶', 32.00, 90, '口服，每日3次', 1),
('开塞露', '20ml*10支', '盒', 15.00, 120, '外用', 1),
('碘伏消毒液', '100ml', '瓶', 9.90, 200, '外用消毒', 1),
('板蓝根颗粒', '10g*20袋', '盒', 19.80, 150, '开水冲服', 1),
('藿香正气水', '10ml*10支', '盒', 14.50, 100, '口服', 1),
('对乙酰氨基酚片', '0.5g*24片', '盒', 11.00, 220, '口服，必要时', 1),
('甲硝唑片', '0.2g*100片', '瓶', 13.50, 80, '口服，遵医嘱', 1),
('葡萄糖酸钙口服液', '10ml*12支', '盒', 38.00, 60, '口服', 1),
('炉甘石洗剂', '100ml', '瓶', 12.80, 70, '外用涂抹', 1),
('红霉素软膏', '10g', '支', 6.50, 150, '外用', 1);

-- -----------------------------------------------------------------------------
-- 初始数据：预约演示 - 数据概览（微信用户 id=2，近7日趋势/评价）
-- -----------------------------------------------------------------------------

-- 近7日预约（create_time 分散，多科室多状态）
-- 6天前
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010001', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 6 DAY), 'completed', '数据概览演示', DATE_SUB(NOW(), INTERVAL 6 DAY)
FROM schedule s WHERE s.doctor_id = 1  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY) AND s.time_slot = '08:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010002', 20.00, 'wechat',  DATE_SUB(NOW(), INTERVAL 6 DAY), 'completed', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 6 DAY), INTERVAL 2 HOUR)
FROM schedule s WHERE s.doctor_id = 7  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY) AND s.time_slot = '09:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010003', 20.00, 'alipay',  DATE_SUB(NOW(), INTERVAL 6 DAY), 'cancelled', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 6 DAY), INTERVAL 4 HOUR)
FROM schedule s WHERE s.doctor_id = 10 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY) AND s.time_slot = '10:00' LIMIT 1;

-- 5天前
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010004', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 5 DAY), 'completed', '数据概览演示', DATE_SUB(NOW(), INTERVAL 5 DAY)
FROM schedule s WHERE s.doctor_id = 19 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY) AND s.time_slot = '14:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010005', 20.00, 'wechat',  DATE_SUB(NOW(), INTERVAL 5 DAY), 'completed', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 1 HOUR)
FROM schedule s WHERE s.doctor_id = 4  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 4 DAY) AND s.time_slot = '15:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010006', 25.00, 'balance', DATE_SUB(NOW(), INTERVAL 5 DAY), 'completed', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 3 HOUR)
FROM schedule s WHERE s.doctor_id = 13 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 4 DAY) AND s.time_slot = '16:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010007', 20.00, 'wechat',  DATE_SUB(NOW(), INTERVAL 5 DAY), 'pending',   '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 5 HOUR)
FROM schedule s WHERE s.doctor_id = 2  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 5 DAY) AND s.time_slot = '08:00' LIMIT 1;

-- 4天前
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010008', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 4 DAY), 'completed', '数据概览演示', DATE_SUB(NOW(), INTERVAL 4 DAY)
FROM schedule s WHERE s.doctor_id = 1  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 4 DAY) AND s.time_slot = '09:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010009', 20.00, 'alipay',  DATE_SUB(NOW(), INTERVAL 4 DAY), 'completed', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 4 DAY), INTERVAL 2 HOUR)
FROM schedule s WHERE s.doctor_id = 7  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 5 DAY) AND s.time_slot = '10:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010010', 20.00, 'wechat',  DATE_SUB(NOW(), INTERVAL 4 DAY), 'checked_in', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 4 DAY), INTERVAL 4 HOUR)
FROM schedule s WHERE s.doctor_id = 22 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY) AND s.time_slot = '14:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010011', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 4 DAY), 'cancelled', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 4 DAY), INTERVAL 6 HOUR)
FROM schedule s WHERE s.doctor_id = 10 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 5 DAY) AND s.time_slot = '15:00' LIMIT 1;

-- 3天前
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010012', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 3 DAY), 'completed', '数据概览演示', DATE_SUB(NOW(), INTERVAL 3 DAY)
FROM schedule s WHERE s.doctor_id = 1  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 5 DAY) AND s.time_slot = '08:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010013', 20.00, 'wechat',  DATE_SUB(NOW(), INTERVAL 3 DAY), 'completed', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 1 HOUR)
FROM schedule s WHERE s.doctor_id = 19 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 6 DAY) AND s.time_slot = '09:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010014', 20.00, 'alipay',  DATE_SUB(NOW(), INTERVAL 3 DAY), 'completed', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 3 HOUR)
FROM schedule s WHERE s.doctor_id = 4  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 4 DAY) AND s.time_slot = '10:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010015', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 3 DAY), 'in_consultation', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 5 HOUR)
FROM schedule s WHERE s.doctor_id = 7  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY) AND s.time_slot = '14:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010016', 20.00, 'wechat',  DATE_SUB(NOW(), INTERVAL 3 DAY), 'pending_rx_pay', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 7 HOUR)
FROM schedule s WHERE s.doctor_id = 13 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY) AND s.time_slot = '15:00' LIMIT 1;

-- 2天前
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010017', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 2 DAY), 'completed', '数据概览演示', DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM schedule s WHERE s.doctor_id = 2  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 6 DAY) AND s.time_slot = '08:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010018', 20.00, 'wechat',  DATE_SUB(NOW(), INTERVAL 2 DAY), 'completed', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 2 HOUR)
FROM schedule s WHERE s.doctor_id = 10 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 5 DAY) AND s.time_slot = '09:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010019', 20.00, 'alipay',  DATE_SUB(NOW(), INTERVAL 2 DAY), 'pending',   '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 4 HOUR)
FROM schedule s WHERE s.doctor_id = 22 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 4 DAY) AND s.time_slot = '10:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010020', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 2 DAY), 'confirmed', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 6 HOUR)
FROM schedule s WHERE s.doctor_id = 1  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 7 DAY) AND s.time_slot = '14:00' LIMIT 1;

-- 1天前
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010021', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 1 DAY), 'completed', '数据概览演示', DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM schedule s WHERE s.doctor_id = 19 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 6 DAY) AND s.time_slot = '08:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010022', 20.00, 'wechat',  DATE_SUB(NOW(), INTERVAL 1 DAY), 'completed', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 2 HOUR)
FROM schedule s WHERE s.doctor_id = 4  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 5 DAY) AND s.time_slot = '09:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010023', 25.00, 'alipay',  DATE_SUB(NOW(), INTERVAL 1 DAY), 'completed', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 4 HOUR)
FROM schedule s WHERE s.doctor_id = 13 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 6 DAY) AND s.time_slot = '10:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010024', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 1 DAY), 'checked_in', '数据概览演示', DATE_SUB(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 6 HOUR)
FROM schedule s WHERE s.doctor_id = 7  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY) AND s.time_slot = '14:00' LIMIT 1;

-- 今日
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010025', 20.00, 'balance', NOW(), 'pending',   '数据概览演示', DATE_SUB(NOW(), INTERVAL 3 HOUR)
FROM schedule s WHERE s.doctor_id = 1  AND s.schedule_date = CURDATE() AND s.time_slot = '09:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010026', 20.00, 'wechat',  NOW(), 'pending',   '数据概览演示', DATE_SUB(NOW(), INTERVAL 2 HOUR)
FROM schedule s WHERE s.doctor_id = 2  AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY) AND s.time_slot = '10:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010027', 20.00, 'alipay',  NOW(), 'completed', '数据概览演示', DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM schedule s WHERE s.doctor_id = 10 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY) AND s.time_slot = '14:00' LIMIT 1;
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark, create_time)
SELECT 2, s.id, '青哥哥', '13988010028', 20.00, 'balance', NOW(), 'completed', '数据概览演示', DATE_SUB(NOW(), INTERVAL 30 MINUTE)
FROM schedule s WHERE s.doctor_id = 19 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY) AND s.time_slot = '15:00' LIMIT 1;

-- 医生评价（星级分布 5/4/3/2/1 星）
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 5, '耐心细致,解答清楚', '医生非常专业，态度很好', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010001' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 5, '医术精湛', '治疗效果明显，推荐', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010002' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 5, '服务周到', '就诊流程顺畅', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010004' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 5, '认真负责', '复查安排合理', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010005' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 4, '态度友好', '整体满意，等待略久', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010006' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 4, '专业可靠', '诊断准确', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010008' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 4, '环境整洁', '医生讲解详细', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010009' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 3, '一般', '还可以，希望缩短排队', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010012' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 3, '基本满意', '用药说明清楚', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010013' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 2, '等待时间长', '建议优化叫号', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010017' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 1, '体验欠佳', '预约时间与实际不符', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010018' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 5, '非常满意', '复诊还会选择', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010021' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 4, '值得推荐', '医生很耐心', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010022' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 5, '五星好评', '服务贴心', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010027' LIMIT 1;
INSERT INTO appointment_review (appointment_id, user_id, doctor_id, rating, tags, comment, create_time)
SELECT a.id, 2, s.doctor_id, 4, '不错', '整体体验良好', a.create_time
FROM appointment a JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988010028' LIMIT 1;

-- -----------------------------------------------------------------------------
-- 初始数据：预约演示 - 诊疗闭环（医生工作台案例）
-- -----------------------------------------------------------------------------

-- 1. 王芳 — 已到院，待医生接诊
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '王芳', '13988776601', 20.00, 'balance', NOW(), 'checked_in', '演示案例-已到院'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = CURDATE() AND s.time_slot = '08:00'
LIMIT 1;

-- 2. 刘强 — 就诊中（尚未写病历）
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '刘强', '13988776602', 20.00, 'wechat', NOW(), 'in_consultation', '演示案例-刚接诊'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = CURDATE() AND s.time_slot = '09:00'
LIMIT 1;

-- 3. 陈静 — 就诊中 + 完整病历（待开方或完成）
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '陈静', '13988776603', 20.00, 'balance', NOW(), 'in_consultation', '演示案例-病历已填'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = CURDATE() AND s.time_slot = '10:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '反复咳嗽、咳痰1周，伴低热',
       '1周前受凉后出现咳嗽，初为干咳，后咳黄痰，伴午后低热37.8℃，无胸痛，自服感冒药效果不佳',
       '急性支气管炎',
       '抗感染、止咳化痰、对症支持；注意休息，多饮水',
       '避免受凉，3天后复诊；若高热或呼吸困难及时就诊'
FROM appointment a WHERE a.patient_phone = '13988776603' LIMIT 1;

-- 4. 赵磊 — 待缴药费 + 未支付处方
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '赵磊', '13988776604', 20.00, 'alipay', NOW(), 'pending_rx_pay', '演示案例-待缴药费'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = CURDATE() AND s.time_slot = '14:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '咽痛、吞咽痛2天',
       '2天前起咽痛明显，吞咽时加重，无发热，自行含片缓解不明显',
       '急性咽炎',
       '局部抗炎、对症止痛，必要时含漱',
       '忌辛辣刺激饮食，少说话，多休息'
FROM appointment a WHERE a.patient_phone = '13988776604' LIMIT 1;

INSERT INTO prescription (appointment_id, user_id, doctor_id, total_amount, status)
SELECT a.id, a.user_id, 1, 40.50, 'unpaid'
FROM appointment a WHERE a.patient_phone = '13988776604' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 1, '阿莫西林胶囊', '0.25g*24粒', '0.5g', '每日3次', 5, 1, 18.50, 18.50
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988776604' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 2, '布洛芬缓释胶囊', '0.3g*20粒', '0.3g', '必要时', 3, 1, 22.00, 22.00
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988776604' LIMIT 1;

-- 5. 孙丽 — 处方已付，待取药
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '孙丽', '13988776605', 20.00, 'balance', NOW(), 'pending_rx_pay', '演示案例-待取药'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = CURDATE() AND s.time_slot = '15:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '胃部胀痛、反酸1周',
       '餐后上腹胀痛，伴反酸嗳气，夜间偶有烧心感，大便正常',
       '慢性胃炎伴反流',
       '抑酸护胃，调整饮食习惯',
       '少食多餐，忌咖啡酒精，睡前2小时不进食'
FROM appointment a WHERE a.patient_phone = '13988776605' LIMIT 1;

INSERT INTO prescription (appointment_id, user_id, doctor_id, total_amount, status, pay_method, pay_time)
SELECT a.id, a.user_id, 1, 28.00, 'paid', 'balance', NOW()
FROM appointment a WHERE a.patient_phone = '13988776605' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 5, '奥美拉唑肠溶胶囊', '20mg*14粒', '20mg', '每日1次', 14, 1, 28.00, 28.00
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988776605' LIMIT 1;

-- 6. 周涛 — 已完成（已取药）
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '周涛', '13988776606', 20.00, 'wechat', DATE_SUB(NOW(), INTERVAL 2 DAY), 'completed', '演示案例-已完成'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY) AND s.time_slot = '09:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '皮肤瘙痒、起疹3天',
       '躯干四肢出现红色丘疹，瘙痒明显，无发热，近期进食海鲜',
       '过敏性皮炎',
       '抗过敏、外用止痒，避免过敏原',
       '忌食海鲜坚果，穿棉质衣物，勿搔抓'
FROM appointment a WHERE a.patient_phone = '13988776606' LIMIT 1;

INSERT INTO prescription (appointment_id, user_id, doctor_id, total_amount, status, pay_method, pay_time)
SELECT a.id, a.user_id, 1, 23.30, 'dispensed', 'wechat', DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM appointment a WHERE a.patient_phone = '13988776606' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 4, '氯雷他定片', '10mg*6片', '10mg', '每日1次', 6, 1, 16.80, 16.80
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988776606' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 18, '红霉素软膏', '10g', '适量', '每日2次', 5, 1, 6.50, 6.50
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988776606' LIMIT 1;

-- 7. 吴敏 — 已完成（无需开药）
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '吴敏', '13988776607', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 1 DAY), 'completed', '演示案例-无需开药'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY) AND s.time_slot = '10:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '体检发现血压偏高',
       '无头痛头晕，家族史父亲高血压，近期工作压力大，睡眠欠佳',
       '高血压病1级（高危）',
       '生活方式干预，低盐饮食，监测血压，必要时药物',
       '每日测血压并记录，控制体重，规律运动，1月后复查'
FROM appointment a WHERE a.patient_phone = '13988776607' LIMIT 1;

-- 8. 郑浩 — 就诊中 + 病历（消化系统）
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '郑浩', '13988776608', 20.00, 'alipay', NOW(), 'in_consultation', '演示案例-腹泻'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = CURDATE() AND s.time_slot = '16:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '腹泻、水样便2天',
       '2天前进食不洁食物后出现腹泻，每日5-6次水样便，伴腹部隐痛，无血便',
       '急性肠胃炎',
       '补液、肠道黏膜保护，饮食调理',
       '清淡饮食，忌生冷油腻，注意手卫生，症状加重及时复诊'
FROM appointment a WHERE a.patient_phone = '13988776608' LIMIT 1;

-- 9. 林雪 — 已到院
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '林雪', '13988776609', 20.00, 'balance', NOW(), 'checked_in', '演示案例-感冒初诊'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY) AND s.time_slot = '08:00'
LIMIT 1;

-- 10. 黄斌 — 待缴药费（多药品处方）
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '黄斌', '13988776610', 20.00, 'wechat', NOW(), 'pending_rx_pay', '演示案例-多药处方'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY) AND s.time_slot = '09:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '鼻塞流涕、头痛3天',
       '3天前感冒后出现鼻塞流清涕，伴头痛乏力，无高热，咳嗽少痰',
       '流行性感冒',
       '抗病毒、对症退热，注意休息',
       '多饮水，室内通风，戴口罩避免传染家人'
FROM appointment a WHERE a.patient_phone = '13988776610' LIMIT 1;

INSERT INTO prescription (appointment_id, user_id, doctor_id, total_amount, status)
SELECT a.id, a.user_id, 1, 62.80, 'unpaid'
FROM appointment a WHERE a.patient_phone = '13988776610' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 12, '板蓝根颗粒', '10g*20袋', '1袋', '每日3次', 3, 1, 19.80, 19.80
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988776610' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 14, '对乙酰氨基酚片', '0.5g*24片', '0.5g', '发热时', 3, 1, 11.00, 11.00
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988776610' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 9, '盐酸氨溴索口服液', '100ml', '10ml', '每日3次', 5, 1, 32.00, 32.00
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988776610' LIMIT 1;

-- -----------------------------------------------------------------------------
-- 初始数据：预约演示 - 微信用户已完成就诊
-- -----------------------------------------------------------------------------

INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '青哥哥', '13988000001', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 5 DAY), 'completed', '青用户演示-已完成取药'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 7 DAY) AND s.time_slot = '08:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '发热、咽痛、流涕3天',
       '3天前受凉后出现发热最高38.5℃，伴咽痛流涕，咳嗽少，精神尚可',
       '上呼吸道感染',
       '抗病毒、退热对症，多休息多饮水',
       '体温超过38.5℃可服退热药，3日无好转复诊'
FROM appointment a WHERE a.patient_phone = '13988000001' LIMIT 1;

INSERT INTO prescription (appointment_id, user_id, doctor_id, total_amount, status, pay_method, pay_time)
SELECT a.id, 2, 1, 30.80, 'dispensed', 'balance', DATE_SUB(NOW(), INTERVAL 4 DAY)
FROM appointment a WHERE a.patient_phone = '13988000001' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 12, '板蓝根颗粒', '10g*20袋', '1袋', '每日3次', 3, 1, 19.80, 19.80
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988000001' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 14, '对乙酰氨基酚片', '0.5g*24片', '0.5g', '发热时', 3, 1, 11.00, 11.00
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988000001' LIMIT 1;

-- 2. 急性胃炎
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '青哥哥', '13988000002', 20.00, 'wechat', DATE_SUB(NOW(), INTERVAL 4 DAY), 'completed', '青用户演示-已完成取药'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 7 DAY) AND s.time_slot = '09:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '上腹痛、恶心1天',
       '昨日晚餐后突发上腹隐痛，伴恶心，呕吐1次，无腹泻，无发热',
       '急性胃炎',
       '抑酸护胃，清淡饮食，观察症状变化',
       '忌辛辣油腻，少量多餐，腹痛加重及时就诊'
FROM appointment a WHERE a.patient_phone = '13988000002' LIMIT 1;

INSERT INTO prescription (appointment_id, user_id, doctor_id, total_amount, status, pay_method, pay_time)
SELECT a.id, 2, 1, 28.00, 'dispensed', 'wechat', DATE_SUB(NOW(), INTERVAL 3 DAY)
FROM appointment a WHERE a.patient_phone = '13988000002' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 5, '奥美拉唑肠溶胶囊', '20mg*14粒', '20mg', '每日1次', 7, 1, 28.00, 28.00
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988000002' LIMIT 1;

-- 3. 支气管炎
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '青哥哥', '13988000003', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 3 DAY), 'completed', '青用户演示-已完成取药'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 7 DAY) AND s.time_slot = '10:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '咳嗽咳痰5天',
       '5天前感冒后咳嗽迁延，咳黄痰，夜间加重，无胸闷气促',
       '急性支气管炎',
       '抗感染、化痰止咳，避免受凉',
       '戒烟酒，多饮水，1周后复诊'
FROM appointment a WHERE a.patient_phone = '13988000003' LIMIT 1;

INSERT INTO prescription (appointment_id, user_id, doctor_id, total_amount, status, pay_method, pay_time)
SELECT a.id, 2, 1, 50.50, 'dispensed', 'balance', DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM appointment a WHERE a.patient_phone = '13988000003' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 1, '阿莫西林胶囊', '0.25g*24粒', '0.5g', '每日3次', 5, 1, 18.50, 18.50
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988000003' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 9, '盐酸氨溴索口服液', '100ml', '10ml', '每日3次', 5, 1, 32.00, 32.00
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988000003' LIMIT 1;

-- 4. 过敏性皮炎
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '青哥哥', '13988000004', 20.00, 'alipay', DATE_SUB(NOW(), INTERVAL 2 DAY), 'completed', '青用户演示-已完成取药'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 7 DAY) AND s.time_slot = '14:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '皮肤瘙痒起疹2天',
       '接触新洗衣液后四肢出现红色丘疹，瘙痒明显，无发热',
       '接触性皮炎',
       '抗过敏、外用止痒，避免接触过敏原',
       '暂停使用可疑洗衣液，勿搔抓，皮疹扩散及时复诊'
FROM appointment a WHERE a.patient_phone = '13988000004' LIMIT 1;

INSERT INTO prescription (appointment_id, user_id, doctor_id, total_amount, status, pay_method, pay_time)
SELECT a.id, 2, 1, 23.30, 'dispensed', 'alipay', DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM appointment a WHERE a.patient_phone = '13988000004' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 4, '氯雷他定片', '10mg*6片', '10mg', '每日1次', 6, 1, 16.80, 16.80
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988000004' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 18, '红霉素软膏', '10g', '适量', '每日2次', 5, 1, 6.50, 6.50
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988000004' LIMIT 1;

-- 5. 急性腹泻
INSERT INTO appointment (user_id, schedule_id, patient_name, patient_phone, amount, pay_method, pay_time, status, remark)
SELECT 2, s.id, '青哥哥', '13988000005', 20.00, 'balance', DATE_SUB(NOW(), INTERVAL 1 DAY), 'completed', '青用户演示-已完成取药'
FROM schedule s
WHERE s.doctor_id = 1 AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 7 DAY) AND s.time_slot = '15:00'
LIMIT 1;

INSERT INTO consultation_record (appointment_id, doctor_id, chief_complaint, present_illness, diagnosis, treatment_plan, doctor_advice)
SELECT a.id, 1,
       '腹泻水样便1天',
       '昨日进食冷饮后出现腹泻，每日4-5次水样便，伴腹部隐痛，无血便',
       '急性肠胃炎',
       '肠道黏膜保护、补液，饮食调理',
       '清淡饮食，注意补水，持续高热或血便及时就诊'
FROM appointment a WHERE a.patient_phone = '13988000005' LIMIT 1;

INSERT INTO prescription (appointment_id, user_id, doctor_id, total_amount, status, pay_method, pay_time)
SELECT a.id, 2, 1, 41.00, 'dispensed', 'balance', NOW()
FROM appointment a WHERE a.patient_phone = '13988000005' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 6, '蒙脱石散', '3g*10袋', '1袋', '每日3次', 3, 1, 25.50, 25.50
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988000005' LIMIT 1;

INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, spec, dosage, frequency, days, quantity, unit_price, amount)
SELECT p.id, 13, '藿香正气水', '10ml*10支', '1支', '每日2次', 3, 1, 14.50, 14.50
FROM prescription p JOIN appointment a ON a.id = p.appointment_id WHERE a.patient_phone = '13988000005' LIMIT 1;

-- -----------------------------------------------------------------------------
-- 初始数据：站内消息（微信用户 id=2）
-- -----------------------------------------------------------------------------

INSERT INTO user_notification (user_id, type, title, content, biz_type, biz_id, read_flag, create_time)
SELECT 2, 'appointment_paid', '预约成功',
       CONCAT('您已成功预约', d.name, ' ', doc.name, '，就诊时间 ', s.schedule_date, ' ', s.time_slot, '，请按时到院签到。'),
       'appointment', a.id, 0, DATE_SUB(NOW(), INTERVAL 2 HOUR)
FROM appointment a
JOIN schedule s ON s.id = a.schedule_id
JOIN doctor doc ON doc.id = s.doctor_id
JOIN department d ON d.id = doc.department_id
WHERE a.patient_phone = '13988010026' LIMIT 1;

INSERT INTO user_notification (user_id, type, title, content, biz_type, biz_id, read_flag, create_time)
SELECT 2, 'check_in_success', '签到成功',
       CONCAT('您已到院签到（', s.schedule_date, ' ', s.time_slot, '），请等待医生接诊。'),
       'appointment', a.id, 0, DATE_SUB(NOW(), INTERVAL 90 MINUTE)
FROM appointment a
JOIN schedule s ON s.id = a.schedule_id
WHERE a.patient_phone = '13988776609' LIMIT 1;

INSERT INTO user_notification (user_id, type, title, content, biz_type, biz_id, read_flag, create_time)
SELECT 2, 'consultation_started', '开始接诊',
       CONCAT(doc.name, '医生已开始为您接诊，请配合问诊。'),
       'appointment', a.id, 0, DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM appointment a
JOIN schedule s ON s.id = a.schedule_id
JOIN doctor doc ON doc.id = s.doctor_id
WHERE a.patient_phone = '13988776608' LIMIT 1;

INSERT INTO user_notification (user_id, type, title, content, biz_type, biz_id, read_flag, create_time)
SELECT 2, 'prescription_unpaid', '待缴药费',
       CONCAT('医生已为您开具处方，药费 ¥', p.total_amount, '，请尽快在小程序支付。'),
       'appointment', a.id, 0, DATE_SUB(NOW(), INTERVAL 30 MINUTE)
FROM appointment a
JOIN prescription p ON p.appointment_id = a.id
WHERE a.patient_phone = '13988776610' LIMIT 1;

INSERT INTO user_notification (user_id, type, title, content, biz_type, biz_id, read_flag, create_time)
SELECT 2, 'prescription_paid', '药费支付成功',
       CONCAT('处方药费 ¥', p.total_amount, ' 已支付成功，请前往药房取药，取药后由医院确认。'),
       'appointment', a.id, 0, DATE_SUB(NOW(), INTERVAL 20 MINUTE)
FROM appointment a
JOIN prescription p ON p.appointment_id = a.id
WHERE a.patient_phone = '13988000005' LIMIT 1;

INSERT INTO user_notification (user_id, type, title, content, biz_type, biz_id, read_flag, create_time)
SELECT 2, 'prescription_dispensed', '取药完成',
       '您已完成取药，本次就诊流程结束。欢迎对医生进行评价。',
       'appointment', a.id, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM appointment a WHERE a.patient_phone = '13988000002' LIMIT 1;

INSERT INTO user_notification (user_id, type, title, content, biz_type, biz_id, read_flag, create_time)
SELECT 2, 'appointment_paid', '预约成功',
       CONCAT('您已成功预约', d.name, ' ', doc.name, '，就诊时间 ', s.schedule_date, ' ', s.time_slot, '，请按时到院签到。'),
       'appointment', a.id, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)
FROM appointment a
JOIN schedule s ON s.id = a.schedule_id
JOIN doctor doc ON doc.id = s.doctor_id
JOIN department d ON d.id = doc.department_id
WHERE a.patient_phone = '13988010001' LIMIT 1;

INSERT INTO user_notification (user_id, type, title, content, biz_type, biz_id, read_flag, create_time)
SELECT 2, 'appointment_cancelled', '预约已取消',
       CONCAT('您的预约（', d.name, ' ', s.schedule_date, ' ', s.time_slot, '）已取消。挂号费已退回账户余额。'),
       'appointment', a.id, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)
FROM appointment a
JOIN schedule s ON s.id = a.schedule_id
JOIN doctor doc ON doc.id = s.doctor_id
JOIN department d ON d.id = doc.department_id
WHERE a.patient_phone = '13988010003' LIMIT 1;

-- -----------------------------------------------------------------------------
-- 同步待支付保留期、签到时间和排班已预约数
-- -----------------------------------------------------------------------------

UPDATE appointment
SET payment_deadline = TIMESTAMPADD(MINUTE, 15, create_time), update_time = update_time
WHERE status = 'unpaid' AND payment_deadline IS NULL;

UPDATE appointment
SET check_in_time = COALESCE(check_in_time, update_time),
    queue_joined_at = COALESCE(check_in_time, update_time), update_time = update_time
WHERE status = 'checked_in' AND queue_joined_at IS NULL;

UPDATE schedule s
SET s.booked_count = (
    SELECT COUNT(*) FROM appointment a
    WHERE a.schedule_id = s.id AND a.status <> 'cancelled'
);
