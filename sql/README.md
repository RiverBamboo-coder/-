# sql/ 目录说明（A 统一维护脚本顺序，B/C 只往自己段落加）

## 1. 脚本清单

| 脚本 | 内容 | 负责人 | 状态 |
| --- | --- | --- | --- |
| `01_schema.sql` | 建库 + **24 张表** + 主键/外键/唯一/检查约束 + 索引（含 M5 生成列） | A | ✅ 已提交（第 2 周，G1 闸口） |
| `02_dict_and_user.sql` | 字典 10 类 46 条 + 5 个演示用户（口令 SHA-256） | A | ✅ 已提交 |
| `03_seed_basic.sql` | 区域 21 / 国家 61 / 企业 35 / 联合体 5 + 成员 14 / 项目 40 / 合同 80 / 参与记录 100 / 状态轨迹 101 | A | ✅ 已提交 |
| `04_seed_business.sql` | 资金/预警（B 段）+ 里程碑/承诺/成效（C 段） | B、C | 待提交 |
| `05_views_fund.sql` | 视图 `v_fund_rate`、`v_fund_progress` | B | 待提交 |
| `06_views_commitment.sql` | 视图 `v_commitment_actual` | C | 待提交 |
| `99_verify.sql` | 验收自检 SQL（每人往自己段落追加，A 汇总） | 全员 | 待提交 |

> 视图单独成文件（05/06），是为了让 B、C 各改各的，不和 A 的 `01_schema.sql` 抢同一个文件。

## 2. 表清单（24 张，按模块归属）

| 模块 | 表 | 负责人 |
| --- | --- | --- |
| M0 | `t_region`、`t_dict_type`、`t_dict_item`、`t_user` | A |
| M1 | `t_country` | A |
| M2 | `t_project`、`t_contract`、`t_project_status_log` | A |
| M3 | `t_enterprise`、`t_consortium`、`t_consortium_member`、`t_project_participant` | A |
| M4 | `t_funding_source`、`t_fund_plan`、`t_fund_disbursement` | B |
| M5 | `t_milestone`、`t_project_milestone` | C |
| M6 | `t_progress_report`、`t_report_indicator` | B |
| M7 | `t_commitment`、`t_commitment_result` | C |
| M8 | `t_warning_rule`、`t_warning` | B |
| M9 | `t_project_outcome` | C |

> 任务书 1.3 的 14 个编号项是“第一批核心表”，其中第 3 项是 `t_dict_type` + `t_dict_item` 两张表；B、C 的 9 张表如果不一起建出来，B/C 第 3 周无法写 DAO，所以 `01_schema.sql` 一次建全 24 张。

## 3. 执行顺序（有外键依赖，不能乱）

```text
01_schema.sql → 02_dict_and_user.sql → 03_seed_basic.sql → 04_seed_business.sql → 05_views_fund.sql → 06_views_commitment.sql → 99_verify.sql
```

三个初始化脚本都可重复执行（先删后插），演示用户见 `02_dict_and_user.sql` 末尾：`admin / planner / fund / field / viewer`，口令统一 `123456`。

## 4. 建库（库名必须与 `db.properties` 一致）

`01_schema.sql` 已包含建库语句（`CREATE DATABASE IF NOT EXISTS bri_pm`），一般不需要手工建库。
如果要彻底重建，可以先执行：

```sql
DROP DATABASE IF EXISTS bri_pm;
CREATE DATABASE bri_pm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

## 5. Windows 下执行脚本

PowerShell 不支持 `<` 输入重定向，用下面任一方式：

```bat
:: 方式一：在 cmd 中执行
mysql -uroot -p --default-character-set=utf8mb4 < sql\01_schema.sql
```

```bat
:: 方式二：先进 mysql 客户端，再 source（路径用正斜杠）
mysql -uroot -p --default-character-set=utf8mb4
```

```sql
mysql> source D:/数据库三级项目/bri-pm/sql/01_schema.sql;
```

一条命令跑完全部脚本（cmd）：

```bat
for %f in (01_schema 02_dict_and_user 03_seed_basic 04_seed_business 05_views_fund 06_views_commitment 99_verify) do (
    mysql -uroot -p --default-character-set=utf8mb4 < sql\%f.sql
)
```

## 6. 写脚本的硬性要求

1. 每个脚本开头 `SET NAMES utf8mb4;`，建表一律 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`；
2. 表顺序严格遵守外键依赖（`01_schema.sql` 已按此顺序排好，改表时不要打乱）：`t_region → t_country →
   t_dict_type → t_dict_item → t_user → t_enterprise → t_consortium → t_consortium_member → t_project →
   t_contract → t_project_status_log → t_project_participant → t_funding_source → t_fund_plan →
   t_fund_disbursement → t_milestone → t_project_milestone → t_progress_report → t_report_indicator →
   t_commitment → t_commitment_result → t_warning_rule → t_warning → t_project_outcome`；
3. 约束统一命名：外键 `fk_表_被引用表`、唯一键 `uk_表_语义`、检查约束 `ck_表_语义`（答辩要 `SHOW CREATE TABLE` 逐张看）；
4. 每个外键列都要有索引；高频筛选列（`status`、`sign_date`、`disb_date`、`period_ym`）必须建索引；
5. 种子数据脚本要可重复执行：`DELETE FROM` 后 `INSERT`，或固定主键 + `INSERT ... ON DUPLICATE KEY UPDATE`；
6. 字符集、时序、外键顺序由 A 统一合并，B/C 不各自建库、不改别人的段落；
7. 数据真实性：项目名参考商务部公开信息与真实工程（蒙内铁路、雅万高铁、中老铁路、瓜达尔港、巴西美丽山特高压等），
   文档中标注为“教学示例数据”。
