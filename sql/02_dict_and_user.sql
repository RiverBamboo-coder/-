-- =====================================================================
-- 02_dict_and_user.sql · 字典与用户初始数据（A-6 交付物）
-- 数据库：MySQL 8.0 / bri_pm
-- 执行顺序：01_schema.sql 之后执行
-- 执行方式：
--     mysql -uroot -p --default-character-set=utf8mb4 < sql\02_dict_and_user.sql
-- 或 Workbench：File → Open SQL Script → 选中本文件 → ⚡ 执行
--
-- 内容：10 类字典、46 个字典项、5 个演示用户（口令统一 123456，存 SHA-256）
-- 特性：脚本可重复执行（先删后插），主键固定，便于 03/04 引用
-- =====================================================================

USE bri_pm;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0. 可重复执行：清空后重插（没有外键指向字典表，直接删即可）
-- ---------------------------------------------------------------------
DELETE FROM t_dict_item;
DELETE FROM t_dict_type;
DELETE FROM t_user;

ALTER TABLE t_dict_type AUTO_INCREMENT = 1;
ALTER TABLE t_dict_item AUTO_INCREMENT = 1;
ALTER TABLE t_user      AUTO_INCREMENT = 1;

-- ---------------------------------------------------------------------
-- 1. 字典类型（10 类）
--    编码约定（全组统一，B/C/D 都按这个取值）：
--    · 存“英文编码”的：FIELD / MODE / CURRENCY / ENTERPRISE_NATURE / ROLE
--      —— 这些列在业务表里存英文码，例如 t_project.field_code = 'TRANS'
--    · 存“中文值”的：PROJECT_STATUS / CONTRACT_TYPE / COMMIT_TYPE /
--      FUND_SOURCE_TYPE / WARNING_LEVEL
--      —— 因为任务书示例 SQL 直接用中文字面量比较，如 WHERE status = '在建'
-- ---------------------------------------------------------------------
INSERT INTO t_dict_type (dict_type_id, type_code, type_name, remark) VALUES
(1,  'FIELD',             '领域',     '项目所属基础设施领域，t_project.field_code 取值'),
(2,  'MODE',              '合作模式', 't_project.mode_code 取值'),
(3,  'CURRENCY',          '币种',     '金额类字段的币种，CHAR(3)'),
(4,  'ENTERPRISE_NATURE', '企业性质', 't_enterprise.nature_code 取值'),
(5,  'ROLE',              '参与角色', 't_project_participant.role_code 取值'),
(6,  'PROJECT_STATUS',    '项目状态', 't_project.status / t_project_status_log.to_status 取值'),
(7,  'CONTRACT_TYPE',     '合同类型', 't_contract.contract_type 取值'),
(8,  'FUND_SOURCE_TYPE',  '资金来源类型', 't_funding_source.source_type 取值'),
(9,  'COMMIT_TYPE',       '承诺类型', 't_commitment.commit_type 取值'),
(10, 'WARNING_LEVEL',     '预警级别', 't_warning_rule.level_code / t_warning.level_code 取值');

-- ---------------------------------------------------------------------
-- 2. 字典项（46 条）
-- ---------------------------------------------------------------------

-- 2.1 领域 FIELD（6）
INSERT INTO t_dict_item (dict_type_id, item_code, item_name, sort_no, enabled) VALUES
(1, 'TRANS',     '交通基础设施', 1, 1),
(1, 'ENERGY',    '能源电力',     2, 1),
(1, 'COMM',      '通信信息',     3, 1),
(1, 'WATER',     '水利水务',     4, 1),
(1, 'LIVELIHOOD','民生改善',     5, 1),
(1, 'PARK',      '产业园区',     6, 1);

-- 2.2 合作模式 MODE（5）
INSERT INTO t_dict_item (dict_type_id, item_code, item_name, sort_no, enabled) VALUES
(2, 'AID',        '援建',             1, 1),
(2, 'EPC',        '工程总承包',       2, 1),
(2, 'PPP',        '政府和社会资本合作', 3, 1),
(2, 'BOT',        '建设-运营-移交',   4, 1),
(2, 'CONSORTIUM', '联合体承建',       5, 1);

-- 2.3 币种 CURRENCY（4）
INSERT INTO t_dict_item (dict_type_id, item_code, item_name, sort_no, enabled) VALUES
(3, 'CNY', '人民币',   1, 1),
(3, 'USD', '美元',     2, 1),
(3, 'EUR', '欧元',     3, 1),
(3, 'OTH', '其他币种', 4, 1);

-- 2.4 企业性质 ENTERPRISE_NATURE（5）
INSERT INTO t_dict_item (dict_type_id, item_code, item_name, sort_no, enabled) VALUES
(4, 'CENTER',        '中央企业', 1, 1),
(4, 'LOCAL_SOE',     '地方国有企业', 2, 1),
(4, 'PRIVATE',       '民营企业', 3, 1),
(4, 'FOREIGN_OWNER', '外方业主', 4, 1),
(4, 'FINANCE',       '金融机构', 5, 1);

-- 2.5 参与角色 ROLE（5）
INSERT INTO t_dict_item (dict_type_id, item_code, item_name, sort_no, enabled) VALUES
(5, 'ZC', '总承包',     1, 1),
(5, 'LH', '联合体成员', 2, 1),
(5, 'FB', '分包',       3, 1),
(5, 'SJ', '设计',       4, 1),
(5, 'JL', '监理',       5, 1);

-- 2.6 项目状态 PROJECT_STATUS（5，存中文）
INSERT INTO t_dict_item (dict_type_id, item_code, item_name, sort_no, enabled) VALUES
(6, '前期',   '前期（立项与合作界定）', 1, 1),
(6, '在建',   '在建（施工中）',         2, 1),
(6, '竣工',   '竣工（待移交）',         3, 1),
(6, '已移交', '已移交（进入运营）',     4, 1),
(6, '暂停',   '暂停（缓建）',           5, 1);

-- 2.7 合同类型 CONTRACT_TYPE（4，存中文）
INSERT INTO t_dict_item (dict_type_id, item_code, item_name, sort_no, enabled) VALUES
(7, '设计', '设计合同', 1, 1),
(7, '施工', '施工合同', 2, 1),
(7, '设备', '设备采购合同', 3, 1),
(7, '监理', '监理合同', 4, 1);

-- 2.8 资金来源类型 FUND_SOURCE_TYPE（5，存中文）
--     与 B 段 t_funding_source.source_type 的取值、以及 01_schema.sql 的 ck_source_type 完全一致
INSERT INTO t_dict_item (dict_type_id, item_code, item_name, sort_no, enabled) VALUES
(8, '政府援助',   '政府援助（无偿援助/成套项目）', 1, 1),
(8, '政策性银行', '政策性银行（国开行/进出口行）', 2, 1),
(8, '商业贷款',   '商业贷款（商业银行项目贷款）',   3, 1),
(8, '企业自筹',   '企业自筹资金',                 4, 1),
(8, '多边机构',   '多边开发机构（世行/亚投行）',   5, 1);

-- 2.9 承诺类型 COMMIT_TYPE（4，存中文）
INSERT INTO t_dict_item (dict_type_id, item_code, item_name, sort_no, enabled) VALUES
(9, '工期',     '工期承诺（天数）',     1, 1),
(9, '金额',     '金额承诺（元）',       2, 1),
(9, '属地用工', '属地用工比例（%）',   3, 1),
(9, '培训',     '培训承诺（人次）',     4, 1);

-- 2.10 预警级别 WARNING_LEVEL（3，存单字母码）
--     与 B 段 t_warning_rule.level_code / t_warning.warn_level 的取值（R/Y/B）一致
INSERT INTO t_dict_item (dict_type_id, item_code, item_name, sort_no, enabled) VALUES
(10, 'R', '红灯（严重）', 1, 1),
(10, 'Y', '黄灯（关注）', 2, 1),
(10, 'B', '蓝灯（提示）', 3, 1);

-- ---------------------------------------------------------------------
-- 3. 演示用户（5 个，口令统一 123456）
--    password_hash 存 SHA2(明文, 256) 的十六进制，登录校验用同一函数：
--      SELECT user_id, real_name FROM t_user
--       WHERE username = ? AND password_hash = SHA2(?, 256) AND enabled = 1;
--    生产环境应加盐（bcrypt/PBKDF2），课程项目用 SHA-256 足够讲清楚“不存明文”。
-- ---------------------------------------------------------------------
INSERT INTO t_user (user_id, username, password_hash, real_name, role_code, enabled) VALUES
(1, 'admin',   SHA2('123456', 256), '系统管理员', 'ADMIN',   1),
(2, 'planner', SHA2('123456', 256), '项目专员',   'PLANNER', 1),
(3, 'fund',    SHA2('123456', 256), '资金管理员', 'FUND',    1),
(4, 'field',   SHA2('123456', 256), '现场进度员', 'FIELD',   1),
(5, 'viewer',  SHA2('123456', 256), '项目主管',   'VIEWER',  1);

-- ---------------------------------------------------------------------
-- 4. 自检（可选，手动执行）
--   SELECT COUNT(*) FROM t_dict_type;   -- 期望 10
--   SELECT COUNT(*) FROM t_dict_item;   -- 期望 46
--   SELECT COUNT(*) FROM t_user;        -- 期望 5
--   SELECT t.type_code, t.type_name, COUNT(i.item_id) AS item_cnt
--     FROM t_dict_type t LEFT JOIN t_dict_item i ON i.dict_type_id = t.dict_type_id
--    GROUP BY t.dict_type_id ORDER BY t.dict_type_id;
--   SELECT username, SHA2('123456', 256) = password_hash AS pwd_ok FROM t_user;
-- ---------------------------------------------------------------------
