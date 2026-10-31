# C-说明 · M5 里程碑 / M7 履约承诺比对 / M9 成果与思政报表

> 交付人：C　对应任务书：M5 进度线（里程碑）、M7 复杂关联（履约承诺比对）、M9 成果与思政报表
> 数据库：MySQL 8.0 / InnoDB / utf8mb4（表与 A 的 01_schema.sql 已并库）
> 约定：只新增文件、不改 A/B 的文件；C 段种子只写在 `04_seed_business.sql` 的 C 段注释块下。

---

## 1. 我交了哪些文件（对照任务书）

| 任务书编号 | 交付物 | 文件 | 状态 |
| --- | --- | --- | --- |
| C-1 | 里程碑字典 + 项目里程碑种子 | `sql/04_seed_business.sql`（C 段） | ✅ 字典 6 条 / 里程碑 160 条 |
| C-2 | 履约承诺 + 成效种子 | 同上（C 段） | ✅ 承诺 136 条 / 成效 40 条 |
| C-3 | M5 里程碑 DAO | `dao/MilestoneDao.java` | ✅ 含 Q5-1 ~ Q5-4 |
| C-4 | M7 履约承诺比对 DAO + 视图 | `dao/CommitmentDao.java`、`sql/06_views_commitment.sql` | ✅ 含 Q7-1 ~ Q7-4 |
| C-5 | M9 成效 DAO + 三条思政查询 | `dao/OutcomeDao.java` | ✅ 含思政 1/2/3 |
| C-6 | VO | `vo/` 下 MilestoneVO、DelayRow、DurationRow、AchievementRateRow、CommitVO、CompareVO、ContraRow、ReconRow、CommitRateRow、OutcomeVO、RegionOutRow、GreenRow | ✅ |
| C-7 | DAO 验证与演示脚本 | `modC/CSelfTest.java`（本文档） | ✅ 现场可跑 |

**没有写的东西（守规矩，不越界）：**

- 不建窗口类（界面全在 `com.bri.ui`，D 负责）；C 的控制台自检放在 `com.bri.modC`，与 B 的 `com.bri.modB` 对称；
- 不改 `pom.xml`、`db.properties`、`README.md`、`01_schema.sql`、`02/03`、`05`（都是 A/B 的文件）；
- 不动 `04_seed_business.sql` 里 B 的段落（B 段保持 TODO 占位，等 B 补资金/报告/预警种子）；
- 不新建 `RateRow`/`HireRow`：承诺维度履约率用 `CommitRateRow`、领域里程碑达成率用 `AchievementRateRow`，
  思政 2 直接复用 B 的 `HireRow`，避免与 B 的同名类冲突。

---

## 2. 怎么跑（三条命令）

```bash
# 0) 前置：A 的 01/02/03 已执行（库名 bri_pm），且 04 C 段、06 视图已执行
mysql --default-character-set=utf8mb4 -uroot -p bri_pm < sql/04_seed_business.sql
mysql --default-character-set=utf8mb4 -uroot -p bri_pm < sql/06_views_commitment.sql

# 1) DAO 控制台自检（不需要界面）
mvn -q clean package
java -cp "target/bri-pm.jar" com.bri.modC.CSelfTest
```

`04_seed_business.sql` 末尾留了自检 SQL（手动 `SELECT COUNT(*)` 核对）：

```text
表              行数
里程碑字典      6
项目里程碑      160
履约承诺        136
项目成效        40
承诺比对结果    0          ← 由 calcAll() 计算，不在种子里造
```

---

## 3. 种子数据设计（确定性，不用 RAND()）

所有“随机感”数据都用 `project_id` 的取模公式生成，重复执行结果完全一致，答辩时能解释每一行。

### 3.1 里程碑字典（6 条，固定主键 1..6）

| milestone_id | code | 名称 | seq_no | planned_days |
| --- | --- | --- | --- | --- |
| 1 | START | 开工 | 1 | NULL |
| 2 | STRUCT | 主体结构 | 2 | 180 |
| 3 | EQUIP | 设备安装 | 3 | 360 |
| 4 | POWER | 带电通车 | 4 | 540 |
| 5 | TURNOVER | 竣工移交 | 5 | 720 |
| 6 | TRIAL | 试运行 | 6 | 900 |

> 注意：任务书 6 个名称里是 **竣工移交 / 试运行**，不是“开通运营”。代码里也做了断言兜底。

### 3.2 项目里程碑（40 项目 × 4 节点 = 160 条）

每个项目只排 **seq 1~4**（开工/主体结构/设备安装/带电通车），计划日期 = 基准日 + `planned_days`；
实际日期用 `project_id` 取模制造三种状态（供生成列 `is_delayed` 演示）：

- 开工/主体结构：全部完成，`project_id % 3 != 0` 的按 15/30 天超期；
- 设备安装：**偶数**项目完成，其余 `actual_date` 为 NULL（未完成）；
- 带电通车：`project_id % 5 = 0` 的项目完成，其余 NULL。

结果：**总 160 条（≥150）**，`actual_date` 为空 **52 条（≥20）**，超期 **70 条（≥15）**。

### 3.3 履约承诺（四类，共 136 条）

| 承诺类型 | 数量 | 目标值 | 比较方向 | 基准日 |
| --- | --- | --- | --- | --- |
| 工期 | 38 | 计划工期天数 | `<=` | 开工日（缺省签约日）；`project_id % 4 == 0` 时置 NULL |
| 金额 | 40 | `投资额 × 0.6` | `>=` | NULL（不参与工期口径） |
| 属地用工 | 29（在建+竣工） | 60% | `>=` | NULL |
| 培训 | 29（在建+竣工） | 1000 人次 | `>=` | NULL |

**故意造成待评估**：`project_id % 4 == 0` 的“工期”承诺 `base_date = NULL`（基准日未定），
视图里 `DATEDIFF(实际完成日, NULL) = NULL` → `calcAll()` 首判 NULL → “待评估”。
有 `planned_end_date` 的这类项目是 **4/8/12/16/20/24/28/32/36 共 9 条（≥8）**（project 40 是前期、无计划竣工日，不参与）。

> `t_commitment_result` 与 `t_warning` **不手工 INSERT**：结果表由 `calcAll()` 算，预警表由 B 的 `WarningDao` 刷新。

### 3.4 项目成效（40 条，与项目 1:1）

- 受益人口、就业岗位、培训人次用 `project_id` 取模生成；
- 绿色装机只给 **能源/通信/园区** 领域项目，其余为 NULL（思政 3 据此按领域分组）。

---

## 4. M7 的四个难点（答辩核心，对应 06_views_commitment.sql）

| 难点 | 做法 | 代码位置 |
| --- | --- | --- |
| 多源对齐 | `v_commitment_actual` 用 CASE 把 3 张不同粒度表的实际值统一到“承诺”粒度 | 视图 `v_commitment_actual` |
| 三值逻辑 | 未拨付/未填报时 `actual_value` 返回 **NULL（不是 0）**，`calcAll()` 首判 NULL → 待评估 | 视图金额/培训分支 + `calcAll()` 的 CASE |
| 除零保护 | 属地用工用 `NULLIF(SUM(on_site_staff), 0)`，履约率/达成率用 `NULLIF(COUNT(*), 0)` | 视图 + 各统计 SQL |
| 基准日 | 工期承诺把基准日存成 `base_date` 字段，不硬编码 | `t_commitment.base_date` |

`calcAll()` 的结论判定顺序（NULL 必须先判，顺序不能反）：

```text
实际值为 NULL            → 待评估
满足比较方向             → 已履约
20% 容差内              → 部分履约
其余                    → 未履约
```

幂等：`INSERT ... ON DUPLICATE KEY UPDATE`（`commit_id` 唯一键），连点不翻倍。

---

## 5. 自检项清单（CSelfTest）

1. 种子数据量：字典 6、里程碑 160（≥150）、NULL 52（≥20）、超期 70（≥15）、承诺 136（≥80）、待评估候选 9（≥8）、成效 40；
2. M5 四类查询：Q5-1 平均超期、Q5-2 超期未完成、Q5-3 工期对比、Q5-4 领域达成率；
3. M5 写操作：补排“竣工移交”节点（upsert）、补录“设备安装”实际日期 → 生成列 `is_delayed` 自动变 true；
4. M7：`calcAll()` 算满全表（结果条数 = 承诺条数），结论分布含 已履约 + 待评估；
5. M7：Q7-1 项目承诺清单、按“待评估”筛选、Q7-2 分类型履约率、Q7-3 交叉矛盾、Q7-4 国别对账；
6. M9：成效详情 + 思政 1（大洲汇总）+ 思政 2（双达标）+ 思政 3（绿色装机）。

运行结束打印 `通过 N 项，失败 M 项`，`fail > 0` 时 `System.exit(1)`。

---

## 6. 答辩三问（预演）

### 6.1 “超期判定为什么交给数据库生成列，而不是 Java 里算？”

1. **唯一真源**：`is_delayed` 是 `STORED` 生成列，`actual_date` 一改它自动跟着变，任何写入路径（界面补录、脚本导入）都绕不开它，不会出现“Java 忘了更新标记”的脏数据；
2. **少写代码**：Java 只 `rs.getObject("is_delayed")` 读，不用每个查询都重复 `actual > plan` 的 CASE；
3. **可解释**：`SHOW CREATE TABLE t_project_milestone` 一眼能看到生成列表达式，三态（1 超期 / 0 准时 / NULL 未完成）语义清晰。

### 6.2 “履约比对为什么要在 SQL 里做，而不是 Java 里循环？”

1. **口径唯一 + 一次算完**：四类承诺的“实际值”来自三张不同粒度的表，`v_commitment_actual` 把对齐 + 三值逻辑 + 除零保护收敛到一处，`calcAll()` 一条 `INSERT ... SELECT` 算满 136 条；Java 循环要 4 类 × 多趟往返；
2. **三值逻辑必须一致**：未拨付/未填报必须判成“待评估”而不是“未履约”，放 SQL 里 `WHEN actual IS NULL THEN '待评估'` 写在最前，全系统唯一；
3. **幂等免费拿**：结果表 `commit_id` 唯一键 + `ON DUPLICATE KEY UPDATE`，重复计算不翻倍。

### 6.3 “待评估是怎么‘故意’造出来的？为什么不是未履约？”

- 工期承诺的 `base_date` 对 `project_id % 4 == 0` 的项目置 **NULL**（“基准日未定”的业务场景）；
- 视图里 `DATEDIFF(实际完成日, NULL) = NULL`，`calcAll()` 第一个分支就是 `WHEN actual IS NULL → 待评估`；
- **语义区分**：NULL 是“不知道”，不是“没达标”。把 NULL 当成 0 会把“还没拨付”误判成“违约”，这是三值逻辑的关键。

---

## 7. 给其他三人的对接说明

### 7.1 给 D（界面）

D 只调 DAO，不写 SQL。C 提供的入口：

| DAO | 方法 | 返回 | 界面用途 |
| --- | --- | --- | --- |
| `MilestoneDao` | `findMilestonesByProject(long)` | `List<MilestoneVO>` | M5 里程碑列表 |
| `MilestoneDao` | `upsertMilestonePlan(MilestoneVO)` | `Result<Long>` | 保存/调整计划日期 |
| `MilestoneDao` | `recordActualDate(long, LocalDate)` / `recordActualByType(long, int, LocalDate)` | `Result<Void>` | 补录实际完成日期 |
| `MilestoneDao` | `statAvgDelayByMilestone()` | `List<DelayRow>` | Q5-1 平均超期 |
| `MilestoneDao` | `findOverdueMilestones()` | `List<DelayRow>` | Q5-2 超期未完成 |
| `MilestoneDao` | `compareProjectDuration()` | `List<DurationRow>` | Q5-3 工期对比 |
| `MilestoneDao` | `statMilestoneRateByField()` | `List<AchievementRateRow>` | Q5-4 领域达成率 |
| `CommitmentDao` | `findCommitments(long)` | `List<CommitVO>` | M7 某项目承诺清单 |
| `CommitmentDao` | `calcAll()` | `int` | “重新计算”按钮 |
| `CommitmentDao` | `findByStatus(String)` | `List<CompareVO>` | 按结论筛选（传 已履约/部分履约/未履约/待评估） |
| `CommitmentDao` | `statRateByType()` | `List<CommitRateRow>` | Q7-2 分类型履约率 |
| `CommitmentDao` | `findCrossContradiction()` | `List<ContraRow>` | Q7-3 交叉矛盾 |
| `CommitmentDao` | `statCountryReconcile()` | `List<ReconRow>` | Q7-4 国别对账 |
| `OutcomeDao` | `findByProject(long)` | `OutcomeVO` | M9 成效详情 |
| `OutcomeDao` | `statByRegion()` | `List<RegionOutRow>` | 思政 1 大洲汇总 |
| `OutcomeDao` | `findHighLocalHire(double, int)` | `List<HireRow>` | 思政 2 双达标（复用 B 的 HireRow） |
| `OutcomeDao` | `statGreenByField()` | `List<GreenRow>` | 思政 3 绿色装机 |

界面小提示（省 D 的时间）：

- `MilestoneVO.isDelayed` 为 `null` 表示未完成，显示“—”，不要显示“未超期”；
- `CommitVO.actualValue` / `gapValue` 为 `null` 时结论是“待评估”，`commitStatus` 已给出中文；
- `CommitRateRow.ratePct` / `AchievementRateRow.ratePct` 为 `null` 表示分母为 0（NULLIF 除零保护），显示“—”；
- 里程碑补录后要刷新列表，`is_delayed` 会由生成列自动更新。

### 7.2 给 B（资金/报告/预警）

- 依赖约定已按 B-7 第 6.3 节对齐：`t_project_milestone` 的 `plan_date`/`actual_date` 列名不变，B 的 F3「超期里程碑 ≥3」会自动读到 C 的数据；
- M7 的“培训”承诺读 `t_report_indicator.indicator_code = 'TRAINING'` 的 `SUM(indicator_val)`，累计口径与 B 一致；
- “金额”承诺读 `t_fund_disbursement` 的 `SUM(disb_amount)`，“属地用工”读 `t_progress_report` 的 `SUM(local_staff)/SUM(on_site_staff)`；
- **B 的数据入库前**，这三类承诺的 `actual_value` 会因无数据返回 NULL → 全判“待评估”，这不是 bug；B 的 240 拨付/240 月报/480 指标一到位，结论自动变为 已履约/部分履约/未履约，C 的代码一行都不用改。

### 7.3 给 A（建表/种子）

- C 的 5 张表（`t_milestone`/`t_project_milestone`/`t_commitment`/`t_commitment_result`/`t_project_outcome`）已在 `01_schema.sql` 建全，C 不改动；
- `sql/README.md` 建议加两行（若还没加）：

```text
| `06_views_commitment.sql` | C 段视图 v_commitment_actual / v_commitment_result | C | 已提交 |
| `04_seed_business.sql`    | 业务种子（B 资金报告 + C 里程碑承诺成效）          | B+C | 各自维护段落 |
```

---

## 8. 已知限制与风险（先讲清楚）

| # | 限制 | 影响 | 处理 |
| --- | --- | --- | --- |
| 1 | B 的种子数据在 `04_seed_business.sql` 里还是 TODO 占位 | M7 的金额/属地用工/培训三类承诺、思政 2 在 B 数据入库前会全判“待评估”/为空 | 不是 C 的 bug；B 数据到位后自动恢复，见 7.2 |
| 2 | `CSelfTest` 第 3 段会真的补排/补录一条里程碑 | 会把 1 号项目从 4 节点补到 5 节点、给“设备安装”写实际日期 | 幂等可重复执行；要复原重跑 04 C 段即可 |
| 3 | 本机若没装 Maven，`mvn` 命令跑不了 | 无法打包验证 | 代码只依赖 `java.sql` + `com.bri.common`，可用 `javac --release 17` 编译（同 B 的处理方式） |
| 4 | 金额单位沿用 A 的“元”（`invest_amount` 是元） | 若 A 用“万元”，金额承诺目标值差 10000 倍 | 已按 `01_schema.sql` 的 `invest_amount DECIMAL` 口径写，用 `ROUND(invest_amount * 0.6, 2)` |

---

## 9. C 的完成定义自查（任务书 2.6）

- [x] 里程碑字典 6 条名称正确，项目里程碑 ≥150 条、未完成 ≥20、超期 ≥15（3.1/3.2 有数据）；
- [x] 履约承诺 ≥80 条、四类齐全、故意造成 ≥8 条待评估（3.3）；
- [x] M5 四类查询 + M7 `calcAll()` 四档结论 + M9 三条思政查询能现场跑出结果（CSelfTest）；
- [x] `t_commitment_result` / `t_warning` 不手工 INSERT，由 `calcAll()` / `WarningDao` 生成；
- [x] 未拨付/未填报返回 NULL（不是 0），NULLIF 除零保护全覆盖；
- [x] 每个 DAO 都有控制台验证记录（`CSelfTest`，输出可直接截图）。
