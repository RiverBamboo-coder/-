# “一带一路”沿线国家基础设施合作项目管理信息系统

《数据库原理》三级项目 · 2025 级软件工程 5 班 第 5 小组（丝绸之路）

面向“一带一路”基础设施合作业务，管理**国家—项目—参与企业—资金—进度**全要素数据，
支撑合作项目从立项到竣工移交的全生命周期跟踪。

| 项 | 内容 |
| --- | --- |
| 技术栈 | MySQL 8.0 + Java（JDK 17）+ JDBC，界面用 Swing（D 负责） |
| 难度 | ★★★★（难度系数 1.05） |
| 数据规模 | 24 张表 / 61 国 / 40 项目 / 35 企业 / 240 笔拨付 / 160 里程碑 / 136 承诺 |

---

## 1. 目录结构

```text
数据库三级项目/
├── bri-pm/                                # 主体工程（Maven）
│   ├── pom.xml
│   ├── README.md                          # ★ 工程详细说明（公共类、DAO 速查、约定）
│   ├── docs/                              # 设计文档
│   │   ├── A4-ER图与关系模式.md
│   │   ├── A5-范式论证表.md
│   │   ├── B-1-字段清单与A侧依赖.md
│   │   ├── B-7-验证与演示脚本.md
│   │   ├── B-7-自检输出原文.txt
│   │   ├── B-合并说明.md
│   │   ├── C-说明.md                      # C 的交付说明 + 答辩三问
│   │   └── C-合并清单.md                  # C 并入工程的文件清单
│   ├── sql/                               # 建库与种子（按序执行）
│   │   ├── 01_schema.sql                  # 24 张表 + 约束 + 索引
│   │   ├── 02_dict_and_user.sql           # 字典 + 演示用户
│   │   ├── 03_seed_basic.sql              # 区域/国家/企业/项目/合同
│   │   ├── 04_seed_business.sql           # B 段资金进度 + C 段里程碑承诺成效
│   │   ├── 05_views_fund.sql              # B：资金到位率视图
│   │   ├── 06_views_commitment.sql        # C：履约承诺比对视图（M7 核心）
│   │   ├── 99_verify_b.sql                # B 段验收自检
│   │   └── README.md                      # ★ 脚本清单与执行顺序
│   └── src/main/java/com/bri/
│       ├── common/                        # DbUtil / Result / Page / TextUtil
│       ├── dao/                           # 18 个 DAO（A/B/C 各自动的部分）
│       ├── vo/                            # 42 个返回对象
│       ├── modA|modB|modC/                # 各人的控制台自检
│       └── ui/                            # D：界面
├── dev-env/                               # JDK / Maven 环境安装脚本
├── 一带一路…需求分析与模块分块.docx        # 需求分析与模块拆分
├── 一带一路…四人分工详细任务书.docx        # 分工任务书
└── .gitignore
```

> `_src/`（生成文档的脚本与备份）不进仓库，已在 `.gitignore` 中排除。

---

## 2. 跑起来（三步）

### 2.1 建库并导入脚本

顺序不能乱（有外键依赖）：

```bat
for %f in (01_schema 02_dict_and_user 03_seed_basic 04_seed_business 05_views_fund 06_views_commitment 99_verify_b) do (
    mysql -uroot -p --default-character-set=utf8mb4 < bri-pm\sql\%f.sql
)
```

`01_schema.sql` 内含 `CREATE DATABASE IF NOT EXISTS bri_pm`，一般无需手工建库。

### 2.2 改连接配置

编辑 `bri-pm/src/main/resources/db.properties`，把 `jdbc.username` / `jdbc.password` 改成自己的。
换机器不想改文件时，可用外部配置覆盖：

```bash
-Dbri.db.config=D:\path\to\db.properties
```

### 2.3 编译并自检

```bash
cd bri-pm
mvn -q clean compile
java -cp "target/classes;target/dependency/*" com.bri.dao.DaoSmokeTest   # A 段 DAO
java -cp "target/classes;target/dependency/*" com.bri.modB.BSelfTest     # B 段资金线
java -cp "target/classes;target/dependency/*" com.bri.modC.CSelfTest     # C 段 M5/M7/M9
```

> `CSelfTest` 第 3 段会真实写入（给 1 号项目补排“竣工移交”、补录“设备安装”实际日期），
> 用来演示生成列 `is_delayed` 自动变 true。要复原数据，重跑 `04_seed_business.sql` 即可（可重复执行）。

---

## 3. 模块与分工

| 成员 | 模块 | 主要内容 |
| --- | --- | --- |
| 崔奥天 | M5 / M7 / M9 | 里程碑与超期判定、履约承诺四类比对（复杂关联核心）、成果与三条思政查询 |
| 洪博 | M4 / M6 / M8 | 资金计划与拨付、进度报告、资金-进度协同预警 |
| 曹文聪 | UI | 主界面、九个业务面板、成果看板 |
| 赵安 | M0–M3 | 建表与约束、ER 图与范式、国别/项目/企业的 DAO |

**M7（履约承诺比对）是本项目的高难度落点**——评分标准要求“高难度需实现复杂关联”，
`06_views_commitment.sql` 用 CASE 把工期/金额/属地用工/培训四类实际值从 3 张不同粒度的表
统一到“承诺”粒度，并用 NULL 首判实现三值逻辑（未拨付 ≠ 0，而是“待评估”）。

---

## 4. 文档索引

| 文档 | 内容 |
| --- | --- |
| `bri-pm/README.md` | 工程说明：公共类 API、A/B/C 段 DAO 速查、编码约定 |
| `bri-pm/sql/README.md` | 脚本清单、执行顺序、写 SQL 的硬性要求 |
| `docs/A4-ER图与关系模式.md` | ER 图（Mermaid）与关系模式 |
| `docs/A5-范式论证表.md` | 逐表函数依赖与范式判断 |
| `docs/C-说明.md` | C 的交付说明、种子设计、M7 四个难点、答辩三问 |
| `docs/C-合并清单.md` | C 并入工程的文件清单（19 新增 + 3 修改） |

---

## 5. 提交约定

- **不要**提交 `target/`、`.idea/`、`*.class`、`_src/`、`*.bak`（`.gitignore` 已排除）；
- 解压他人压缩包时注意剔除带 `(2)` 的重复副本，否则 Maven 会报“类重复”；
- `db.properties` 含本机演示口令，课程项目可提交；换真实口令时改用外部配置覆盖；
- 提交信息建议写明模块，例如 `feat(C): 并入 M5/M7/M9 与 06 视图`。
