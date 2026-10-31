-- =====================================================================
-- 05_view_b_fund.sql  ·  B 段视图（M4 到位率 / M8 资金-进度协同）
-- ---------------------------------------------------------------------
-- 谁写：B
-- 执行：01_schema.sql → 02 → 03 → 04_seed_business.sql → 本文件（可重复执行）
-- 为什么单独成文件：
--   视图只依赖表结构，不依赖种子数据，所以既能挂在 01_schema.sql 末尾（A 的选择），
--   也能作为第 5 个脚本单独执行（B 本地的选择）。用 CREATE OR REPLACE，
--   两种方式都不会因为“重复定义视图”报错。
--
-- 两个视图的分工：
--   v_fund_rate      项目级“资金到位率” = 实拨 / 计划，M4 列表、Q4-1/Q4-2、预警规则都读它；
--   v_fund_progress  资金 × 进度 协同视图，M8 预警看板读它（只有“在建”项目）。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- v_fund_rate：项目资金到位率
--   口径（答辩要说清）：以“年度资金计划”为抓手，拨付按 plan_id 归到计划上再汇总；
--   因此 plan_id 为空的“计划外拨付”不计入到位率（这类应急款在 M4 明细里照常显示）。
--   除零保护：计划为 0 时 rate_pct 返回 NULL 而不是报错/0 —— NULL 表示“无从谈起”，
--   和“到位率 0%”是两件事，前端显示“—”，排序时 NULL 排最后。
--
--   【重要】这里先把计划、拨付各自聚合成一行再 JOIN，不能像任务书 2.3 的示例那样
--   “LEFT JOIN 计划 LEFT JOIN 拨付 ON d.plan_id = pl.plan_id”直接 SUM：
--   一个年度计划有多笔拨付时，计划金额会被重复计数（3 年计划 × 每计划 2 笔拨付，
--   分母就凭空翻倍，到位率直接减半）。本视图的写法在 04 的种子数据上实测：
--   任务书示例 SQL 算出 P001 = 26.26%，真实值 52.52%，正好差 2 倍（每计划 2 笔拨付） —— 这就是“分析函数
--   用错导致口径失真”的典型案例，也是 B 段最值得讲的一个技术细节。
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_fund_rate AS
SELECT p.project_id,
       p.project_code,
       p.project_name,
       p.status,
       COALESCE(plan.plan_total, 0)                                  AS plan_total,
       COALESCE(disb.actual_total, 0)                                AS actual_total,
       CASE WHEN COALESCE(plan.plan_total, 0) = 0 THEN NULL
            ELSE ROUND(100.0 * COALESCE(disb.actual_total, 0) / plan.plan_total, 2)
       END                                                           AS rate_pct
FROM t_project p
LEFT JOIN (SELECT project_id, SUM(plan_amount) AS plan_total
           FROM t_fund_plan
           GROUP BY project_id) plan ON plan.project_id = p.project_id
LEFT JOIN (SELECT pl.project_id, SUM(d.disb_amount) AS actual_total
           FROM t_fund_disbursement d
           JOIN t_fund_plan pl ON pl.plan_id = d.plan_id
           GROUP BY pl.project_id) disb ON disb.project_id = p.project_id
WHERE p.status IN ('在建', '竣工');

-- ---------------------------------------------------------------------
-- v_fund_progress：资金 × 进度 协同视图（M8 预警的输入）
--   每个在建项目一行：到位率 + 最新一期形象进度。
--   “最新一期”用 MAX(period_ym) 定位，避免 MO_M 报告把进度算错。
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_fund_progress AS
SELECT p.project_id,
       p.project_code,
       p.project_name,
       c.country_name,
       fr.rate_pct                    AS fund_rate,
       COALESCE(r.progress_pct, 0)    AS cur_progress,
       lr.ym                          AS cur_period
FROM t_project p
JOIN t_country c ON c.country_code = p.country_code
LEFT JOIN v_fund_rate fr ON fr.project_id = p.project_id
LEFT JOIN (SELECT project_id, MAX(period_ym) AS ym
           FROM t_progress_report
           GROUP BY project_id) lr ON lr.project_id = p.project_id
LEFT JOIN t_progress_report r ON r.project_id = p.project_id AND r.period_ym = lr.ym
WHERE p.status = '在建';

-- 自检：把两个视图各看一眼（行数应与“在建+竣工”项目数一致）
SELECT 'v_fund_rate' AS 视图, COUNT(*) AS 行数 FROM v_fund_rate
UNION ALL
SELECT 'v_fund_progress', COUNT(*) FROM v_fund_progress;
