-- ============================================================
-- SmartTicket 智工单 · 数据库 schema（ARCHITECTURE §4）
-- 字符集 utf8mb4；逻辑删除 deleted；乐观锁 version；时间字段自动填充
-- ============================================================
CREATE DATABASE IF NOT EXISTS smart_ticket DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE smart_ticket;

-- ---------- RBAC ----------
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(32)  NOT NULL COMMENT '角色名',
    code        VARCHAR(32)  NOT NULL COMMENT '角色码 EMPLOYEE/ENGINEER/ADMIN',
    create_time DATETIME     NULL,
    update_time DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE = InnoDB COMMENT = '角色';

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    real_name   VARCHAR(32)  NULL COMMENT '姓名',
    dept        VARCHAR(64)  NULL COMMENT '部门',
    role_id     BIGINT       NOT NULL COMMENT '角色',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0停用',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NULL,
    update_time DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT = '用户';

CREATE TABLE IF NOT EXISTS sys_permission (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    perm_code   VARCHAR(64)  NOT NULL COMMENT '权限码',
    uri         VARCHAR(128) NULL COMMENT '接口 URI（支持 Ant 通配）',
    create_time DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code)
) ENGINE = InnoDB COMMENT = '权限';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE = InnoDB COMMENT = '角色-权限';

-- ---------- 工单 ----------
CREATE TABLE IF NOT EXISTS ticket (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    ticket_no    VARCHAR(32)  NOT NULL COMMENT '工单号',
    title        VARCHAR(128) NOT NULL,
    content      TEXT         NULL COMMENT '问题描述',
    image_url    VARCHAR(512) NULL COMMENT '图片 URL（逗号分隔）',
    category     VARCHAR(16)  NULL COMMENT 'NETWORK/HARDWARE/ACCOUNT/SOFTWARE/LOGISTICS',
    priority     VARCHAR(8)   NULL COMMENT 'HIGH/MID/LOW',
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/ACCEPTING/DONE/RATED',
    creator_id   BIGINT       NOT NULL COMMENT '报修人',
    assignee_id  BIGINT       NULL COMMENT '处理工程师',
    version      INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
    sla_deadline DATETIME     NULL COMMENT 'SLA 截止',
    sla_overdue  TINYINT      NOT NULL DEFAULT 0 COMMENT 'SLA 是否已超时',
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     NULL,
    update_time  DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_creator (creator_id),
    KEY idx_assignee (assignee_id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '工单';

CREATE TABLE IF NOT EXISTS ticket_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    ticket_id   BIGINT       NOT NULL,
    action      VARCHAR(32)  NOT NULL COMMENT '动作：提交/派单/接单/完成/驳回/转派/评价',
    operator_id BIGINT       NULL,
    remark      VARCHAR(255) NULL,
    create_time DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_ticket (ticket_id)
) ENGINE = InnoDB COMMENT = '工单时间线/审计';

CREATE TABLE IF NOT EXISTS ticket_solution (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id     BIGINT NOT NULL,
    engineer_id   BIGINT NOT NULL,
    solution_text TEXT   NULL,
    image_url     VARCHAR(512) NULL,
    create_time   DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_ticket (ticket_id)
) ENGINE = InnoDB COMMENT = '解决方案';

CREATE TABLE IF NOT EXISTS evaluation (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    ticket_id   BIGINT      NOT NULL,
    score       TINYINT     NOT NULL COMMENT '1-5 星',
    tags        VARCHAR(128) NULL COMMENT '标签，逗号分隔',
    comment     VARCHAR(512) NULL,
    create_time DATETIME    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket (ticket_id)
) ENGINE = InnoDB COMMENT = '评价';

CREATE TABLE IF NOT EXISTS engineer_profile (
    user_id         BIGINT      NOT NULL COMMENT '工程师 user id',
    category_skills VARCHAR(128) NULL COMMENT '技能类别，逗号分隔',
    current_load    INT         NOT NULL DEFAULT 0 COMMENT '当前负载',
    PRIMARY KEY (user_id)
) ENGINE = InnoDB COMMENT = '工程师档案';

-- ---------- 全局审计 ----------
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    operator_id BIGINT       NULL,
    role        VARCHAR(16)  NULL,
    action_type VARCHAR(32)  NOT NULL COMMENT '登录/派单/状态流转/评价/用户管理/权限变更',
    target      VARCHAR(255) NULL COMMENT '操作对象',
    ip          VARCHAR(64)  NULL,
    result      VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAIL',
    create_time DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_action_type (action_type),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB COMMENT = '全局操作审计';

-- ============================================================
-- 初始化 RBAC 角色与权限（用户在 Phase 1 用真实 BCrypt 密文插入）
-- ============================================================
INSERT INTO sys_role (id, role_name, code, create_time, update_time) VALUES
    (1, '报修人', 'EMPLOYEE', NOW(), NOW()),
    (2, '工程师', 'ENGINEER', NOW(), NOW()),
    (3, '管理员', 'ADMIN', NOW(), NOW())
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO sys_permission (id, perm_code, uri, create_time) VALUES
    (1,  'ticket:submit',   '/api/tickets/submit',     NOW()),
    (2,  'ticket:my',       '/api/tickets/my',         NOW()),
    (3,  'ticket:detail',   '/api/tickets/*',          NOW()),
    (4,  'ticket:todo',     '/api/tickets/todo',       NOW()),
    (5,  'ticket:handle',   '/api/tickets/*/handle/**',NOW()),
    (6,  'ticket:evaluate', '/api/evaluations/**',     NOW()),
    (7,  'dispatch:manage', '/api/dispatch/**',        NOW()),
    (8,  'stats:board',     '/api/stats/**',           NOW()),
    (9,  'user:manage',     '/api/users/**',           NOW()),
    (10, 'audit:query',     '/api/audit/**',           NOW())
ON DUPLICATE KEY UPDATE perm_code = VALUES(perm_code);

-- 报修人：提交/我的/详情/评价
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
    (1,1),(1,2),(1,3),(1,6)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
-- 工程师：待办/详情/处理
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
    (2,3),(2,4),(2,5),(2,8)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
-- 管理员：全部
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
    (3,1),(3,2),(3,3),(3,4),(3,5),(3,6),(3,7),(3,8),(3,9),(3,10)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
