# B 段合并说明（A 执行）

> 合并时间：2026-10-24　执行人：A　来源：`bri-pm-B段交付-20261024.zip`（B 的 `deepseek工作区`）
> 结论：**B 的 7 张表 DDL、04 种子、05 视图、7 个 DAO、15 个 VO、3 份文档已并入本仓库**；
> B 的 `README.md`、`sql/README.md`、`common/*`、`pom.xml`、`db.properties`、`sql/dev/` 未采用（见第 3 节）。

## 1. 冲突是怎么解决的（一句话）

B 自己写了一套 M4/M6/M8 的 7 张表 DDL，和 A 已冻结的 24 张表**列名/类型/取值都不一样**。合并规则定为：

| 项目 | 决定 | 理由 |
| --- | --- | --- |
| M4/M6/M8 的 7 张表结构 | **以 B 的 DDL 为准**，并入 `01_schema.sql` | 这 7 张表本来就是 B 的模块，B 的 DAO/SQL/自检全按他的列名写，改他的代码面更大、易错 |
| A 的 12 张表 + C 的 5 张表 | 保持 A 的冻结版不动 | 已上传 GitHub，D 已按它写界面、C 已按它准备视图 |
| 金额单位 | **统一为“元”** | A 的 `t_project.invest_amount` 是元；B 原来按“万元”设计，会导致 10000 倍口径错位 |
| 表数量 | 仍是 **24 张**（B 的 7 张替换 A 原来的同名 7 张） | `SHOW TABLES` 数量不变 |

## 2. 具体改了什么（逐条可核对）

| # | 文件 | 改动 | 原因 |
| --- | --- | --- | --- |
| 1 | `sql/01_schema.sql` | 用 B 的 DDL 替换 `t_funding_source / t_fund_plan / t_fund_disbursement / t_progress_report / t_report_indicator / t_warning_rule / t_warning` 七张表 | 与 B 的代码对齐 |
| 2 | 同上：`t_progress_report` | 在 B 的版本上**补回** `safety_events`、`quality_events` | 任务书 M6 界面要求“事件数”，B 原版没有；补回后 B 的 INSERT 不写这两列，走 `DEFAULT 0`，不影响 |
| 3 | 同上：金额注释 | `plan_amount / disb_amount / output_value` 的注释由“万元”改为“元” | 统一单位，避免答辩被问“到底哪个单位” |
| 4 | 同上：表选项 | `ENGINE = InnoDB DEFAULT CHARSET = utf8mb4` → `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`；去掉 `IF NOT EXISTS` | 与 A 的 24 张表风格一致（脚本开头已 DROP，不需要 IF NOT EXISTS） |
| 5 | `sql/04_seed_business.sql` | `t.investment_amount` → `t.invest_amount`（4 处）；`COALESCE(..., 80000)` → `COALESCE(..., 800000000)`（8 亿元兜底）；注释单位改“元” | A 的项目表列名是 `invest_amount`、单位是元 |
| 6 | `sql/02_dict_and_user.sql` | `FUND_SOURCE_TYPE` 字典项改为 B 的 5 类：政府援助 / 政策性银行 / 商业贷款 / 企业自筹 / 多边机构 | 与 `t_funding_source.source_type` 的 CHECK、B 的种子数据一致 |
| 7 | 同上 | `WARNING_LEVEL` 字典项由 `RED/YELLOW/BLUE` 改为 **`R/Y/B`** | 与 B 的 `level_code CHAR(1)`、`t_warning.warn_level` 一致 |
| 8 | `sql/05_views_fund.sql` | 由 B 的 `05_view_b_fund.sql` 更名并入 | A 的 `sql/README.md` 早已约定这个文件名 |
| 9 | `sql/99_verify_b.sql` | 原样并入 | B 的验收自检 |
| 10 | `src/main/java/com/bri/dao/` | `FundDao / FundingSourceDao / FundPlanDao / ReportDao / ReportIndicatorDao / WarningDao / WarningRuleDao` 7 个类 | 编译目录 `_build/classes` 证明 B 已编译通过 |
| 11 | `src/main/java/com/bri/vo/` | 15 个 VO（DisbVO、FundPlanVO、ReportVO、MoMRow、WarningVO…） | 与 A 的 14 个 VO 无重名 |
| 12 | `src/main/java/com/bri/modB/` | `BSelfTest` + `package-info` | B 的控制台自检 |
| 13 | `docs/` | `B-1-字段清单与A侧依赖.md`、`B-7-验证与演示脚本.md`、`B-7-自检输出原文.txt` | 交付文档 |

## 3. 没有采用的文件（不要上传）

| B 的文件 | 为什么不采用 |
| --- | --- |
| `sql/01_schema_b_fund.sql` | 内容已并入 `01_schema.sql`，再单独执行会与已存在的表冲突（它是 `IF NOT EXISTS`，不会报错但会让人以为表是它建的）；保留在 B 本地作历史即可 |
| `sql/dev/dev_only_00_stub_A_tables.sql` | B 本地联调用的“替身表”，会 `DROP TABLE t_project/t_country/t_region`，**绝对不能在有数据的库执行** |
| `pom.xml`、`db.properties`、`src/main/resources/db.properties`、`.gitignore` | 与 A 的逐字节一致（已校验哈希） |
| `src/main/java/com/bri/common/*`、4 个测试类、`dao/package-info`、`vo/package-info` | 同上，与 A 的一致 |
| `README.md`、`sql/README.md` | 是 B 拉取的旧版（还是“14 张表 / 待提交”），A 的版本更新 |
| `lib/`、`_build/`、`_logs/`、`bri-pm-B段交付-*.zip` | 依赖 jar、编译产物、日志，不进仓库 |

## 4. B 需要复核的 8 个点

合并后请 B 在**空库**上按顺序重跑，并确认 `BSelfTest` 仍全绿：

```bash
mysql -uroot -p --default-character-set=utf8mb4 -e "DROP DATABASE IF EXISTS bri_pm; CREATE DATABASE bri_pm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
for %f in (01_schema 02_dict_and_user 03_seed_basic 04_seed_business 05_views_fund 99_verify_b) do (
    mysql -uroot -p --default-character-set=utf8mb4 < sql\%f.sql
)
mvn -q clean compile && java -cp "target/classes;lib/*" com.bri.modB.BSelfTest
```

| # | 复核点 | 预期 |
| --- | --- | --- |
| 1 | `SHOW TABLES` | **24 张**（不是 B 本地联调时的 10 张） |
| 2 | 04 脚本执行 | 零报错；`资金来源 9 / 计划 120 / 拨付 240 / 月报 240 / 指标 480 / 规则 5` |
| 3 | 金额口径 | 计划/拨付量级是 **十亿级（元）**，不再是“万元”；`Q4-4` 打印的数字变大是正常的 |
| 4 | 数据量断言 | B 原自检里“到位率<60% 的项目 12 个”等**具体条数会变**（因为项目主数据换成了 A 的 40 个真实项目，状态分布不同：在建 22 个而不是 25 个） |
| 5 | 在建项目数 | 变成 **22**（A 的 03 里在建 22 个，另 7 竣工 / 7 已移交 / 2 暂停 / 2 前期）→ `v_fund_progress` 行数 = 22 |
| 6 | `v_fund_rate` | 覆盖 在建+竣工 = **29** 个项目 |
| 7 | 字典下拉 | 资金来源类型变成 5 类（政府援助/政策性银行/商业贷款/企业自筹/多边机构），预警级别是 R/Y/B |
| 8 | `t_progress_report` | 多了 `safety_events`、`quality_events` 两列（B 的 INSERT 不写，默认 0） |

## 5. 合并后 B 的 SQL 兼容性（A 已静态校验）

逐项核对过，B 的 SQL/DAO 与合并后的 24 张表**列名全部命中**：

- `04_seed_business.sql` 里所有 `INSERT ... (列)` 的列都存在；
- `05_views_fund.sql` 引用的 `plan_amount / disb_amount / status / progress_pct / period_ym / country_name` 都存在；
- B 的 22 个 Java 文件里，SQL 中的 `别名.列名` 都能在表结构里找到；
- 已经消失的旧列名（`investment_amount`、`warning_id`、`status_code`、`RED/YELLOW/BLUE`）在 B 的代码里**一次都没出现**（那是我原来 24 张表里的写法，已被 B 的版本替代）。

## 6. 答辩时值得讲的三个点（B 的贡献）

1. **任务书 2.3 的到位率示例 SQL 是错的**：`LEFT JOIN 计划 LEFT JOIN 拨付 ON d.plan_id = pl.plan_id` 在一个计划有多笔拨付时会把分母重复计数（B 的数据上算出 26.26%，真实 52.52%，正好差 2 倍）。B 改成“先按项目聚合再 JOIN”，这是“聚合顺序影响口径”的典型案例。
2. **预警是规则表驱动**：`cond1_metric/op/value + cond2_*` 两个条件用 AND 组合，改阈值只改数据不改代码；`refreshWarnings()` 用 `UNIQUE(rule_id, project_id, period_ym)` + `ON DUPLICATE KEY UPDATE` 保证幂等（B 自检里连点两次刷新，条数不变）。
3. **同项目同月重复报工**由 `UNIQUE(project_id, period_ym)` 挡住，DAO 把 1062 错误翻译成“该项目 2025-06 已报过进度，请改为修改”，这是“数据库约束 + 中文提示”的配合。
