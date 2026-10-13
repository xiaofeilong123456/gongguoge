CREATE DATABASE IF NOT EXISTS gongguoge
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE gongguoge;

CREATE TABLE IF NOT EXISTS t_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  phone       VARCHAR(20)  NULL COMMENT '手机号，验证码通道使用；用户名注册的账号为空',
  username    VARCHAR(32)  NULL COMMENT '用户名，密码通道使用；手机号注册的账号为空',
  password    VARCHAR(100) NULL COMMENT 'BCrypt 加密后的密码，可为空',
  nickname    VARCHAR(50)  NOT NULL DEFAULT '功过格用户',
  avatar      VARCHAR(500) NULL,
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 禁用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_phone (phone),
  UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS t_category (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL DEFAULT 0 COMMENT '所属用户 ID，0 为系统预设',
  type        TINYINT     NOT NULL COMMENT '1 功 2 过',
  name        VARCHAR(30) NOT NULL,
  sort        INT         NOT NULL DEFAULT 0,
  is_system   TINYINT     NOT NULL DEFAULT 0 COMMENT '1 系统预设 0 用户自定义',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_category_user_type (user_id, type)
) ENGINE = InnoDB COMMENT = '功过分类表';

CREATE TABLE IF NOT EXISTS t_merit_record (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  type        TINYINT      NOT NULL COMMENT '1 功 2 过',
  category_id BIGINT       NOT NULL,
  content     VARCHAR(200) NOT NULL,
  record_date DATE         NOT NULL,
  client_id   VARCHAR(64)  NULL COMMENT '前端离线同步幂等 ID',
  deleted     TINYINT      NOT NULL DEFAULT 0,
  deleted_at  DATETIME     NULL COMMENT '回收站删除时间',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_client (user_id, client_id),
  KEY idx_user_date (user_id, record_date),
  KEY idx_user_deleted (user_id, deleted)
) ENGINE = InnoDB COMMENT = '功过记录表';

CREATE TABLE IF NOT EXISTS t_feedback (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  content     VARCHAR(500) NOT NULL,
  contact     VARCHAR(50)  NULL,
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未处理 1 已处理',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_feedback_user (user_id)
) ENGINE = InnoDB COMMENT = '意见反馈表';

CREATE TABLE IF NOT EXISTS t_sms_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  phone         VARCHAR(20)  NOT NULL,
  scene         VARCHAR(20)  NOT NULL COMMENT 'register/login/reset',
  status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待发送 1 已发送 2 失败',
  request_id    VARCHAR(64)  NULL,
  error_message VARCHAR(500) NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sms_phone_create (phone, create_time)
) ENGINE = InnoDB COMMENT = '短信发送日志表';

INSERT INTO t_category (user_id, type, name, sort, is_system, create_time, update_time)
SELECT 0, 1, '自律', 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_category WHERE user_id = 0 AND type = 1 AND name = '自律');

INSERT INTO t_category (user_id, type, name, sort, is_system, create_time, update_time)
SELECT 0, 1, '学习', 2, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_category WHERE user_id = 0 AND type = 1 AND name = '学习');

INSERT INTO t_category (user_id, type, name, sort, is_system, create_time, update_time)
SELECT 0, 1, '健康', 3, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_category WHERE user_id = 0 AND type = 1 AND name = '健康');

INSERT INTO t_category (user_id, type, name, sort, is_system, create_time, update_time)
SELECT 0, 1, '人际', 4, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_category WHERE user_id = 0 AND type = 1 AND name = '人际');

INSERT INTO t_category (user_id, type, name, sort, is_system, create_time, update_time)
SELECT 0, 1, '习惯', 5, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_category WHERE user_id = 0 AND type = 1 AND name = '习惯');

INSERT INTO t_category (user_id, type, name, sort, is_system, create_time, update_time)
SELECT 0, 2, '自律', 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_category WHERE user_id = 0 AND type = 2 AND name = '自律');

INSERT INTO t_category (user_id, type, name, sort, is_system, create_time, update_time)
SELECT 0, 2, '学习', 2, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_category WHERE user_id = 0 AND type = 2 AND name = '学习');

INSERT INTO t_category (user_id, type, name, sort, is_system, create_time, update_time)
SELECT 0, 2, '健康', 3, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_category WHERE user_id = 0 AND type = 2 AND name = '健康');

INSERT INTO t_category (user_id, type, name, sort, is_system, create_time, update_time)
SELECT 0, 2, '人际', 4, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_category WHERE user_id = 0 AND type = 2 AND name = '人际');

INSERT INTO t_category (user_id, type, name, sort, is_system, create_time, update_time)
SELECT 0, 2, '习惯', 5, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_category WHERE user_id = 0 AND type = 2 AND name = '习惯');
