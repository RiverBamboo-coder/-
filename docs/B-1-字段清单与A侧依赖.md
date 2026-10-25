# B-1 字段需求清单（B → A 的建表输入）

> 交付人：B　接收人：A（并入 `sql/01_schema.sql`）　对应任务书：2.1 的 B-1、1.3 的 14 张表
> 权威 DDL 就在 [`sql/01_schema_b_fund.sql`](../sql/01_schema_b_fund.sql)，本文是给 A 的“字段清单 + 依赖说明”，A 直接按第 2 节的顺序把 DDL 粘进 `01_schema.sql` 即可。
>
> **✅ 已合并（2026-10-24，A 执行）**：7 张表已按本文第 2 节并入 `sql/01_schema.sql`（全库 24 张），
> `01_schema_b_fund.sql` 不再单独执行；金额单位在合并时统一为「元」。改动明细见 [`B-合并说明.md`](B-合并说明.md)。

---

## 1. 先说要紧的一件事：14 张表其实不够

任务书 1.3 列的 14 张表里，属于 B 的只有 `t_funding_source` 一张。但 B 负责的三个模块要用的表是：

| 模块 | 需要表 | 是否在 A 的 14 张表清单里 |
| --- | --- | --- |
| M4 资金计划与拨付 | `t_funding_source`、`t_fund_plan`、`t_fund_disbursement` | 只有第 1 张 |
| M6 履约报告 | `t_progress_report`、`t_report_indicator` | 都不在 |
| M8 资金-进度预警 | `t_warning_rule`、`t_warning` | 都不在 |

也就是说，**全库最终是 24 张表**（A 的 M0–M3 共 12 张 + B 的 7 张 + C 的 5 张），不是 14 张；合并后的实际清单见 `sql/README.md` 第 2 节。

建议的处理方式（两条都可行，推荐第 1 条）：

1. A 把 `sql/01_schema_b_fund.sql` 里的 7 张表按第 2 节的顺序并进 `01_schema.sql`，`01_schema_b_fund.sql` 保留为“B 段的出处”，不再单独执行；
2. 或者 `01_schema.sql` 只管 A 的 14 张表，第 5 个脚本 `sql/05_view_b_fund.sql` 之前先执行 `01_schema_b_fund.sql`。这样 A 的脚本一行都不用改，但课设文档里要写明“B 段表由 B 脚本创建”。

无论选哪条，**执行顺序都是**：

```text
01_schema.sql（含 B 段 7 张表） → 02_dict_and_user.sql → 03_seed_basic.sql
→ 04_seed_business.sql → 05_view_b_fund.sql → 99_verify_b.sql
```

---

## 2. 七张表（字段 / 类型 / 约束 / 索引）

命名全部按任务书 0.2：表 `t_` 前缀、主键 `表名单数_id`、`fk_表_被引用表`、`uk_表_语义`、`ck_表_语义`。
金额统一 **DECIMAL(18,2)，单位“万元人民币”**（与 `t_project.investment_amount` 同单位，统计时不用换算）。

### 2.1 `t_funding_source` 资金来源

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `source_id` | BIGINT | PK, AUTO_INCREMENT | 来源主键 |
| `source_code` | VARCHAR(20) | NOT NULL, `uk_source_code` | 如 SRC01 |
| `source_name` | VARCHAR(60) | NOT NULL | 如 中国进出口银行 |
| `source_type` | VARCHAR(20) | NOT NULL, `ck_source_type` | 政府援助/政策性银行/商业贷款/企业自筹/多边机构 |
| `country_code` | CHAR(3) | NULL, `fk_source_country` → `t_country` | 多边机构可空 |
| `institution` | VARCHAR(100) | NULL | 具体机构 |
| `remark` | VARCHAR(200) | NULL | 备注 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 入库时间 |

索引：`idx_source_country(country_code)`。

### 2.2 `t_fund_plan` 年度资金计划（到位率的分母）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `plan_id` | BIGINT | PK, AUTO_INCREMENT | 计划主键 |
| `project_id` | BIGINT | NOT NULL, `fk_plan_project` → `t_project` | 所属项目 |
| `plan_year` | SMALLINT | NOT NULL, `ck_plan_year_range` | 2000–2100 |
| `plan_amount` | DECIMAL(18,2) | NOT NULL, `ck_plan_amount_positive` | 年度计划（万元） |
| `currency_code` | CHAR(3) | NOT NULL DEFAULT 'CNY' | 币种 |
| `approved_date` | DATE | NULL | 批复日期 |
| `remark` | VARCHAR(200) | NULL | 备注 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |

**`UNIQUE(project_id, plan_year)`（`uk_plan_project_year`）—— 一个项目一年只能有一条计划**，这是 upsert 的依据。

### 2.3 `t_fund_disbursement` 资金拨付（到位率的分子）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `disb_id` | BIGINT | PK, AUTO_INCREMENT | 拨付主键 |
| `project_id` | BIGINT | NOT NULL, `fk_disb_project` | 所属项目 |
| `source_id` | BIGINT | NOT NULL, `fk_disb_source` | 资金来源 |
| `plan_id` | BIGINT | NULL, `fk_disb_plan` ON DELETE SET NULL | 可空＝计划外拨付 |
| `disb_date` | DATE | NOT NULL | 拨付日期 |
| `disb_amount` | DECIMAL(18,2) | NOT NULL, `ck_disb_amount_positive` | 拨付金额（万元） |
| `currency_code` | CHAR(3) | NOT NULL DEFAULT 'CNY' | 币种 |
| `purpose` | VARCHAR(200) | NULL | 用途 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |

索引：`idx_disb_project`、`idx_disb_date`、`idx_disb_source`、`idx_disb_plan`（任务书 1.3 要求高频筛选列必须有索引）。

### 2.4 `t_progress_report` 履约进度月报

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `report_id` | BIGINT | PK, AUTO_INCREMENT | 报告主键 |
| `project_id` | BIGINT | NOT NULL, `fk_report_project` | 所属项目 |
| `period_ym` | CHAR(7) | NOT NULL, `ck_report_period_format` | `YYYY-MM` |
| `progress_pct` | DECIMAL(5,2) | NOT NULL, `ck_report_progress_range` | 0–100 |
| `on_site_staff` | INT | NOT NULL DEFAULT 0, `ck_report_staff_nonneg` | 现场用工人数 |
| `local_staff` | INT | NOT NULL DEFAULT 0, `ck_report_local_le_onsite` | 属地员工 ≤ 现场总人数 |
| `output_value` | DECIMAL(18,2) | NOT NULL DEFAULT 0, `ck_report_output_nonneg` | 本期产值（万元） |
| `report_date` | DATE | NULL | 填报日期 |
| `reporter` | VARCHAR(50) | NULL | 填报人 |
| `remark` | VARCHAR(200) | NULL | 备注 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |

**`UNIQUE(project_id, period_ym)`（`uk_report_project_period`）—— 同项目同月只能报一次**，DAO 把 1062 错误翻成“该项目 2025-06 已报过进度，请改为修改”。
索引：`idx_report_period(period_ym)`。

### 2.5 `t_report_indicator` 报告多值指标（EAV）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `report_id` | BIGINT | PK 之一, `fk_indicator_report` | 所属报告 |
| `indicator_code` | VARCHAR(20) | PK 之一 | TRAINING 培训人次 / LOCAL_SUPPLY 本地采购额 |
| `indicator_val` | DECIMAL(18,2) | NOT NULL, `ck_indicator_val_nonneg` | 本期值 |
| `unit` | VARCHAR(10) | NULL | 人次 / 万元 |

主键 `(report_id, indicator_code)`；索引 `idx_indicator_code`。
**给 C 的约定**：`indicator_val` 是“本期增量”，C 的 M7 承诺比对用 `SUM()` 累计，不要按“累计值”填，否则会重复计算。

### 2.6 `t_warning_rule` 预警规则（阈值存在表里，不写在代码里）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `rule_id` | BIGINT | PK, AUTO_INCREMENT | 规则主键 |
| `rule_code` | VARCHAR(20) | NOT NULL, `uk_rule_code` | F1/F2/F3/F4/F5 |
| `rule_name` | VARCHAR(60) | NOT NULL | 资金断档 / 资金沉淀 / … |
| `level_code` | CHAR(1) | NOT NULL, `ck_rule_level` | R 红 / Y 黄 / B 蓝 |
| `cond1_metric` | VARCHAR(20) | NOT NULL, `ck_rule_metric` | FUND_RATE / PROGRESS_PCT / OVERDUE_COUNT / LOCAL_HIRE_RATE / REPORT_GAP_MONTH |
| `cond1_op` | VARCHAR(4) | NOT NULL, `ck_rule_op` | LT / LE / GT / GE / EQ |
| `cond1_value` | DECIMAL(10,2) | NOT NULL | 阈值 1 |
| `cond2_metric` / `cond2_op` / `cond2_value` | 同上 | NULL | 条件 2，空表示单条件 |
| `is_enabled` | TINYINT(1) | NOT NULL DEFAULT 1 | 是否启用 |
| `message_tpl` | VARCHAR(200) | NOT NULL | 说明模板，`{v1}`/`{v2}` 替换成实际值 |
| `updated_at` | TIMESTAMP | ON UPDATE CURRENT_TIMESTAMP | |

两个条件按 **AND** 组合。这样 F1「到位率<60 且 进度<50」是数据，不是 `if`，改阈值不用改 Java。

### 2.7 `t_warning` 预警实例（刷新幂等的关键）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `warn_id` | BIGINT | PK, AUTO_INCREMENT | 预警主键 |
| `rule_id` | BIGINT | NOT NULL, `fk_warning_rule` | 触发规则 |
| `project_id` | BIGINT | NOT NULL, `fk_warning_project` | 涉及项目 |
| `period_ym` | CHAR(7) | NOT NULL | 预警期（取该项目最新报告期） |
| `warn_level` | CHAR(1) | NOT NULL, `ck_warning_level` | R/Y/B |
| `rule_name` | VARCHAR(60) | NOT NULL | 规则名快照 |
| `trigger_snapshot` | VARCHAR(200) | NULL | 触发时指标值快照 |
| `status` | VARCHAR(8) | NOT NULL DEFAULT 'OPEN', `ck_warning_status` | OPEN/ACK/CLOSED |
| `created_at` / `ack_at` / `ack_by` | TIMESTAMP/… | | 触发、确认信息 |
| `updated_at` | TIMESTAMP | ON UPDATE CURRENT_TIMESTAMP | |

**`UNIQUE(rule_id, project_id, period_ym)`（`uk_warning_rule_project_period`）—— 刷新幂等的数据库级保证**：
`INSERT ... ON DUPLICATE KEY UPDATE` 连点 10 次刷新也只有一份数据。
索引：`idx_warning_level`、`idx_warning_status`、`idx_warning_project`。

---

## 3. B 的 SQL / DAO 依赖 A 的这些列（请 A 冻结前对一眼）

B 所有查询只读 A 的两张表，列清单如下（**列名一旦改动，B 只需改第 4 节列的那几处**）：

| A 的表 | B 用到的列 | 用途 |
| --- | --- | --- |
| `t_project` | `project_id`、`project_code`、`project_name`、`country_code`、`status`、`investment_amount` | 所有统计的分组键与展示列 |
| `t_country` | `country_code`、`country_name` | 视图 `v_fund_progress` 的国别列 |
| `t_project.status` | 取值必须含 **`在建`**、**`竣工`** | `v_fund_rate` / `v_fund_progress` 按状态过滤 |
| `t_project.investment_amount` | DECIMAL，单位**万元** | 种子数据按它推导计划/拨付/产值 |

两个约定（B 已验证过，改了对不上）：

1. `t_project.status` 只用字典里的标准值 `规划 / 在建 / 竣工 / 停工`，`v_fund_progress` 只取 `在建`，`v_fund_rate` 取 `在建 + 竣工`；
2. `investment_amount` 允许 NULL（B 的种子脚本对 NULL 用 80000 万元兜底），单位必须是万元。

---

## 4. 万一 A 改了列名：B 需要同步的位置（共 8 处）

| # | 文件 | 位置 | 内容 |
| --- | --- | --- | --- |
| 1 | `sql/05_view_b_fund.sql` | `v_fund_rate` | `p.project_code / p.project_name / p.status` |
| 2 | `sql/05_view_b_fund.sql` | `v_fund_progress` | `c.country_name`、`p.country_code` |
| 3 | `sql/04_seed_business.sql` | 第 3、4、5 段 | `t_project.investment_amount`、`project_id` |
| 4 | `dao/FundDao.java` | `SQL_RATE` / `SQL_DISB_SELECT` | `p.project_code / p.project_name` |
| 5 | `dao/ReportDao.java` | `SQL_SELECT` / `statOutputByCountry` | `p.project_code / p.project_name / p.country_code` |
| 6 | `dao/ReportDao.java` | `findLowLocalHireRate` / `findStagnantProjects` | 同上 |
| 7 | `dao/WarningDao.java` | `findByLevel` / `findByProject` | `p.project_code / p.project_name / c.country_name` |
| 8 | `dao/FundPlanDao.java` | `SQL_SELECT` | `p.project_code / p.project_name` |

---

## 5. 种子数据的分工边界（B 只写自己那一段）

`sql/04_seed_business.sql` 现在只有 **B 段**（资金 / 报告 / 指标 / 规则）。C 的里程碑、承诺、成效数据请 C 追加在文件末尾，用注释块分隔：

```sql
-- =====================================================================
-- C 段：里程碑 / 承诺 / 成效（C 负责，B 不动这一段）
-- =====================================================================
```

B 段对 A 的依赖只有一个硬条件：**`t_project` 里前 40 条项目必须存在**（脚本开头有守卫，不满足会直接报错，不会插进一堆错位数据）。如果 A 的项目主键不是 1–40，只需改 `04_seed_business.sql` 开头的 `SET @pid_base = ...`。
