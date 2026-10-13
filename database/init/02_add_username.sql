-- ============================================================
-- 迁移 02：新增「用户名 + 密码」账号通道
--
-- 背景：手机号验证码通道要求用户必须有手机号，且每条短信都产生费用。
--       申请软著 / App 备案时需要向审核方提供测试账号，不能使用开发者本人手机号，
--       因此增加一条与手机号无关的用户名通道。
--
-- 设计要点：
--   1. username 唯一且可空 —— 手机号注册的老账号 username 为 NULL，不受影响；
--   2. phone 改为可空 —— 用户名注册的账号没有手机号；
--      手机号通道的账号仍然唯一（MySQL 唯一索引允许多个 NULL，正好符合需要）。
--
-- 本脚本可重复执行（幂等）。
-- 执行：mysql -uroot -p'密码' gongguoge < 02_add_username.sql
-- ============================================================

USE gongguoge;

-- 1) phone 允许为空（用户名账号没有手机号）
ALTER TABLE t_user
  MODIFY COLUMN phone VARCHAR(20) NULL COMMENT '手机号，验证码通道使用；用户名注册的账号为空';

-- 2) 新增 username 列（已存在则跳过）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'username'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE t_user ADD COLUMN username VARCHAR(32) NULL COMMENT ''用户名，密码通道使用；手机号注册的账号为空'' AFTER phone',
  'SELECT ''username 列已存在，跳过'' AS msg'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3) 新增 username 唯一索引（已存在则跳过）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND INDEX_NAME = 'uk_user_username'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE t_user ADD UNIQUE KEY uk_user_username (username)',
  'SELECT ''uk_user_username 索引已存在，跳过'' AS msg'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 4) 校验
SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user'
ORDER BY ORDINAL_POSITION;
