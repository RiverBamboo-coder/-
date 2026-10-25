-- =====================================================================
-- 01_schema.sql · 建库建表（A-3 交付物，G1 闸口）
-- 项目：“一带一路”沿线国家基础设施合作项目管理信息系统（bri-pm）
-- 数据库：MySQL 8.0 / InnoDB / utf8mb4
-- 负责人：A（全组唯一的建表脚本；B、C 的字段需求由 A 统一并入本文件）
--
-- 执行方式（Windows cmd）：
--     mysql -uroot -p --default-character-set=utf8mb4 < sql\01_schema.sql
-- 或在 mysql 客户端内：
--     source D:/数据库三级项目/bri-pm/sql/01_schema.sql;
--
-- 脚本特性：
--   1) 可重复执行：先按反序 DROP TABLE，再按外键依赖顺序 CREATE，不会残留旧结构；
--   2) 共 24 张表 = A 负责 12 张（M0/M1/M2/M3）+ B 负责 7 张（M4/M6/M8）+ C 负责 5 张（M5/M7/M9）；
--      （任务书 1.3 的 14 个编号项是“第一批核心表”，第 3 项含两张字典表；
--        其余 B/C 的表不建出来，B/C 第 3 周无法写 DAO，故一并落库）
--   3) 每个外键列都有索引；高频筛选列（status、sign_date、disb_date、period_ym）另建索引；
--   4) 约束命名：fk_表_被引用表 / uk_表_语义 / ck_表_语义（答辩 SHOW CREATE TABLE 逐个看）；
--   5) M5 的超期判定用 MySQL 8 生成列 is_delayed，Java 只读不算（项目亮点）。
--
-- 视图不在本文件（避免 B/C 合并冲突）：
--   B 写 sql/05_views_fund.sql        → v_fund_rate、v_fund_progress
--   C 写 sql/06_views_commitment.sql  → v_commitment_actual
-- =====================================================================

CREATE DATABASE IF NOT EXISTS bri_pm
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE bri_pm;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0. 可重复执行：按反序删表（关外键检查，避免删表顺序影响）
-- ---------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS t_project_outcome;
DROP TABLE IF EXISTS t_warning;
DROP TABLE IF EXISTS t_warning_rule;
DROP TABLE IF EXISTS t_commitment_result;
DROP TABLE IF EXISTS t_commitment;
DROP TABLE IF EXISTS t_report_indicator;
DROP TABLE IF EXISTS t_progress_report;
DROP TABLE IF EXISTS t_project_milestone;
DROP TABLE IF EXISTS t_milestone;
DROP TABLE IF EXISTS t_fund_disbursement;
DROP TABLE IF EXISTS t_fund_plan;
DROP TABLE IF EXISTS t_funding_source;
DROP TABLE IF EXISTS t_project_participant;
DROP TABLE IF EXISTS t_project_status_log;
DROP TABLE IF EXISTS t_contract;
DROP TABLE IF EXISTS t_project;
DROP TABLE IF EXISTS t_consortium_member;
DROP TABLE IF EXISTS t_consortium;
DROP TABLE IF EXISTS t_enterprise;
DROP TABLE IF EXISTS t_user;
DROP TABLE IF EXISTS t_dict_item;
DROP TABLE IF EXISTS t_dict_type;
DROP TABLE IF EXISTS t_country;
DROP TABLE IF EXISTS t_region;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- M0 基础数据（A）：区域、国家、字典、用户
-- 建表顺序即外键依赖顺序，不能调换
-- =====================================================================

-- 1. 区域：洲 → 区域 两级，自关联（递归联系）
CREATE TABLE t_region (
    region_id   INT         NOT NULL AUTO_INCREMENT COMMENT '区域主键',
    region_code VARCHAR(10) NOT NULL COMMENT '区域编码，如 AS、AF、EU、LATAM、OC、NA',
    region_name VARCHAR(50) NOT NULL COMMENT '区域名称：一级为洲，二级为东亚/东南亚等',
    parent_id   INT         NULL     COMMENT '上级区域；为空表示一级（洲）',
    level_no    TINYINT     NOT NULL DEFAULT 1 COMMENT '层级：1=洲，2=区域',
    PRIMARY KEY (region_id),
    UNIQUE KEY uk_region_code (region_code),
    KEY idx_region_parent (parent_id),
    CONSTRAINT fk_region_parent FOREIGN KEY (parent_id) REFERENCES t_region (region_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_region_level CHECK (level_no IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='区域：洲到区域两级，自关联递归';

-- 2. 国家（东道国）：ISO 三字母代码作主键
CREATE TABLE t_country (
    country_code    CHAR(3)      NOT NULL COMMENT 'ISO 3166-1 三字母代码，如 KEN、IDN',
    country_name    VARCHAR(100) NOT NULL COMMENT '中文国名',
    country_name_en VARCHAR(100) NULL     COMMENT '英文国名，可为空',
    region_id       INT          NOT NULL COMMENT '所属二级区域（其 parent 为洲）',
    is_belt_road    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否共建国家：1=是，0=否',
    population_k    DECIMAL(12,2) NULL    COMMENT '人口（万人），可为空',
    PRIMARY KEY (country_code),
    KEY idx_country_region (region_id),
    KEY idx_country_belt (is_belt_road),
    CONSTRAINT fk_country_region FOREIGN KEY (region_id) REFERENCES t_region (region_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_country_belt CHECK (is_belt_road IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='国家（东道国）：国家到项目 1:N 的 1 端';

-- 3. 字典类型：避免把中文枚举写死在 Java 和表结构里
CREATE TABLE t_dict_type (
    dict_type_id INT         NOT NULL AUTO_INCREMENT COMMENT '字典类型主键',
    type_code    VARCHAR(30) NOT NULL COMMENT '类型编码：FIELD/MODE/CURRENCY/ENTERPRISE_NATURE/PROJECT_STATUS/ROLE',
    type_name    VARCHAR(50) NOT NULL COMMENT '类型名称：领域/合作模式/币种/企业性质/项目状态/角色',
    remark       VARCHAR(200) NULL    COMMENT '备注',
    PRIMARY KEY (dict_type_id),
    UNIQUE KEY uk_dict_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型：全组下拉框的数据源';

-- 4. 字典项：字典类型到字典项 1:N
CREATE TABLE t_dict_item (
    item_id      INT         NOT NULL AUTO_INCREMENT COMMENT '字典项主键',
    dict_type_id INT         NOT NULL COMMENT '所属字典类型',
    item_code    VARCHAR(30) NOT NULL COMMENT '项编码，如 TRANS、EPC、CNY',
    item_name    VARCHAR(50) NOT NULL COMMENT '项名称，如 交通、工程总承包、人民币',
    sort_no      INT         NOT NULL DEFAULT 0 COMMENT '显示顺序',
    enabled      TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用：1=是，0=否',
    PRIMARY KEY (item_id),
    UNIQUE KEY uk_dict_item_type_code (dict_type_id, item_code),
    CONSTRAINT fk_dict_item_type FOREIGN KEY (dict_type_id) REFERENCES t_dict_type (dict_type_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_dict_item_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典项：同一类型下编码唯一';

-- 5. 系统用户：口令只存哈希
CREATE TABLE t_user (
    user_id       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    username      VARCHAR(50) NOT NULL COMMENT '登录名',
    password_hash CHAR(64)    NOT NULL COMMENT 'SHA-256 十六进制哈希，禁止存明文',
    real_name     VARCHAR(50) NULL     COMMENT '姓名',
    role_code     VARCHAR(20) NOT NULL COMMENT '角色：ADMIN/PLANNER/FUND/FIELD/VIEWER',
    enabled       TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用：1=是，0=否',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_username (username),
    CONSTRAINT ck_user_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户：角色只在界面层使用，不做权限体系';

-- =====================================================================
-- M3 参与企业与组织（A）：企业、联合体、联合体成员、参与关系
-- 注意顺序：企业要先于联合体；联合体成员表在两个父表之后
-- =====================================================================

-- 6. 企业：中方承建 / 外方业主 / 金融机构；parent_enterprise_id 是自关联递归
CREATE TABLE t_enterprise (
    enterprise_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '企业主键',
    enterprise_name      VARCHAR(150) NOT NULL COMMENT '企业名称',
    enterprise_name_en   VARCHAR(150) NULL     COMMENT '英文名称',
    nature_code          VARCHAR(30)  NOT NULL COMMENT '性质（字典 ENTERPRISE_NATURE）：CENTER/LOCAL_SOE/PRIVATE/FOREIGN_OWNER/FINANCE',
    country_code         CHAR(3)      NOT NULL COMMENT '注册地国别',
    parent_enterprise_id BIGINT       NULL     COMMENT '上级企业（母子公司/总分包递归联系），为空表示顶层',
    credit_code          VARCHAR(30)  NULL     COMMENT '统一社会信用代码，可为空',
    remark               VARCHAR(200) NULL     COMMENT '备注',
    PRIMARY KEY (enterprise_id),
    KEY idx_enterprise_country (country_code),
    KEY idx_enterprise_parent (parent_enterprise_id),
    KEY idx_enterprise_nature (nature_code),
    CONSTRAINT fk_enterprise_country FOREIGN KEY (country_code) REFERENCES t_country (country_code)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_enterprise_parent FOREIGN KEY (parent_enterprise_id) REFERENCES t_enterprise (enterprise_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业：企业到企业自关联递归（母子公司/总分包）';

-- 7. 联合体：牵头单位是外键（M3 递归联系的“牵头”边）
CREATE TABLE t_consortium (
    consortium_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '联合体主键',
    consortium_name      VARCHAR(150) NOT NULL COMMENT '联合体名称',
    leader_enterprise_id BIGINT       NOT NULL COMMENT '牵头单位（企业）',
    country_code         CHAR(3)      NULL     COMMENT '联合体注册国别，可为空',
    establish_date       DATE         NULL     COMMENT '成立日期',
    remark               VARCHAR(200) NULL     COMMENT '备注',
    PRIMARY KEY (consortium_id),
    UNIQUE KEY uk_consortium_name (consortium_name),
    KEY idx_consortium_leader (leader_enterprise_id),
    KEY idx_consortium_country (country_code),
    CONSTRAINT fk_consortium_leader FOREIGN KEY (leader_enterprise_id) REFERENCES t_enterprise (enterprise_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_consortium_country FOREIGN KEY (country_code) REFERENCES t_country (country_code)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='联合体：牵头单位外键 + 成员表构成“牵头/参与”两条边';

-- 8. 联合体成员：企业到联合体 M:N，带份额属性
CREATE TABLE t_consortium_member (
    consortium_id BIGINT       NOT NULL COMMENT '联合体',
    enterprise_id BIGINT       NOT NULL COMMENT '成员企业',
    share_pct     DECIMAL(5,2) NULL     COMMENT '成员份额百分比，可为空',
    join_date     DATE         NULL     COMMENT '加入日期',
    PRIMARY KEY (consortium_id, enterprise_id),
    KEY idx_cm_enterprise (enterprise_id),
    CONSTRAINT fk_cm_consortium FOREIGN KEY (consortium_id) REFERENCES t_consortium (consortium_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cm_enterprise FOREIGN KEY (enterprise_id) REFERENCES t_enterprise (enterprise_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_cm_share CHECK (share_pct IS NULL OR (share_pct >= 0 AND share_pct <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='联合体成员：复合主键保证同一企业在一个联合体里只有一条';

-- =====================================================================
-- M2 项目主数据（A）：项目、合同、状态轨迹
-- =====================================================================

-- 9. 项目：核心主表
CREATE TABLE t_project (
    project_id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '项目主键',
    project_code        VARCHAR(20)   NOT NULL COMMENT '项目编码，业务唯一',
    project_name        VARCHAR(200)  NOT NULL COMMENT '项目名称',
    country_code        CHAR(3)       NOT NULL COMMENT '东道国',
    owner_enterprise_id BIGINT        NULL     COMMENT '东道国业主企业，可为空',
    field_code          VARCHAR(20)   NOT NULL COMMENT '领域（字典 FIELD）：TRANS/ENERGY/COMM/WATER/LIVELIHOOD/PARK',
    mode_code           VARCHAR(20)   NOT NULL COMMENT '合作模式（字典 MODE）：AID/EPC/PPP/BOT/CONSORTIUM',
    status              VARCHAR(10)   NOT NULL COMMENT '状态（字典 PROJECT_STATUS）：前期/在建/竣工/已移交/暂停',
    invest_amount       DECIMAL(18,2) NOT NULL COMMENT '总投资（元）',
    currency_code       CHAR(3)       NOT NULL DEFAULT 'CNY' COMMENT '币种（字典 CURRENCY）',
    sign_date           DATE          NULL     COMMENT '签约日期',
    planned_start_date  DATE          NULL     COMMENT '计划开工日期',
    planned_end_date    DATE          NULL     COMMENT '计划竣工日期',
    actual_start_date   DATE          NULL     COMMENT '实际开工日期',
    actual_end_date     DATE          NULL     COMMENT '实际竣工日期',
    remark              VARCHAR(500)  NULL     COMMENT '备注',
    PRIMARY KEY (project_id),
    UNIQUE KEY uk_project_code (project_code),
    KEY idx_project_country (country_code),
    KEY idx_project_owner (owner_enterprise_id),
    KEY idx_project_status (status),
    KEY idx_project_sign_date (sign_date),
    KEY idx_project_field (field_code),
    KEY idx_project_mode (mode_code),
    CONSTRAINT fk_project_country FOREIGN KEY (country_code) REFERENCES t_country (country_code)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_project_owner FOREIGN KEY (owner_enterprise_id) REFERENCES t_enterprise (enterprise_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_project_invest CHECK (invest_amount > 0),
    CONSTRAINT ck_project_status CHECK (status IN ('前期', '在建', '竣工', '已移交', '暂停')),
    CONSTRAINT ck_project_end CHECK (planned_end_date IS NULL OR planned_start_date IS NULL
        OR planned_end_date >= planned_start_date),
    CONSTRAINT ck_project_actual_end CHECK (actual_end_date IS NULL OR actual_start_date IS NULL
        OR actual_end_date >= actual_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目主数据：国家到项目 1:N 的 N 端';

-- 10. 合同：一个项目 1:N 多份合同（设计/施工/设备/监理）
CREATE TABLE t_contract (
    contract_id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '合同主键',
    project_id            BIGINT        NOT NULL COMMENT '所属项目',
    contract_no           VARCHAR(30)   NOT NULL COMMENT '合同编号，业务唯一',
    contract_type         VARCHAR(20)   NOT NULL COMMENT '合同类型：设计/施工/设备/监理',
    party_b_enterprise_id BIGINT        NULL     COMMENT '承接方（乙方）企业',
    amount                DECIMAL(18,2) NOT NULL COMMENT '合同金额',
    currency_code         CHAR(3)       NOT NULL DEFAULT 'CNY' COMMENT '币种',
    sign_date             DATE          NULL     COMMENT '签约日期',
    remark                VARCHAR(300)  NULL     COMMENT '备注',
    PRIMARY KEY (contract_id),
    UNIQUE KEY uk_contract_no (contract_no),
    KEY idx_contract_project (project_id),
    KEY idx_contract_enterprise (party_b_enterprise_id),
    KEY idx_contract_sign_date (sign_date),
    KEY idx_contract_type (contract_type),
    CONSTRAINT fk_contract_project FOREIGN KEY (project_id) REFERENCES t_project (project_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_contract_enterprise FOREIGN KEY (party_b_enterprise_id) REFERENCES t_enterprise (enterprise_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_contract_amount CHECK (amount > 0),
    CONSTRAINT ck_contract_type CHECK (contract_type IN ('设计', '施工', '设备', '监理'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目合同：合同金额之和应等于项目投资口径（Q2-2 一致性校验）';

-- 11. 项目状态变更轨迹
CREATE TABLE t_project_status_log (
    log_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    project_id  BIGINT       NOT NULL COMMENT '项目',
    from_status VARCHAR(10)  NULL     COMMENT '变更前状态，首条为空',
    to_status   VARCHAR(10)  NOT NULL COMMENT '变更后状态',
    change_date DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    operator    VARCHAR(50)  NULL     COMMENT '操作人',
    remark      VARCHAR(200) NULL     COMMENT '变更原因',
    PRIMARY KEY (log_id),
    KEY idx_psl_project (project_id),
    KEY idx_psl_date (change_date),
    CONSTRAINT fk_psl_project FOREIGN KEY (project_id) REFERENCES t_project (project_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_psl_to_status CHECK (to_status IN ('前期', '在建', '竣工', '已移交', '暂停'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目状态变更轨迹：项目进退场可追溯';

-- =====================================================================
-- 12. 项目参与企业：带属性的多对多（M3 核心，答辩重点）
-- 复合主键三列：(project_id, enterprise_id, role_code)
-- 为什么要三列：同一家企业在同一个项目里可能同时是“设计”和“施工”，
-- 只用两列会把这两种角色压成一条记录；第三列是联系自身的属性判定键。
-- =====================================================================
CREATE TABLE t_project_participant (
    project_id    BIGINT       NOT NULL COMMENT '项目',
    enterprise_id BIGINT       NOT NULL COMMENT '参与企业',
    role_code     VARCHAR(20)  NOT NULL COMMENT '角色：ZC=总承包 LH=联合体成员 FB=分包 SJ=设计 JL=监理',
    share_pct     DECIMAL(5,2) NULL     COMMENT '工作份额百分比，可为空',
    join_date     DATE         NULL     COMMENT '参与日期',
    consortium_id BIGINT       NULL     COMMENT '以联合体身份参与时填写',
    PRIMARY KEY (project_id, enterprise_id, role_code),
    KEY idx_pp_enterprise (enterprise_id),
    KEY idx_pp_role (role_code),
    KEY idx_pp_consortium (consortium_id),
    CONSTRAINT fk_pp_project FOREIGN KEY (project_id) REFERENCES t_project (project_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pp_enterprise FOREIGN KEY (enterprise_id) REFERENCES t_enterprise (enterprise_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_pp_consortium FOREIGN KEY (consortium_id) REFERENCES t_consortium (consortium_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_pp_share CHECK (share_pct IS NULL OR (share_pct >= 0 AND share_pct <= 100)),
    CONSTRAINT ck_pp_role CHECK (role_code IN ('ZC', 'LH', 'FB', 'SJ', 'JL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目参与企业：带属性的多对多联系，复合主键三列';

-- =====================================================================
-- M4 资金线（B）：资金来源、年度计划、实际拨付
-- =====================================================================

-- 13. 资金来源：政策性银行 / 多边机构 / 专项基金 / 东道国财政 / 企业自筹
CREATE TABLE t_funding_source (
    source_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资金来源主键',
    source_code   VARCHAR(20)  NOT NULL                COMMENT '来源编码，如 SRC01',
    source_name   VARCHAR(60)  NOT NULL                COMMENT '来源名称，如 中国进出口银行',
    source_type   VARCHAR(20)  NOT NULL                COMMENT '政府援助/政策性银行/商业贷款/企业自筹/多边机构',
    country_code  CHAR(3)      NULL                    COMMENT '资金提供方所属国家（多边机构可空）',
    institution   VARCHAR(100) NULL                    COMMENT '具体机构/部门',
    remark        VARCHAR(200) NULL                    COMMENT '备注',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (source_id),
    UNIQUE KEY uk_source_code (source_code),
    KEY idx_source_country (country_code),
    CONSTRAINT fk_source_country FOREIGN KEY (country_code) REFERENCES t_country (country_code)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_source_type CHECK (source_type IN ('政府援助', '政策性银行', '商业贷款', '企业自筹', '多边机构'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资金来源（M4）：Q4-3 来源占比的维表';

-- 14. 年度资金计划：一个项目一年一条（UNIQUE 保证；upsertPlan 依赖它）
CREATE TABLE t_fund_plan (
    plan_id       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '计划主键',
    project_id    BIGINT        NOT NULL                COMMENT '所属项目',
    plan_year     SMALLINT      NOT NULL                COMMENT '计划年度，如 2024',
    plan_amount   DECIMAL(18,2) NOT NULL                COMMENT '年度计划金额（元）',
    currency_code CHAR(3)       NOT NULL DEFAULT 'CNY'  COMMENT '币种',
    approved_date DATE          NULL                    COMMENT '计划批复日期',
    remark        VARCHAR(200)  NULL                    COMMENT '备注',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (plan_id),
    UNIQUE KEY uk_plan_project_year (project_id, plan_year),
    CONSTRAINT fk_plan_project FOREIGN KEY (project_id) REFERENCES t_project (project_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_plan_amount CHECK (plan_amount > 0),
    CONSTRAINT ck_plan_year CHECK (plan_year BETWEEN 2000 AND 2100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年度资金计划（M4）：到位率的分母，同项目同年度唯一';

-- 15. 资金拨付：实际发生额（字段按 B-1 字段需求）
CREATE TABLE t_fund_disbursement (
    disb_id       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '拨付主键',
    project_id    BIGINT        NOT NULL                COMMENT '所属项目',
    source_id     BIGINT        NOT NULL                COMMENT '资金来源',
    plan_id       BIGINT        NULL                    COMMENT '对应年度计划（可空＝计划外拨付）',
    disb_date     DATE          NOT NULL                COMMENT '拨付日期',
    disb_amount   DECIMAL(18,2) NOT NULL                COMMENT '拨付金额（元）',
    currency_code CHAR(3)       NOT NULL DEFAULT 'CNY'  COMMENT '币种',
    purpose       VARCHAR(200)  NULL                    COMMENT '用途：设备采购/土建/征地拆迁/人员费',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (disb_id),
    KEY idx_disb_project (project_id),
    KEY idx_disb_date (disb_date),
    KEY idx_disb_source (source_id),
    KEY idx_disb_plan (plan_id),
    CONSTRAINT fk_disb_project FOREIGN KEY (project_id) REFERENCES t_project (project_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_disb_source FOREIGN KEY (source_id) REFERENCES t_funding_source (source_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_disb_plan FOREIGN KEY (plan_id) REFERENCES t_fund_plan (plan_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_disb_amount CHECK (disb_amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资金拨付流水（M4）：到位率的分子';

-- =====================================================================
-- M5 进度里程碑（C）：里程碑字典 + 项目里程碑（生成列自动判超期）
-- =====================================================================

-- 16. 里程碑字典
CREATE TABLE t_milestone (
    milestone_id   INT         NOT NULL AUTO_INCREMENT COMMENT '里程碑主键',
    milestone_code VARCHAR(20) NOT NULL COMMENT '编码：START/STRUCT/EQUIP/POWER/OPEN/TURNOVER',
    milestone_name VARCHAR(50) NOT NULL COMMENT '名称：开工/主体结构/设备安装/带电通车/竣工移交',
    seq_no         INT         NOT NULL DEFAULT 0 COMMENT '建议顺序',
    planned_days   INT         NULL     COMMENT '基准工期天数，可为空',
    PRIMARY KEY (milestone_id),
    UNIQUE KEY uk_milestone_code (milestone_code),
    CONSTRAINT ck_milestone_days CHECK (planned_days IS NULL OR planned_days > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='里程碑字典：6 类节点，全项目共享';

-- 17. 项目里程碑：计划日期 + 实际日期，is_delayed 由数据库自动算
-- 三态必须分清：actual_date IS NULL → 未完成（is_delayed 为 NULL）；0=准时；1=超期
CREATE TABLE t_project_milestone (
    pm_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '项目里程碑主键',
    project_id   BIGINT       NOT NULL COMMENT '项目',
    milestone_id INT          NOT NULL COMMENT '里程碑类型',
    plan_date    DATE         NOT NULL COMMENT '计划完成日期',
    actual_date  DATE         NULL     COMMENT '实际完成日期，未完成时为空',
    is_delayed   TINYINT      GENERATED ALWAYS AS (
                     CASE WHEN actual_date IS NULL THEN NULL
                          WHEN actual_date > plan_date THEN 1
                          ELSE 0 END) STORED COMMENT '是否超期：NULL=未完成，0=准时，1=超期',
    remark       VARCHAR(200) NULL     COMMENT '备注',
    PRIMARY KEY (pm_id),
    UNIQUE KEY uk_pm_project_milestone (project_id, milestone_id),
    KEY idx_pm_milestone (milestone_id),
    KEY idx_pm_plan_date (plan_date),
    KEY idx_pm_actual_date (actual_date),
    KEY idx_pm_delayed (is_delayed),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES t_project (project_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pm_milestone FOREIGN KEY (milestone_id) REFERENCES t_milestone (milestone_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目里程碑：超期由生成列 STORED 自动判定，Java 只读不算';

-- =====================================================================
-- M6 履约报告（B）：月度报告 + 多值指标（EAV）
-- =====================================================================

-- 18. 进度报告：同项目同报告期唯一（唯一约束挡住重复报工）
CREATE TABLE t_progress_report (
    report_id      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '报告主键',
    project_id     BIGINT        NOT NULL                COMMENT '所属项目',
    period_ym      CHAR(7)       NOT NULL                COMMENT '报告期 YYYY-MM',
    progress_pct   DECIMAL(5,2)  NOT NULL                COMMENT '形象进度百分比 0~100',
    on_site_staff  INT           NOT NULL DEFAULT 0      COMMENT '现场用工人数',
    local_staff    INT           NOT NULL DEFAULT 0      COMMENT '其中属地员工人数',
    output_value   DECIMAL(18,2) NOT NULL DEFAULT 0      COMMENT '本期完成产值（元）',
    safety_events  INT           NOT NULL DEFAULT 0      COMMENT '安全事件数（M6 界面“事件数”）',
    quality_events INT           NOT NULL DEFAULT 0      COMMENT '质量事件数（M6 界面“事件数”）',
    report_date    DATE          NULL                    COMMENT '填报日期',
    reporter       VARCHAR(50)   NULL                    COMMENT '填报人',
    remark         VARCHAR(200)  NULL                    COMMENT '备注',
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (report_id),
    UNIQUE KEY uk_report_project_period (project_id, period_ym),
    KEY idx_report_period (period_ym),
    KEY idx_report_project (project_id),
    CONSTRAINT fk_report_project FOREIGN KEY (project_id) REFERENCES t_project (project_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_report_progress CHECK (progress_pct >= 0 AND progress_pct <= 100),
    CONSTRAINT ck_report_staff CHECK (on_site_staff >= 0 AND local_staff >= 0),
    CONSTRAINT ck_report_local CHECK (local_staff <= on_site_staff),
    CONSTRAINT ck_report_output CHECK (output_value >= 0),
    CONSTRAINT ck_report_period CHECK (period_ym LIKE '____-__')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目履约进度月报（M6）：同项目同月唯一，挡住重复报工';

-- 19. 报告指标：一条报告挂多个指标（多值事实的 EAV 表）
CREATE TABLE t_report_indicator (
    report_id      BIGINT        NOT NULL COMMENT '所属报告',
    indicator_code VARCHAR(20)   NOT NULL COMMENT '指标编码：TRAINING 培训人次 / LOCAL_SUPPLY 本地采购额',
    indicator_val  DECIMAL(18,2) NOT NULL COMMENT '本期指标值（增量，不是累计）',
    unit           VARCHAR(10)   NULL     COMMENT '单位：人次/万元/工时',
    PRIMARY KEY (report_id, indicator_code),
    KEY idx_indicator_code (indicator_code),
    CONSTRAINT fk_ri_report FOREIGN KEY (report_id) REFERENCES t_progress_report (report_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_ri_val CHECK (indicator_val >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月报多值指标（M6，EAV）：M7 培训承诺的实际值来源';

-- =====================================================================
-- M7 履约承诺比对（C）：承诺 + 比对结果
-- =====================================================================

-- 20. 履约承诺：目标值 + 比较方向 + 基准日（业务规则做成数据）
CREATE TABLE t_commitment (
    commit_id    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '承诺主键',
    project_id   BIGINT        NOT NULL COMMENT '项目',
    commit_type  VARCHAR(10)   NOT NULL COMMENT '类型：工期/金额/属地用工/培训',
    target_value DECIMAL(18,2) NOT NULL COMMENT '目标值：工期为天数、金额为元、用工为百分比、培训为人次',
    compare_op   VARCHAR(2)    NOT NULL COMMENT '比较方向：<= 表示不超过，>= 表示不低于',
    base_date    DATE          NULL     COMMENT '基准日（工期从签约日/开工日算），显式存储不硬编码',
    clause_text  VARCHAR(500)  NULL     COMMENT '承诺条款原文，界面可展开',
    sign_date    DATE          NULL     COMMENT '承诺签署日期',
    remark       VARCHAR(200)  NULL     COMMENT '备注',
    PRIMARY KEY (commit_id),
    KEY idx_commit_project (project_id),
    KEY idx_commit_type (commit_type),
    CONSTRAINT fk_commit_project FOREIGN KEY (project_id) REFERENCES t_project (project_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_commit_type CHECK (commit_type IN ('工期', '金额', '属地用工', '培训')),
    CONSTRAINT ck_commit_op CHECK (compare_op IN ('<=', '>=')),
    CONSTRAINT ck_commit_target CHECK (target_value >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='履约承诺：四类承诺，比对前的“计划值”';

-- 21. 承诺比对结果：一条承诺一条结果（commit_id 唯一，calcAll 幂等）
-- actual_value 允许 NULL：三值逻辑，NULL 不是 0，单独判“待评估”
CREATE TABLE t_commitment_result (
    result_id     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '结果主键',
    commit_id     BIGINT        NOT NULL COMMENT '承诺',
    actual_value  DECIMAL(18,2) NULL     COMMENT '实际值，无法计算时为 NULL',
    gap_value     DECIMAL(18,2) NULL     COMMENT '差距 = 实际值 - 目标值',
    commit_status VARCHAR(10)   NOT NULL COMMENT '结论：已履约/部分履约/未履约/待评估',
    calc_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    PRIMARY KEY (result_id),
    UNIQUE KEY uk_result_commit (commit_id),
    KEY idx_result_status (commit_status),
    CONSTRAINT fk_result_commit FOREIGN KEY (commit_id) REFERENCES t_commitment (commit_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_result_status CHECK (commit_status IN ('已履约', '部分履约', '未履约', '待评估'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='承诺比对结果：四档结论，NULL 单独判待评估';

-- =====================================================================
-- M8 资金-进度协同预警（B）：规则表 + 预警记录
-- 预警不是写死 if，而是“规则表 + 视图 + 刷新”，改阈值不改代码
-- =====================================================================

-- 22. 预警规则：阈值、级别、是否启用都做成数据
CREATE TABLE t_warning_rule (
    rule_id      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '规则主键',
    rule_code    VARCHAR(20)   NOT NULL                COMMENT '规则编码 F1/F2/F3/F4',
    rule_name    VARCHAR(60)   NOT NULL                COMMENT '规则名称，如 资金断档',
    level_code   CHAR(1)       NOT NULL                COMMENT 'R 红灯 / Y 黄灯 / B 蓝灯',
    cond1_metric VARCHAR(20)   NOT NULL                COMMENT '条件1指标：FUND_RATE/PROGRESS_PCT/OVERDUE_COUNT/LOCAL_HIRE_RATE',
    cond1_op     VARCHAR(4)    NOT NULL                COMMENT '条件1比较符：LT/LE/GT/GE/EQ',
    cond1_value  DECIMAL(10,2) NOT NULL                COMMENT '条件1阈值',
    cond2_metric VARCHAR(20)   NULL                    COMMENT '条件2指标（可空）',
    cond2_op     VARCHAR(4)    NULL                    COMMENT '条件2比较符（可空）',
    cond2_value  DECIMAL(10,2) NULL                    COMMENT '条件2阈值（可空）',
    is_enabled   TINYINT       NOT NULL DEFAULT 1      COMMENT '是否启用 1/0',
    message_tpl  VARCHAR(200)  NOT NULL                COMMENT '预警说明模板，{v1}/{v2} 替换为实际值',
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (rule_id),
    UNIQUE KEY uk_rule_code (rule_code),
    CONSTRAINT ck_rule_level CHECK (level_code IN ('R', 'Y', 'B')),
    CONSTRAINT ck_rule_metric CHECK (cond1_metric IN ('FUND_RATE', 'PROGRESS_PCT', 'OVERDUE_COUNT', 'LOCAL_HIRE_RATE', 'REPORT_GAP_MONTH')),
    CONSTRAINT ck_rule_op CHECK (cond1_op IN ('LT', 'LE', 'GT', 'GE', 'EQ'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预警规则（M8）：阈值存在表里，改阈值不改代码';

-- 23. 预警记录：规则+项目+状态唯一，保证 refreshWarnings 幂等
CREATE TABLE t_warning (
    warn_id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '预警主键',
    rule_id          BIGINT       NOT NULL                COMMENT '触发规则',
    project_id       BIGINT       NOT NULL                COMMENT '涉及项目',
    period_ym        CHAR(7)      NOT NULL                COMMENT '预警期 YYYY-MM（取该项目最新报告期）',
    warn_level       CHAR(1)      NOT NULL                COMMENT 'R/Y/B，刷新时按规则快照',
    rule_name        VARCHAR(60)  NOT NULL                COMMENT '规则名称快照（规则改名不影响历史预警）',
    trigger_snapshot VARCHAR(200) NULL                    COMMENT '触发时的指标值快照，如 到位率41.2%<60',
    status           VARCHAR(8)   NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ACK/CLOSED',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次触发时间',
    ack_at           TIMESTAMP    NULL                    COMMENT '确认时间',
    ack_by           VARCHAR(50)  NULL                    COMMENT '确认人',
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后刷新时间',
    PRIMARY KEY (warn_id),
    UNIQUE KEY uk_warning_rule_project_period (rule_id, project_id, period_ym),
    KEY idx_warning_level (warn_level),
    KEY idx_warning_status (status),
    KEY idx_warning_project (project_id),
    CONSTRAINT fk_warning_rule FOREIGN KEY (rule_id) REFERENCES t_warning_rule (rule_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_warning_project FOREIGN KEY (project_id) REFERENCES t_project (project_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_warning_level CHECK (warn_level IN ('R', 'Y', 'B')),
    CONSTRAINT ck_warning_status CHECK (status IN ('OPEN', 'ACK', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预警实例（M8）：按 规则+项目+预警期 唯一，刷新幂等';

-- =====================================================================
-- M9 成果展示（C）：项目成效，与项目 1:1
-- =====================================================================

-- 24. 项目成效：受益人口、就业、培训、绿色装机、标准输出（思政报表数据源）
CREATE TABLE t_project_outcome (
    project_id    BIGINT        NOT NULL COMMENT '项目（主键即外键，保证 1:1）',
    beneficiary_k DECIMAL(12,2) NULL     COMMENT '受益人口（千人）',
    jobs_created  INT           NULL     COMMENT '直接创造就业岗位数',
    training_ppl  INT           NULL     COMMENT '培训人次',
    green_mw      DECIMAL(10,2) NULL     COMMENT '绿色装机容量（MW）',
    standards_out INT           NULL     COMMENT '输出标准数量',
    update_date   DATE          NULL     COMMENT '数据更新日期',
    remark        VARCHAR(300)  NULL     COMMENT '备注',
    PRIMARY KEY (project_id),
    CONSTRAINT fk_outcome_project FOREIGN KEY (project_id) REFERENCES t_project (project_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_outcome_nonneg CHECK (
        (beneficiary_k IS NULL OR beneficiary_k >= 0)
        AND (jobs_created IS NULL OR jobs_created >= 0)
        AND (training_ppl IS NULL OR training_ppl >= 0)
        AND (green_mw IS NULL OR green_mw >= 0)
        AND (standards_out IS NULL OR standards_out >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目成效：与项目 1:1，三条思政查询的数据源';

-- =====================================================================
-- 自检（可选，手动执行）
--   SHOW TABLES;                                   -- 期望 24 张表
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_schema = 'bri_pm';                -- 期望 24
--   SELECT table_name, constraint_name, constraint_type
--     FROM information_schema.table_constraints
--    WHERE table_schema = 'bri_pm' AND constraint_type <> 'PRIMARY KEY'
--    ORDER BY table_name, constraint_type;         -- 抽查外键/唯一/检查约束
-- =====================================================================
