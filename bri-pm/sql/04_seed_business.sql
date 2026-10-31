-- =====================================================================
-- 04_seed_business.sql  ·  B 段：资金 / 履约报告 / 预警规则种子数据
-- ---------------------------------------------------------------------
-- 谁写：B（本文件下半段的“C 段：里程碑/承诺/成效”由 C 追加，B 不动 C 的段落）
-- 前置：01_schema.sql（含 B 段 7 张表）+ 02_dict_and_user.sql + 03_seed_basic.sql
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p bri_pm < sql/04_seed_business.sql
--
-- 设计说明（答辩可直接讲）：
--   1) 数据量：资金来源 9 / 年度计划 120（40 项目×3 年）/ 拨付 240（每计划 2 笔）
--      / 月报 240（40 项目×2025 年 1–6 月）/ 指标 480（每报告 2 个）/ 规则 5 条。
--   2) 可重复执行：先按外键反序 DELETE 自己的 6 张表，再 INSERT，不用 DROP TABLE；
--      连续执行两次结果完全一致（末尾有自检输出，行数不会翻倍）。
--   3) 与 A 的主键解耦：所有金额/进度都是“确定性公式”由 t_project 推导出来的，
--      不写死具体数值，也不使用 RAND()，所以换机器、换时间执行结果都一样。
--      项目主键若整体偏移，只需改下面的 @pid_base（默认 0，即 project_id 1–40）。
--   4) 年度节奏：2023 年按计划 90% 拨付、2024 年 100%、2025 年 110%
--      （三个年度的到位率因此不同，Q4-4“年度计划 vs 实际”才有对比意义）；
--      三个年度按 30%/40%/30% 加权后约等于项目总到位率（分年四舍五入会有零点几个点的偏差）。
--   5) 数值故意留出了“业务故事”：
--      有的项目资金到位率只有 38%–58%（F1 资金断档），
--      有的项目钱到位 102%–108% 但进度只有 20% 多（F2 资金沉淀），
--      有的项目 6 个月进度一动不动（Q6-3 连续 N 月停滞）。
--      这些不是随机数，是 refreshWarnings() 演示时要用到的“触发样本”。
--   6) 预警实例 t_warning 不在本脚本里造：它由 WarningDao.refreshWarnings() 按规则算出来，
--      保证“改阈值 → 刷新 → 预警变化”这条演示链路只有一处真相。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0. 前置检查：B 段种子数据依赖 A 的 03_seed_basic.sql 已经插好 40 个项目
--    （不满足就让脚本立刻失败，而不是插入一堆错位数据）
-- ---------------------------------------------------------------------
SET @pid_base = 0;
SET @b_project_cnt := (SELECT COUNT(*) FROM t_project
                       WHERE project_id BETWEEN @pid_base + 1 AND @pid_base + 40);
SELECT @b_project_cnt AS '可用项目数(应为40)',
       '不足 40 时请先执行 A 的 03_seed_basic.sql' AS '提示';

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_b_seed_guard (
    cnt INT NOT NULL,
    CONSTRAINT ck_seed_need_40_projects_from_A03 CHECK (cnt >= 40)
) ENGINE = InnoDB;
DELETE FROM tmp_b_seed_guard;
INSERT INTO tmp_b_seed_guard (cnt) VALUES (@b_project_cnt);
DROP TEMPORARY TABLE tmp_b_seed_guard;

-- ---------------------------------------------------------------------
-- 1. 清空 B 段（外键反序；只清自己的 7 张表，不碰 A/C 的数据）
--    顺序：预警 → 规则 → 指标 → 月报 → 拨付 → 计划 → 来源
--    （t_warning_rule 也必须清，否则重复执行时固定 rule_id 会撞主键）
-- ---------------------------------------------------------------------
DELETE FROM t_warning;
DELETE FROM t_warning_rule;
DELETE FROM t_report_indicator;
DELETE FROM t_progress_report;
DELETE FROM t_fund_disbursement;
DELETE FROM t_fund_plan;
DELETE FROM t_funding_source;

-- ---------------------------------------------------------------------
-- 2. t_funding_source：9 个资金来源（固定主键，方便人工核对）
--    覆盖任务书要求的 5 类：政府援助 / 政策性银行 / 商业贷款 / 企业自筹 / 多边机构
-- ---------------------------------------------------------------------
INSERT INTO t_funding_source (source_id, source_code, source_name, source_type, country_code, institution, remark) VALUES
(1, 'SRC01', '中国政府无偿援助',       '政府援助',   'CHN', '商务部国际经济合作事务局', '无偿援助、成套项目'),
(2, 'SRC02', '中国进出口银行',         '政策性银行', 'CHN', '优惠出口买方信贷',         '两优贷款主力'),
(3, 'SRC03', '国家开发银行',           '政策性银行', 'CHN', '国际业务局',               '项目贷款'),
(4, 'SRC04', '世界银行',               '多边机构',   NULL,  'IBRD/IDA',                 '多边开发银行'),
(5, 'SRC05', '亚洲基础设施投资银行',   '多边机构',   'CHN', 'AIIB',                     '多边开发银行'),
(6, 'SRC06', '非洲开发银行',           '多边机构',   NULL,  'AfDB',                     '非洲区域项目'),
(7, 'SRC07', '东道国政府配套资金',     '政府援助',   NULL,  '项目所在国财政部',          '征地、配套基础设施'),
(8, 'SRC08', '承建企业自筹',           '企业自筹',   'CHN',  '中方总承包企业',            '前期垫资、设备投入'),
(9, 'SRC09', '中国工商银行项目贷款',   '商业贷款',   'CHN',  '专项融资部',                '商业性贷款（利率随行就市）');

-- ---------------------------------------------------------------------
-- 3. t_fund_plan：40 项目 × 2023/2024/2025 三年 = 120 条年度计划
--    年度权重 30% / 40% / 30%，项目系数 85%–97%（其余留作预备费）
-- ---------------------------------------------------------------------
INSERT INTO t_fund_plan (project_id, plan_year, plan_amount, currency_code, approved_date, remark)
SELECT r.project_id,
       y.plan_year,
       ROUND(r.inv * r.total_factor * y.weight, 2),
       'CNY',
       MAKEDATE(y.plan_year, 20),
       'B段种子：年度计划 = 总投资 × 项目系数 × 年度权重'
FROM (SELECT t.project_id,
             COALESCE(t.invest_amount, 80000)                  AS inv,
             0.85 + 0.03 * MOD(t.rn, 5)                            AS total_factor
      FROM (SELECT project_id, invest_amount,
                   ROW_NUMBER() OVER (ORDER BY project_id) AS rn
            FROM t_project) t
      WHERE t.rn <= 40) r
CROSS JOIN (SELECT 2023 AS plan_year, 0.30 AS weight
            UNION ALL SELECT 2024, 0.40
            UNION ALL SELECT 2025, 0.30) y;

-- ---------------------------------------------------------------------
-- 4. t_fund_disbursement：每个年度计划拨 2 笔 = 240 条
--    进度系数 ratio 由项目序号决定，于是“项目到位率”就等于 ratio：
--      rn%10 = 0/1/2 → 38% / 52% / 58%（资金断档样本，F1）
--      rn%10 = 7/8   → 102% / 108%（资金沉淀样本，F2）
--    资金来源按 (项目序号 + 年度) 循环分配到 9 个来源，Q4-3 才有分布可言；
--    年度节奏 2023=90%、2024=100%、2025=110%，让 Q4-4 的三个年度到位率不相同。
-- ---------------------------------------------------------------------
INSERT INTO t_fund_disbursement (project_id, source_id, plan_id, disb_date, disb_amount, currency_code, purpose)
SELECT pl.project_id,
       1 + MOD(src.rn + pl.plan_year, 9)                        AS source_id,
       pl.plan_id,
       DATE_ADD(MAKEDATE(pl.plan_year, 1), INTERVAL s.day_offset DAY) AS disb_date,
       ROUND(pl.plan_amount * src.ratio * s.split
             * CASE MOD(pl.plan_year, 3) WHEN 0 THEN 0.90 WHEN 1 THEN 1.00 ELSE 1.10 END, 2) AS disb_amount,
       'CNY',
       s.purpose
FROM t_fund_plan pl
JOIN (SELECT t.project_id,
             t.rn,
             CASE MOD(t.rn, 10)
                 WHEN 0 THEN 0.38 WHEN 1 THEN 0.52 WHEN 2 THEN 0.58 WHEN 3 THEN 0.72
                 WHEN 4 THEN 0.80 WHEN 5 THEN 0.86 WHEN 6 THEN 0.93 WHEN 7 THEN 1.02
                 WHEN 8 THEN 1.08 ELSE 0.66
             END AS ratio
      FROM (SELECT project_id, ROW_NUMBER() OVER (ORDER BY project_id) AS rn FROM t_project) t
      WHERE t.rn <= 40) src ON src.project_id = pl.project_id
CROSS JOIN (SELECT 0.45 AS split, 75  AS day_offset, '土建与设备采购'   AS purpose
            UNION ALL
            SELECT 0.55, 260, '征地拆迁与人员费') s;

-- ---------------------------------------------------------------------
-- 5. t_progress_report：40 项目 × 2025-01 ~ 2025-06 = 240 条月报
--    base_pct 按项目序号分档（22%~70%），step_pct 为每月推进量；
--    rn%13 = 0 的项目 step=0，用来演示 Q6-3“连续 N 月进度不涨”。
-- ---------------------------------------------------------------------
INSERT INTO t_progress_report (project_id, period_ym, progress_pct, on_site_staff, local_staff,
                               output_value, report_date, reporter, remark)
SELECT r.project_id,
       CONCAT('2025-0', m.month_no)                                  AS period_ym,
       LEAST(99.50, ROUND(r.base_pct + (m.month_no - 1) * r.step_pct, 2)) AS progress_pct,
       r.on_site_staff,
       FLOOR(r.on_site_staff * r.local_ratio)                        AS local_staff,
       ROUND(r.inv * r.output_factor * (1 + 0.05 * (m.month_no - 1)), 2) AS output_value,
       STR_TO_DATE(CONCAT('2025-0', m.month_no, '-28'), '%Y-%m-%d')  AS report_date,
       CONCAT('现场组-', LPAD(r.rn, 2, '0'))                         AS reporter,
       'B段种子：月度履约报告'
FROM (SELECT t.project_id,
             t.rn,
             COALESCE(t.invest_amount, 80000) AS inv,
             CASE MOD(t.rn, 10)
                 WHEN 0 THEN 22 WHEN 1 THEN 35 WHEN 2 THEN 41 WHEN 3 THEN 55 WHEN 4 THEN 63
                 WHEN 5 THEN 70 WHEN 6 THEN 46 WHEN 7 THEN 26 WHEN 8 THEN 24 ELSE 58
             END AS base_pct,
             CASE WHEN MOD(t.rn, 13) = 0 THEN 0
                  ELSE 1.20 + MOD(t.rn, 5) * 0.40 END AS step_pct,
             120 + MOD(t.rn * 37, 280)              AS on_site_staff,
             0.35 + MOD(t.rn, 8) * 0.07             AS local_ratio,
             0.008 + 0.001 * MOD(t.rn, 4)           AS output_factor
      FROM (SELECT project_id, invest_amount,
                   ROW_NUMBER() OVER (ORDER BY project_id) AS rn
            FROM t_project) t
      WHERE t.rn <= 40) r
CROSS JOIN (SELECT 1 AS month_no UNION ALL SELECT 2 UNION ALL SELECT 3
            UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) m;

-- ---------------------------------------------------------------------
-- 6. t_report_indicator：每张月报 2 个指标（EAV）= 480 条
--    TRAINING     本期培训人次（C 的 M7 承诺比对要用 SUM，所以是“本期增量”）
--    LOCAL_SUPPLY 本期本地采购额（元）
-- ---------------------------------------------------------------------
INSERT INTO t_report_indicator (report_id, indicator_code, indicator_val, unit)
SELECT r.report_id, 'TRAINING', 20 + MOD(r.report_id * 13, 90), '人次'
FROM t_progress_report r
UNION ALL
SELECT r.report_id, 'LOCAL_SUPPLY', ROUND(r.output_value * (0.15 + 0.01 * MOD(r.report_id, 6)), 2), '万元'
FROM t_progress_report r;

-- ---------------------------------------------------------------------
-- 7. t_warning_rule：5 条规则（阈值在表里，Java 只读表算结果）
--    F1 资金断档   红：到位率 < 60 且 进度 < 50
--    F2 资金沉淀   黄：到位率 > 90 且 进度 < 40
--    F3 里程碑超期 红：超期未完成里程碑 ≥ 3（依赖 C 的 t_project_milestone，未建表时自动跳过）
--    F4 属地用工偏低 蓝：属地用工占比 < 50
--    F5 报告缺失   蓝：留作 D 第二期“按报告数”扩展，默认关闭
-- ---------------------------------------------------------------------
INSERT INTO t_warning_rule (rule_id, rule_code, rule_name, level_code,
                            cond1_metric, cond1_op, cond1_value,
                            cond2_metric, cond2_op, cond2_value,
                            is_enabled, message_tpl) VALUES
(1, 'F1', '资金断档',     'R', 'FUND_RATE',        'LT', 60.00, 'PROGRESS_PCT',    'LT', 50.00, 1, '到位率{v1}%<60%，且形象进度{v2}%<50%'),
(2, 'F2', '资金沉淀',     'Y', 'FUND_RATE',        'GT', 90.00, 'PROGRESS_PCT',    'LT', 40.00, 1, '到位率{v1}%>90%，但进度仅{v2}%<40%，资金沉淀'),
(3, 'F3', '里程碑超期',   'R', 'OVERDUE_COUNT',    'GE',  3.00, NULL,              NULL, NULL,  1, '超期未完成里程碑{v1}个≥3个'),
(4, 'F4', '属地用工偏低', 'B', 'LOCAL_HIRE_RATE',  'LT', 50.00, NULL,              NULL, NULL,  1, '属地用工占比{v1}%<50%'),
(5, 'F5', '报告缺失',     'B', 'REPORT_GAP_MONTH', 'GE',  2.00, NULL,              NULL, NULL,  0, '连续{v1}个月未报进度报告（预留，默认关闭）');

-- ---------------------------------------------------------------------
-- 8. 自检输出（执行完直接把这段结果粘到验证记录里）
-- ---------------------------------------------------------------------
SELECT 'B段种子数据已写入' AS msg;
SELECT '资金来源' AS 表, COUNT(*) AS 行数 FROM t_funding_source
UNION ALL SELECT '年度计划', COUNT(*) FROM t_fund_plan
UNION ALL SELECT '拨付流水', COUNT(*) FROM t_fund_disbursement
UNION ALL SELECT '进度月报', COUNT(*) FROM t_progress_report
UNION ALL SELECT '报告指标', COUNT(*) FROM t_report_indicator
UNION ALL SELECT '预警规则', COUNT(*) FROM t_warning_rule
UNION ALL SELECT '预警实例', COUNT(*) FROM t_warning;

-- =====================================================================
-- C 段：里程碑 / 履约承诺 / 项目成效（由 C 负责）
-- 数据量目标：项目里程碑 ≥150 条（40 项目 × 4 节点 = 160）、履约承诺 ≥80 条、
--            项目成效 40 条（与项目 1:1）。
-- 可重复执行：先按 FK 反向顺序清空（t_commitment_result → t_commitment →
--            t_project_milestone → t_project_outcome → t_milestone），再插入。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 清空 C 段（外键反向删除；t_commitment_result 只删不插，运行期由 calcAll 计算）
-- ---------------------------------------------------------------------
DELETE FROM t_commitment_result;
DELETE FROM t_commitment;
DELETE FROM t_project_milestone;
DELETE FROM t_project_outcome;
DELETE FROM t_milestone;

-- ---------------------------------------------------------------------
-- C-1 里程碑字典（固定主键 1..6，全项目共享；ON DUPLICATE KEY UPDATE 保证幂等）
-- 名称按任务书：开工/主体结构/设备安装/带电通车/竣工移交/试运行
-- ---------------------------------------------------------------------
INSERT INTO t_milestone (milestone_id, milestone_code, milestone_name, seq_no, planned_days)
VALUES
    (1, 'START',    '开工',       1, NULL),
    (2, 'STRUCT',   '主体结构',   2, 180),
    (3, 'EQUIP',    '设备安装',   3, 360),
    (4, 'POWER',    '带电通车',   4, 540),
    (5, 'TURNOVER', '竣工移交',   5, 720),
    (6, 'TRIAL',    '试运行',     6, 900)
ON DUPLICATE KEY UPDATE
    milestone_code = VALUES(milestone_code),
    milestone_name = VALUES(milestone_name),
    seq_no         = VALUES(seq_no),
    planned_days   = VALUES(planned_days);

-- ---------------------------------------------------------------------
-- C-2 项目里程碑：每个项目排前 4 个节点（开工/主体结构/设备安装/带电通车），
-- 计划日期 = 基准日 + 字典里的 planned_days；
-- 实际日期用项目序号做确定性公式（不用 RAND()），制造三种状态（供 is_delayed 演示）：
--   · 开工 / 主体结构（seq 1/2）：全部完成，project_id % 3 != 0 的按 15/30 天超期；
--   · 设备安装（seq 3）：偶数项目完成，其余未完成（actual_date 为 NULL）；
--   · 带电通车（seq 4）：project_id % 5 = 0 的项目完成，其余未完成。
-- 预期：总行数 = 40 × 4 = 160（≥150）；actual_date 为 NULL 约 52 条（≥20）；
--       超期（actual_date > plan_date）约 64 条（≥15）。
-- ---------------------------------------------------------------------
INSERT INTO t_project_milestone (project_id, milestone_id, plan_date, actual_date, remark)
SELECT t.project_id, t.milestone_id, t.plan_date,
       CASE
           WHEN t.seq_no = 1 THEN
               DATE_ADD(t.plan_date, INTERVAL ((t.project_id % 3) * 15) DAY)
           WHEN t.seq_no = 2 THEN
               DATE_ADD(t.plan_date, INTERVAL ((t.project_id % 3) * 20) DAY)
           WHEN t.seq_no = 3 AND (t.project_id % 2) = 0 THEN
               DATE_ADD(t.plan_date, INTERVAL ((t.project_id % 4) * 10) DAY)
           WHEN t.seq_no = 4 AND (t.project_id % 5) = 0 THEN
               DATE_ADD(t.plan_date, INTERVAL ((t.project_id % 3) * 5) DAY)
           ELSE NULL
       END AS actual_date,
       CONCAT('教学示例：', t.milestone_name, '节点')
FROM (
    SELECT p.project_id, m.milestone_id, m.milestone_name, m.seq_no,
           DATE_ADD(COALESCE(p.planned_start_date, p.sign_date),
                    INTERVAL COALESCE(m.planned_days, 0) DAY) AS plan_date
    FROM t_project p
    JOIN t_milestone m ON m.seq_no <= 4
    WHERE COALESCE(p.planned_start_date, p.sign_date) IS NOT NULL
) t
ORDER BY t.project_id, t.seq_no;

-- ---------------------------------------------------------------------
-- C-3 履约承诺：四类承诺（工期/金额/属地用工/培训），目标值+比较方向+基准日都做成数据
-- 结果表 t_commitment_result 由运行期 CommitmentDao.calcAll() 计算，这里只清空。
-- 待评估制造：project_id % 4 = 0 的“工期”承诺 base_date 置 NULL（基准日未定），
--             视图里 DATEDIFF(实际完成日, NULL) = NULL → calcAll 判“待评估”，
--             预期 9 条（project 4/8/12/16/20/24/28/32/36，project 40 无计划竣工日不参与）。
-- ---------------------------------------------------------------------

-- ① 工期承诺：承诺总工期不超过“计划工期天数”，基准日=开工日（缺省用签约日）
INSERT INTO t_commitment (project_id, commit_type, target_value, compare_op, base_date, clause_text, sign_date, remark)
SELECT p.project_id, '工期',
       DATEDIFF(p.planned_end_date, COALESCE(p.planned_start_date, p.sign_date)) AS target_value,
       '<=',
       CASE WHEN (p.project_id % 4) = 0 THEN NULL
            ELSE COALESCE(p.planned_start_date, p.sign_date)
       END AS base_date,
       CONCAT('承诺项目总工期不超过 ',
              DATEDIFF(p.planned_end_date, COALESCE(p.planned_start_date, p.sign_date)),
              ' 天（自开工/签约日起算）'),
       p.sign_date,
       CASE WHEN (p.project_id % 4) = 0 THEN '教学示例：基准日未定→待评估'
            ELSE '教学示例：工期承诺'
       END
FROM t_project p
WHERE p.planned_end_date IS NOT NULL
  AND COALESCE(p.planned_start_date, p.sign_date) IS NOT NULL;

-- ② 金额承诺：承诺累计资金拨付不低于项目总投资的 60%
INSERT INTO t_commitment (project_id, commit_type, target_value, compare_op, base_date, clause_text, sign_date, remark)
SELECT p.project_id, '金额',
       ROUND(p.invest_amount * 0.6, 2),
       '>=',
       NULL,
       CONCAT('承诺累计资金拨付不低于项目总投资的 60%（约 ',
              ROUND(p.invest_amount * 0.6, 2), ' 元）'),
       p.sign_date,
       '教学示例：金额承诺'
FROM t_project p;

-- ③ 属地用工承诺：承诺施工高峰期属地员工占比不低于 60%
INSERT INTO t_commitment (project_id, commit_type, target_value, compare_op, base_date, clause_text, sign_date, remark)
SELECT p.project_id, '属地用工',
       60, '>=', NULL,
       '承诺施工高峰期属地员工占比不低于 60%',
       p.sign_date,
       '教学示例：属地用工承诺'
FROM t_project p
WHERE p.status IN ('在建', '竣工');

-- ④ 培训承诺：承诺为东道国培养不少于 1000 人次技术技能人才
INSERT INTO t_commitment (project_id, commit_type, target_value, compare_op, base_date, clause_text, sign_date, remark)
SELECT p.project_id, '培训',
       1000, '>=', NULL,
       '承诺为东道国培养不少于 1000 人次技术技能人才',
       p.sign_date,
       '教学示例：培训承诺'
FROM t_project p
WHERE p.status IN ('在建', '竣工');

-- ---------------------------------------------------------------------
-- C-4 项目成效：与项目 1:1，覆盖全部 40 个项目（思政报表数据源）
-- 绿色装机只给能源/通信/园区类项目，其余为 NULL（思政 3 据此按领域分组）。
-- ---------------------------------------------------------------------
INSERT INTO t_project_outcome (project_id, beneficiary_k, jobs_created, training_ppl, green_mw, standards_out, update_date, remark)
SELECT p.project_id,
       ROUND(500 + ((p.project_id * 137) % 4500), 2) AS beneficiary_k,          -- 受益人口(千人) 500~5000
       ((p.project_id * 37) % 4000) + 200           AS jobs_created,            -- 就业岗位
       ((p.project_id * 53) % 2500) + 300           AS training_ppl,            -- 培训人次
       CASE WHEN p.field_code IN ('ENERGY', 'COMM', 'PARK')
            THEN ROUND(((p.project_id % 20) * 45.0) + 50, 2)                    -- 绿色装机(MW)
            ELSE NULL
       END                                          AS green_mw,
       (p.project_id % 5) + 1                       AS standards_out,           -- 输出标准数
       CURDATE(),
       '教学示例：项目成效数据'
FROM t_project p;

-- =====================================================================
-- 自检（可选，手动执行）
--   SELECT COUNT(*) AS milestone_cnt    FROM t_project_milestone;   -- 期望 ≥150（160）
--   SELECT COUNT(*) AS commitment_cnt   FROM t_commitment;          -- 期望 ≥80（136）
--   SELECT COUNT(*) AS outcome_cnt      FROM t_project_outcome;     -- 期望 = 项目数(40)
--   SELECT commit_type, COUNT(*) FROM t_commitment GROUP BY commit_type;  -- 四类承诺数量
--   SELECT COUNT(*) FROM t_commitment WHERE base_date IS NULL;      -- 待评估候选（9）
-- =====================================================================
