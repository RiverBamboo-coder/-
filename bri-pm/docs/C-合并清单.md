# C 段合并清单（C 的交付 → bri-pm 工程）

> 用途：提交 Git 时逐条核对；也便于答辩时说明“谁交了什么”。

> 来源：`--main.zip`（C 提交的合并包）。合并时已剔除包内 8 个 `(2)` 重复副本。


## 1. 新增文件（19 个，全部是新增，不改动 A/B 的文件）

| # | 文件 | 字节 | 行数 | 说明 |
| --- | --- | ---: | ---: | --- |
| 1 | `src/main/java/com/bri/dao/MilestoneDao.java` | 15702 | 331 | M5：里程碑列表/计划保存/实际补录 + Q5-1~Q5-4 |
| 2 | `src/main/java/com/bri/dao/CommitmentDao.java` | 13943 | 276 | M7：calcAll() 四档结论 + Q7-1~Q7-4（本任务书最高优先级） |
| 3 | `src/main/java/com/bri/dao/OutcomeDao.java` | 8956 | 191 | M9：成效详情 + 思政 1/2/3 |
| 4 | `src/main/java/com/bri/vo/MilestoneVO.java` | 2825 | 127 | M5 里程碑列表（isDelayed 三态） |
| 5 | `src/main/java/com/bri/vo/DelayRow.java` | 3077 | 130 | M5 Q5-1/Q5-2 超期统计 |
| 6 | `src/main/java/com/bri/vo/DurationRow.java` | 2643 | 121 | M5 Q5-3 计划 vs 实际工期 |
| 7 | `src/main/java/com/bri/vo/AchievementRateRow.java` | 1793 | 72 | M5 Q5-4 领域达成率 |
| 8 | `src/main/java/com/bri/vo/CommitVO.java` | 3322 | 147 | M7 某项目承诺清单 |
| 9 | `src/main/java/com/bri/vo/CompareVO.java` | 2546 | 113 | M7 按结论筛选 |
| 10 | `src/main/java/com/bri/vo/ContraRow.java` | 1853 | 89 | M7 Q7-3 交叉矛盾 |
| 11 | `src/main/java/com/bri/vo/ReconRow.java` | 1825 | 90 | M7 Q7-4 国别对账 |
| 12 | `src/main/java/com/bri/vo/CommitRateRow.java` | 1509 | 61 | M7 Q7-2 分类型履约率 |
| 13 | `src/main/java/com/bri/vo/OutcomeVO.java` | 2191 | 102 | M9 项目成效详情 |
| 14 | `src/main/java/com/bri/vo/RegionOutRow.java` | 1124 | 57 | M9 思政 1 大洲汇总 |
| 15 | `src/main/java/com/bri/vo/GreenRow.java` | 1187 | 57 | M9 思政 3 绿色装机 |
| 16 | `src/main/java/com/bri/modC/CSelfTest.java` | 17733 | 341 | modC 控制台自检（不依赖界面，证明 DAO 逻辑通） |
| 17 | `src/main/java/com/bri/modC/package-info.java` | 628 | 17 | modC 包注释（与 modB 对称） |
| 18 | `sql/06_views_commitment.sql` | 4589 | 87 | v_commitment_actual + v_commitment_result（M7 多源对齐） |
| 19 | `docs/C-说明.md` | 14275 | 233 | C 的交付说明、种子设计、答辩三问、给 A/B/D 的对接说明 |

## 2. 修改文件（3 个，只动 C 自己的段落/索引行）

| # | 文件 | 字节 | 行数 | 改了什么 |
| --- | --- | ---: | ---: | --- |
| 1 | `sql/04_seed_business.sql` | 22541 | 359 | C 段由 TODO 占位替换为真实种子（字典 6 / 里程碑 160 / 承诺 136 / 成效 40）；**B 段未动** |
| 2 | `sql/README.md` | 5545 | 96 | 脚本清单：06 状态改为「已并入」、04 说明补 C 段、执行顺序与一键命令加 06 |
| 3 | `README.md` | 21295 | 370 | 交付清单加 C 行；目录结构补 06/modB/modC；新增 §4.7 C 段 DAO 速查与 M7 四难点表 |

## 3. 提交前必须确认的三件事

1. **不要提交包里的 `(2)` 副本**：`DbUtil (2).java`、`Page (2).java`、`Result (2).java`、`TextUtil (2).java`、
   `db (2).properties`、`package-info (2).java` 等 8 个文件是解压冲突产物，内容与正本相同，
   一并提交会导致「类重复」编译错误。本工程里已确认无此类文件。
2. **`src/main/resources/db.properties` 含本机口令**（`jdbc.password=123456`）。
   课程项目可提交，但换真实口令时请改用 `-Dbri.db.config=<外部文件>` 覆盖，不要把它推到公开仓库。
3. **执行顺序**：`01 → 02 → 03 → 04 → 05 → 06 → 99`。06 建视图只需表结构已存在（01），
   放在 04 之后是为了紧接着用 `CSelfTest` 就能验到真实数据的计算结果；单独先建 06 也不会报错。


## 4. 提交命令（PowerShell / CMD 通用）

```bash
cd /d D:\数据库三级项目\bri-pm
git status                                   # 先看清单是否符合第 1、2 节
git add src/main/java/com/bri/dao/MilestoneDao.java src/main/java/com/bri/dao/CommitmentDao.java src/main/java/com/bri/dao/OutcomeDao.java
git add src/main/java/com/bri/vo/            # 12 个新 VO
git add src/main/java/com/bri/modC/          # CSelfTest + package-info
git add sql/06_views_commitment.sql sql/04_seed_business.sql sql/README.md
git add docs/C-说明.md README.md
git commit -m "feat(C): 并入 M5 里程碑 / M7 履约比对 / M9 成果思政（3 DAO + 12 VO + 06 视图 + C 段种子）"
git push
```

> 若担心误提交，可先 `git add -A` 再 `git status` 逐条核对，确认没有 `(2)` 副本、没有 `target/`、
> 没有 `*.bak` 之后再 `git commit`。
