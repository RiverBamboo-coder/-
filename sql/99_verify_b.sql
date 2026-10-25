-- =====================================================================
-- 99_verify_b.sql  ·  B 段验收自检 SQL（任务书 7.2 的第 7 条：25+ 条查询能出结果）
-- ---------------------------------------------------------------------
-- 谁写：B（A 的 99_verify.sql 汇总时把本文件 include 进去即可，或单独 source）
-- 前置：01_schema.sql + 03_seed_basic.sql + 04_seed_business.sql + 05_view_b_fund.sql
-- 用法：
--   mysql --default-character-set=utf8mb4 -uroot -p bri_pm < sql/99_verify_b.sql
--   带 * 的段落是“应该报错”的负向验证，报错才说明约束生效（见每段说明）。
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 一、数据量体检（对照任务书 2.1 / 6 的表）
-- =====================================================================
SELECT '1.1 数据量' AS 检查项;
SELECT 't_funding_source' AS 表, COUNT(*) AS 行数 FROM t_funding_source
UNION ALL SELECT 't_fund_plan',        COUNT(*) FROM t_fund_plan
UNION ALL SELECT 't_fund_disbursement', COUNT(*) FROM t_fund_disbursement
UNION ALL SELECT 't_progress_report',  COUNT(*) FROM t_progress_report
UNION ALL SELECT 't_report_indicator', COUNT(*) FROM t_report_indicator
UNION ALL SELECT 't_warning_rule',     COUNT(*) FROM t_warning_rule
UNION ALL SELECT 't_warning',          COUNT(*) FROM t_warning;

-- 1.2 每个项目的年度计划必须正好 3 条（2023/2024/2025），否则“到位率”口径不齐
SELECT '1.2 计划年度覆盖（每项目 3 条）' AS 检查项,
       COUNT(*) AS 项目数,
       SUM(CASE WHEN cnt = 3 THEN 1 ELSE 0 END) AS 合规项目数
FROM (SELECT project_id, COUNT(*) AS cnt FROM t_fund_plan GROUP BY project_id) t;

-- 1.3 拨付必须挂在计划上（计划外拨付在 B 段种子里为 0 条，口径才干净）
SELECT '1.3 计划外拨付（应为 0）' AS 检查项, COUNT(*) AS 行数
FROM t_fund_disbursement WHERE plan_id IS NULL;

-- =====================================================================
-- 二、M4 资金四类统计（Q4-1 ~ Q4-5）
-- =====================================================================

-- Q4-1 项目资金到位率（最低 10 个）：到位率 = 实拨 / 计划，除零返回 NULL
SELECT '2.1 Q4-1 到位率（最低 10 个）' AS 检查项;
SELECT project_code, project_name, status, plan_total, actual_total, rate_pct
FROM v_fund_rate
ORDER BY rate_pct IS NULL, rate_pct
LIMIT 10;

-- Q4-2 到位率低于 60% 的项目（预警规则 F1 的候选集）
SELECT '2.2 Q4-2 到位率 < 60% 的项目' AS 检查项;
SELECT project_code, project_name, rate_pct
FROM v_fund_rate
WHERE rate_pct IS NOT NULL AND rate_pct < 60
ORDER BY rate_pct;

-- Q4-3 资金来源占比：窗口函数算占比 + 排名（不用子查询再聚合）
SELECT '2.3 Q4-3 资金来源占比' AS 检查项;
SELECT s.source_name, s.source_type,
       SUM(d.disb_amount)                                                        AS actual_total,
       ROUND(100.0 * SUM(d.disb_amount) / SUM(SUM(d.disb_amount)) OVER (), 2)    AS share_pct,
       RANK() OVER (ORDER BY SUM(d.disb_amount) DESC)                            AS rank_no
FROM t_fund_disbursement d
JOIN t_funding_source s ON s.source_id = d.source_id
GROUP BY s.source_id, s.source_name, s.source_type
ORDER BY actual_total DESC;

-- Q4-4 年度计划 vs 实际（三年到位率应不同：90% / 100% / 110% 的年度节奏）
SELECT '2.4 Q4-4 年度计划 vs 实际' AS 检查项;
SELECT pl.plan_year,
       SUM(pl.plan_amount)                                   AS plan_total,
       COALESCE(SUM(a.actual_total), 0)                      AS actual_total,
       ROUND(100.0 * COALESCE(SUM(a.actual_total), 0) / SUM(pl.plan_amount), 2) AS rate_pct
FROM t_fund_plan pl
LEFT JOIN (SELECT plan_id, SUM(disb_amount) AS actual_total
           FROM t_fund_disbursement WHERE plan_id IS NOT NULL GROUP BY plan_id) a
       ON a.plan_id = pl.plan_id
GROUP BY pl.plan_year
ORDER BY pl.plan_year;

-- Q4-5 超计划拨付（超 0%）前 10 条
SELECT '2.5 Q4-5 超计划拨付（前 10）' AS 检查项;
SELECT p.project_code, pl.plan_year, pl.plan_amount,
       COALESCE(a.actual_total, 0) AS actual_amount,
       ROUND(100.0 * (COALESCE(a.actual_total, 0) - pl.plan_amount) / pl.plan_amount, 2) AS overrun_pct
FROM t_fund_plan pl
JOIN t_project p ON p.project_id = pl.project_id
LEFT JOIN (SELECT plan_id, SUM(disb_amount) AS actual_total
           FROM t_fund_disbursement WHERE plan_id IS NOT NULL GROUP BY plan_id) a
       ON a.plan_id = pl.plan_id
WHERE pl.plan_amount > 0
  AND COALESCE(a.actual_total, 0) > pl.plan_amount
ORDER BY overrun_pct DESC
LIMIT 10;

-- =====================================================================
-- 三、M6 履约报告四类统计（Q6-1 ~ Q6-4）
-- =====================================================================

-- Q6-1 月度环比（以 P001 为例，LAG 窗口函数）
SELECT '3.1 Q6-1 环比（P001）' AS 检查项;
SELECT r.period_ym, r.progress_pct,
       LAG(r.progress_pct) OVER (PARTITION BY r.project_id ORDER BY r.period_ym) AS prev_pct,
       r.progress_pct - LAG(r.progress_pct) OVER (PARTITION BY r.project_id ORDER BY r.period_ym) AS mom_delta
FROM t_progress_report r
JOIN t_project p ON p.project_id = r.project_id
WHERE p.project_code = 'P001'
ORDER BY r.period_ym;

-- Q6-2 国别累计产值前 10
SELECT '3.2 Q6-2 国别累计产值' AS 检查项;
SELECT c.country_name, COUNT(DISTINCT r.project_id) AS project_cnt,
       SUM(r.output_value) AS output_total,
       ROUND(AVG(r.progress_pct), 2) AS avg_progress
FROM t_progress_report r
JOIN t_project p ON p.project_id = r.project_id
JOIN t_country c ON c.country_code = p.country_code
GROUP BY c.country_code, c.country_name
ORDER BY output_total DESC
LIMIT 10;

-- Q6-3 连续 3 个月进度不涨（gaps-and-islands，要求“现在还在停”）
SELECT '3.3 Q6-3 连续 3 个月停滞' AS 检查项;
WITH islands AS (
    SELECT r.project_id, r.period_ym, r.progress_pct,
           ROW_NUMBER() OVER (PARTITION BY r.project_id ORDER BY r.period_ym)
         - ROW_NUMBER() OVER (PARTITION BY r.project_id, r.progress_pct ORDER BY r.period_ym) AS grp
    FROM t_progress_report r
), agg AS (
    SELECT project_id, grp, progress_pct, COUNT(*) AS months,
           MIN(period_ym) AS start_ym, MAX(period_ym) AS end_ym
    FROM islands GROUP BY project_id, grp, progress_pct
)
SELECT p.project_code, p.project_name, a.start_ym, a.end_ym, a.months, a.progress_pct
FROM agg a
JOIN t_project p ON p.project_id = a.project_id
JOIN (SELECT project_id, MAX(period_ym) AS ym FROM t_progress_report GROUP BY project_id) l
  ON l.project_id = a.project_id AND l.ym = a.end_ym
WHERE a.months >= 3
ORDER BY a.months DESC, p.project_code;

-- Q6-4 属地用工占比 < 50%（取最新一期）
SELECT '3.4 Q6-4 属地用工偏低' AS 检查项;
SELECT p.project_code, r.period_ym, r.local_staff, r.on_site_staff,
       ROUND(100.0 * r.local_staff / NULLIF(r.on_site_staff, 0), 2) AS local_hire_pct
FROM t_progress_report r
JOIN t_project p ON p.project_id = r.project_id
JOIN (SELECT project_id, MAX(period_ym) AS ym FROM t_progress_report GROUP BY project_id) l
  ON l.project_id = r.project_id AND l.ym = r.period_ym
WHERE r.on_site_staff > 0 AND 100.0 * r.local_staff / r.on_site_staff < 50
ORDER BY local_hire_pct;

-- =====================================================================
-- 四、M8 预警：视图、规则表、刷新前后对比
-- =====================================================================

-- 4.1 资金-进度协同视图（预警的输入）
SELECT '4.1 v_fund_progress（在建项目）' AS 检查项;
SELECT project_code, country_name, fund_rate, cur_progress, cur_period
FROM v_fund_progress
ORDER BY fund_rate IS NULL, fund_rate
LIMIT 10;

-- 4.2 规则表（阈值是数据，改这里不用改代码）
SELECT '4.2 预警规则' AS 检查项;
SELECT rule_code, rule_name, level_code, cond1_metric, cond1_op, cond1_value,
       cond2_metric, cond2_op, cond2_value, is_enabled
FROM t_warning_rule
ORDER BY rule_id;

-- 4.3 若还没刷新过预警，用 SQL 先“模拟”一次刷新结果（验证规则口径是否正确）
--     注意：正式演示走 WarningDao.refreshWarnings()，不要用这段代替 Java。
SELECT '4.3 规则口径预演（F1 + F2）' AS 检查项;
SELECT v.project_code, v.fund_rate, v.cur_progress,
       CASE WHEN v.fund_rate < 60 AND v.cur_progress < 50 THEN 'F1 红灯'
            WHEN v.fund_rate > 90 AND v.cur_progress < 40 THEN 'F2 黄灯'
            ELSE '正常' END AS 规则判定
FROM v_fund_progress v
HAVING 规则判定 <> '正常'
ORDER BY 规则判定, v.fund_rate;

-- 4.4 刷新后的预警清单（先跑 java com.bri.modB.BSelfTest 或界面点“刷新预警”）
SELECT '4.4 预警清单（刷新后）' AS 检查项;
SELECT w.warn_level, r.rule_code, r.rule_name, p.project_code,
       w.period_ym, w.trigger_snapshot, w.status
FROM t_warning w
JOIN t_warning_rule r ON r.rule_id = w.rule_id
JOIN t_project p      ON p.project_id = w.project_id
ORDER BY FIELD(w.warn_level, 'R', 'Y', 'B'), p.project_code;

-- =====================================================================
-- 五、约束体检（负向验证：下面三条“应该报错”，报错才说明约束在起作用）
--     命令行执行时用 --force 让脚本继续，或逐条单独执行。
-- =====================================================================

-- * 5.1 同项目同月重复报工 → 应报 1062（uk_report_project_period）
-- INSERT INTO t_progress_report (project_id, period_ym, progress_pct) VALUES (1, '2025-06', 88.80);

-- * 5.2 属地员工多于现场总人数 → 应报 3819（ck_report_local_le_onsite）
-- INSERT INTO t_progress_report (project_id, period_ym, progress_pct, on_site_staff, local_staff)
-- VALUES (1, '2025-07', 50.00, 100, 120);

-- * 5.3 拨付金额为 0 → 应报 3819（ck_disb_amount_positive）
-- INSERT INTO t_fund_disbursement (project_id, source_id, disb_date, disb_amount)
-- VALUES (1, 1, '2025-01-01', 0);

-- 5.4 外键体检：拨付指向不存在的项目 → 应报 1452（fk_disb_project）
-- INSERT INTO t_fund_disbursement (project_id, source_id, disb_date, disb_amount)
-- VALUES (99999, 1, '2025-01-01', 100.00);

-- =====================================================================
-- 六、表结构抽查（答辩现场 SHOW CREATE TABLE 用）
-- =====================================================================
SELECT '6.1 约束清单' AS 检查项;
SELECT TABLE_NAME AS 表, CONSTRAINT_NAME AS 约束名, CONSTRAINT_TYPE AS 类型
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('t_funding_source', 't_fund_plan', 't_fund_disbursement',
                     't_progress_report', 't_report_indicator', 't_warning_rule', 't_warning')
ORDER BY TABLE_NAME, CONSTRAINT_TYPE, CONSTRAINT_NAME;

SELECT '6.2 B 段视图' AS 检查项;
SELECT TABLE_NAME AS 视图名 FROM information_schema.VIEWS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('v_fund_rate', 'v_fund_progress');
