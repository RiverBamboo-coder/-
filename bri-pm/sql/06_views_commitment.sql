-- =====================================================================
-- 06_views_commitment.sql · M7 履约承诺比对视图（C 负责）
-- 项目：“一带一路”沿线国家基础设施合作项目管理信息系统（bri-pm）
-- 数据库：MySQL 8.0 / InnoDB / utf8mb4
--
-- 说明：
--   1) 视图单独成文件，避免与 A 的 01_schema.sql、B 的 05_views_fund.sql 抢文件；
--   2) v_commitment_actual 是 M7 的核心：把“工期/金额/属地用工/培训”四类实际值
--      从不同粒度的表统一对齐到“承诺”粒度，供 CommitmentDao.calcAll() 比对；
--   3) 依赖关系：只读 A 的 t_project、C 的 t_milestone/t_project_milestone/t_commitment，
--      B 的 t_fund_disbursement/t_progress_report/t_report_indicator（01_schema 已建全，可随时创建）。
--
-- 四个难点（答辩核心，对应任务书 3.3）：
--   · 多源对齐   → CASE 把 3 张不同粒度表的实际值统一到项目粒度；
--   · 三值逻辑   → 未拨付/未填报时 actual_value 为 NULL（不是 0），calcAll() 首判 NULL → “待评估”；
--   · 除零保护   → 属地用工用 NULLIF(SUM(on_site_staff), 0)；
--   · 基准日     → 工期承诺用 base_date 字段显式存基准日，不硬编码。
-- =====================================================================

SET NAMES utf8mb4;
USE bri_pm;

-- ---------------------------------------------------------------------
-- 视图 1：履约承诺实际值（四类承诺 → 统一到承诺粒度）
-- ---------------------------------------------------------------------
DROP VIEW IF EXISTS v_commitment_actual;

CREATE OR REPLACE VIEW v_commitment_actual AS
SELECT c.commit_id,
       c.project_id,
       c.commit_type,
       c.target_value,
       c.compare_op,
       c.base_date,
       CASE c.commit_type
           -- 工期：实际工期 = 该项目的最后一个实际完成里程碑日 - 基准日（天）
           WHEN '工期' THEN DATEDIFF(
               (SELECT MAX(pm.actual_date)
                  FROM t_project_milestone pm
                 WHERE pm.project_id = c.project_id),
               c.base_date)
           -- 金额：实际拨付累计（未拨付时 SUM 为 NULL → 待评估，不能 COALESCE 成 0）
           WHEN '金额' THEN (SELECT SUM(d.disb_amount)
                               FROM t_fund_disbursement d
                              WHERE d.project_id = c.project_id)
           -- 属地用工：最新报告里的属地员工占比（分母为 0 时 NULLIF 返回 NULL，不除零）
           WHEN '属地用工' THEN (SELECT ROUND(100.0 * SUM(r.local_staff)
                                              / NULLIF(SUM(r.on_site_staff), 0), 2)
                                   FROM t_progress_report r
                                  WHERE r.project_id = c.project_id)
           -- 培训：进度报告指标里的培训人次合计（未填报时 SUM 为 NULL → 待评估）
           WHEN '培训' THEN (SELECT SUM(i.indicator_val)
                               FROM t_report_indicator i
                               JOIN t_progress_report r USING (report_id)
                              WHERE r.project_id = c.project_id
                                AND i.indicator_code = 'TRAINING')
       END AS actual_value
FROM t_commitment c;

-- ---------------------------------------------------------------------
-- 视图 2：承诺比对结果（比对结果 + 承诺 + 项目，供 D 的 M7 面板直接展示/筛选）
-- 注意：t_commitment_result 由 CommitmentDao.calcAll() 写入，未计算时本视图为空。
-- ---------------------------------------------------------------------
DROP VIEW IF EXISTS v_commitment_result;

CREATE OR REPLACE VIEW v_commitment_result AS
SELECT p.project_id,
       p.project_code,
       p.project_name,
       c.commit_id,
       c.commit_type,
       c.target_value,
       c.compare_op,
       c.clause_text,
       r.actual_value,
       r.gap_value,
       r.commit_status,
       r.calc_time
FROM t_commitment c
JOIN t_commitment_result r ON r.commit_id = c.commit_id
JOIN t_project p           ON p.project_id = c.project_id;

-- =====================================================================
-- 自检（可选，手动执行）
--   SHOW FULL TABLES WHERE Table_type = 'VIEW';        -- 期望看到 2 个 v_commitment_* 视图
--   SELECT * FROM v_commitment_actual LIMIT 5;         -- 四类承诺的实际值（可能为 NULL）
-- =====================================================================
