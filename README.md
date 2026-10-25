# bri-pm · “一带一路”沿线国家基础设施合作项目管理信息系统

> 《数据库原理》三级项目 · 四人共用的单个 Maven 工程
> 当前进度：**A 的 9 个交付物全部完成**（A-1～A-9），B/C/D 按各自章节推进。

| 编号 | 交付物 | 负责人 | 状态 |
| --- | --- | --- | --- |
| A-1 | Maven 工程 + pom.xml + 目录结构 + db.properties | A | ✅ 已完成 |
| A-2 | 公共类 DbUtil / Result / Page / TextUtil | A | ✅ 已完成 |
| A-3 | `sql/01_schema.sql`（24 张表 + 约束 + 索引，含 M5 生成列） | A | ✅ 已完成 |
| A-4 / A-5 | ER 图（Mermaid，可导出 PNG）+ 关系模式 + 范式论证表 | A | ✅ 已完成（`docs/`） |
| A-6 / A-7 | `02_dict_and_user.sql`（10 类 46 条字典 + 5 用户）、`03_seed_basic.sql`（61 国 / 35 企业 / 40 项目 / 80 合同 / 100 参与记录） | A | ✅ 已完成 |
| A-8 | M1–M3 的 DAO：CountryDao / ProjectDao / EnterpriseDao / ContractDao / DictDao + 14 个 VO | A | ✅ 已完成 |
| A-9 | X4 数据字典：MetaDao（表/字段/约束元数据） | A | ✅ 已完成 |
| B-1 ~ B-7 | 资金线：7 张表并入 01、04 种子、05 视图、7 个 DAO + 15 个 VO + 自检 | B | ✅ 已合并（A 并入本仓库） |
| B / C / D | 资金线、进度与承诺比对、界面 | B / C / D | ⏳ 各自章节 |

技术栈：**MySQL 8.0 + Java（JDK 17）+ Swing，JDBC 直连，不用 Spring**。

---

## 1. 环境要求

| 组件 | 版本 | 检查命令 | 说明 |
| --- | --- | --- | --- |
| JDK | 17 | `java -version` | `mvn` 编译用；`JAVA_HOME` 要指向 JDK 17 |
| Maven | 3.8+ | `mvn -v` | 首次构建会联网下载依赖 |
| MySQL | 8.0 | `mysql --version` | 库名统一 `bri_pm`，字符集 `utf8mb4` |

## 2. 目录结构

```text
bri-pm/
├── pom.xml                          # A：统一依赖（mysql-connector-java 8.0.33 + junit 4.13.2）
├── README.md                        # 本文件
├── .gitignore
├── docs/                            # A-4 / A-5 文档（Mermaid 图 + 范式论证表）+ B 的 B-1 / B-7
├── sql/
│   ├── 01_schema.sql                # 24 张表（含 B 的 M4/M6/M8 七张，按 B 的字段定义并入）
│   ├── 02_dict_and_user.sql         # 字典 + 演示用户
│   ├── 03_seed_basic.sql            # A 的基础数据
│   ├── 04_seed_business.sql         # B 的资金/报告/预警种子（C 段追加在文件末尾）
│   ├── 05_views_fund.sql            # B：v_fund_rate / v_fund_progress
│   ├── 99_verify_b.sql              # B：验收自检 SQL
│   └── README.md                    # 脚本清单、执行顺序、写脚本的硬性要求
└── src/
    ├── main/
    │   ├── java/com/bri/
    │   │   ├── common/              # A：DbUtil、Result、Page、TextUtil、DataAccessException
    │   │   ├── dao/                 # A：CountryDao / ProjectDao / EnterpriseDao / ContractDao / DictDao / MetaDao
    │   │   ├── vo/                  # A：14 个返回对象（CountryVO、ProjectVO、StatRow…）
    │   │   ├── service/             # 可选：跨 DAO 的业务计算
    │   │   ├── modA/                # A：自测面板（保留位，可空）
    │   │   └── ui/                  # D：MainFrame、9 个业务面板、看板、通用组件
    │   └── resources/
    │       └── db.properties        # A：数据库连接配置（唯一入口，改这里不改代码）
    └── test/
        └── java/com/bri/common/     # A：公共类单元测试（不连库）
```

`App.java`（程序入口）在 `com/bri/App.java`，由 **D** 负责编写；在 D 提交之前工程可以正常编译、跑测试，只是还没有可运行的 main 方法。

## 3. 三步跑起来

### 3.1 建库（A-3 脚本提交前先手工建一次）

```sql
DROP DATABASE IF EXISTS bri_pm;
CREATE DATABASE bri_pm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

执行方式与脚本顺序见 [`sql/README.md`](sql/README.md)。

### 3.2 改连接配置

编辑 `src/main/resources/db.properties` 里的 `jdbc.username` / `jdbc.password`（必要时改 `127.0.0.1:3306`）。
**不要**把账号密码写进任何 Java 文件。

### 3.3 构建与验证

```bash
mvn clean test        # 第 1 周：验证工程骨架 + 公共类（不需要数据库）
mvn clean package     # 生成 target/bri-pm.jar（已把 MySQL 驱动打进去，入口类由 D 提供）
java -jar target/bri-pm.jar
```

> 第 1 周的判定标准就是 `mvn clean test` 出现 **BUILD SUCCESS**。
> `target/bri-pm.jar` 目前还没有 `main` 方法（D 第 3 周补上 `com.bri.App`），此时 `java -jar` 会提示找不到主类，属正常现象。

## 4. 公共类速查（A-2）

### 4.1 DbUtil —— 全组唯一的取连接入口

```java
// ① D 的展示型查询：直接拿 List<Map> 绑 JTable（列名 = SELECT 别名）
List<Map<String, Object>> rows = DbUtil.query(
        "SELECT project_code, project_name, status FROM t_project WHERE status = ? ORDER BY project_id",
        "在建");

// ② 增删改
int updated = DbUtil.update(
        "UPDATE t_country SET is_belt_road = ? WHERE country_code = ?", 1, "KEN");

// ③ A/B/C 的业务 DAO：自己开连接、自己写 PreparedStatement、映射成 VO
try (Connection conn = DbUtil.getConnection();
     PreparedStatement ps = conn.prepareStatement(
             "SELECT project_id, project_code, project_name FROM t_project WHERE country_code = ?")) {
    DbUtil.setParameters(ps, "KEN");            // 统一参数绑定：null / LocalDate / BigDecimal / 枚举
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            // 映射成 ProjectVO
        }
    }
}
```

约定：

- `query / update` 内部只用 `PreparedStatement`，**禁止**把用户输入拼进 SQL；
- 返回的 Map 按 SELECT 列顺序排列（`LinkedHashMap`），重名列请用别名区分；
- 返回类型：MySQL `DATE` → `java.sql.Date`，`DATETIME` → `java.sql.Timestamp`，`DECIMAL` → `BigDecimal`；
  界面显示统一用 `TextUtil.toDisplay(value)`（否则 `Timestamp` 会带 `.0`）；
- 失败时抛 `DataAccessException`（`message` 已是中文提示）；`getConnection()` 失败抛 `SQLException`，`message` 同样可读；
- 启动自检写法：

```java
try (Connection ignored = DbUtil.getConnection()) {
    // 连上了，继续启动
} catch (SQLException e) {
    JOptionPane.showMessageDialog(null, e.getMessage(), "数据库连接失败", JOptionPane.ERROR_MESSAGE);
    System.exit(1);
}
```

### 4.2 Result —— 统一返回值

```java
public Result<Long> insert(ProjectVO vo) {
    try {
        // ... 执行插入，拿到自增主键
        return Result.ok(projectId, "新增成功");
    } catch (DataAccessException e) {
        return Result.fail("新增项目失败：" + e.getMessage());
    }
}

// 界面侧
Result<Long> r = projectDao.insert(vo);
if (r.isSuccess()) { MessageUtil.success(this, r.getMessage()); }
else               { MessageUtil.error(this, r.getMessage()); }
```

### 4.3 Page —— 分页 + 排序白名单

```java
Page page = Page.of(2, 20, "sign_date DESC");     // 默认 pageNo=1、pageSize=20；pageSize 上限 500
String order = page.resolveOrderBy("project_code", "project_name", "sign_date");  // 白名单校验
String sql = "SELECT project_id, project_code FROM t_project WHERE status = ?"
        + (order == null ? " ORDER BY project_id" : " " + order)
        + " LIMIT ? OFFSET ?";
ps.setString(1, status);
ps.setInt(2, page.getPageSize());
ps.setInt(3, page.getOffset());
```

`resolveOrderBy` 的规则：支持多列（`"sign_date DESC, project_name"`）、方向只允许 `ASC/DESC`、
最终拼进 SQL 的是**白名单里的列名**而不是用户输入；列名不在白名单、方向非法、写法非法都抛
`IllegalArgumentException`，DAO 转成 `Result.fail("排序字段不合法：...")`。

### 4.4 TextUtil —— 文本与显示格式

| 方法 | 用途 |
| --- | --- |
| `isBlank / isNotBlank` | null、空串、全空格（含全角空格）都算空白 |
| `trimToNull` | 表单输入去空格，空白转 `null` 再入库 |
| `defaultIfBlank` / `nullToEmpty` / `safe` | 界面显示“未填写”、避免 `null` 字样 |
| `containsLike` + `LIKE_ESCAPE_SQL` | 模糊查询参数 `%关键字%`，自动转义 `% _ \` |
| `escapeLike` | 只转义不包装 |
| `toDisplay` | 表格单元格统一格式化（日期、时间戳、BigDecimal） |
| `formatAmount` | `1,234,567.89` |
| `formatPercent` | `98.5%`（自动 HALF_UP 四舍五入） |
| `truncate` | 长文本截断加 `...` |

模糊查询标准写法：

```java
String sql = "SELECT project_id, project_name FROM t_project WHERE project_name LIKE ?"
        + TextUtil.LIKE_ESCAPE_SQL;                 // " ESCAPE '\\'"
ps.setString(1, TextUtil.containsLike(keyword));    // keyword 为空时返回 null
```

### 4.5 DAO 速查（A-8 / A-9，D 直接调用）

```java
// M1 国家
CountryDao countryDao = new CountryDao();
List<CountryVO> list = countryDao.findAll(Page.of(1, 20));   // 分页 + 白名单排序
List<CountryVO> asia = countryDao.findByRegion(1);            // 传大洲或区域 ID 都能查
List<StatRow> byContinent = countryDao.statProjectsByRegion();   // Q1-1
List<CountryVO> noProject = countryDao.findCountriesWithoutProject(); // Q1-2
List<StatRow> coverage = countryDao.statRegionCoverage();        // Q1-3（cnt/covered/pct）

// M2 项目
ProjectDao projectDao = new ProjectDao();
ProjectQuery q = new ProjectQuery();
q.setCountryCode("IDN"); q.setFieldCode("TRANS"); q.setStatus("在建");
List<ProjectVO> page1 = projectDao.findByCondition(q, Page.of(1, 20, "invest_amount DESC"));
long total = projectDao.count(q);
ProjectVO detail = projectDao.findById(1L);
Result<Long> r = projectDao.insert(vo);       // 失败时 r.getMessage() 就是中文提示
List<StatRow> cross = projectDao.statByFieldAndStatus();   // Q2-1（code="TRANS|在建"）
List<GapRow> gaps = projectDao.findContractGapProjects();  // Q2-2（示例数据里有 5 个不一致）
List<ContractVO> contracts = new ContractDao().findByProject(1L);

// M3 企业
EnterpriseDao enterpriseDao = new EnterpriseDao();
List<EnterpriseVO> ents = enterpriseDao.findAll(Page.of(1, 50, "project_count DESC"));
List<ProjectRoleVO> roles = enterpriseDao.findProjectsByEnterprise(1L);
List<RankRow> top = enterpriseDao.statEnterpriseRanking(10);   // Q3-1
List<ShareRow> shares = enterpriseDao.statConsortiumShare();    // Q3-4

// M0 字典（下拉框）
List<DictItem> statuses = new DictDao().listByType("PROJECT_STATUS");

// X4 数据字典
MetaDao metaDao = new MetaDao();
List<MetaTableVO> tables = metaDao.listTables();                  // 24 张
List<MetaColumnVO> cols = metaDao.listColumns("t_project");
List<MetaConstraintVO> cons = metaDao.listConstraints("t_project_participant");
```

**异常约定**：查询方法抛 `DataAccessException`（中文 message），写入方法返回 `Result`；界面统一 `catch (DataAccessException e) { MessageUtil.error(this, e.getMessage()); }` 即可，不需要自己翻译错误码。

### 4.6 B 段 DAO 速查（资金线 M4 / M6 / M8，已并入）

```java
FundDao fundDao = new FundDao();
List<FundPlanVO> plans = fundDao.findPlansByProject(1L);          // 年度计划
List<DisbVO> disb = fundDao.findDisbByProject(1L, Page.of(1, 20)); // 拨付流水（分页）
fundDao.insertDisbursement(vo);                                    // Result<Long>
List<RateRow> rates = fundDao.statFundRateByProject();             // Q4-1 到位率
List<RateRow> low = fundDao.findLowRateProjects(60);               // Q4-2
List<SourceRow> bySource = fundDao.statBySource();                 // Q4-3 来源占比
List<YearRow> byYear = fundDao.statYearPlanVsActual();             // Q4-4 年度对比
List<OverrunRow> over = fundDao.findOverrunProjects(0);            // Q4-5 超计划

ReportDao reportDao = new ReportDao();
reportDao.insertReport(vo);                                        // 同项目同月会返回中文“已报过”
List<MoMRow> mom = reportDao.statMonthlyMoM(1L);                   // Q6-1 环比（LAG）
List<OutRow> out = reportDao.statOutputByCountry();                // Q6-2
List<StagnantRow> st = reportDao.findStagnantProjects(3);          // Q6-3
List<HireRow> hire = reportDao.findLowLocalHireRate(50);           // Q6-4

WarningDao warningDao = new WarningDao();
int n = warningDao.refreshWarnings();                              // 幂等刷新，返回命中条数
List<WarningVO> red = warningDao.findByLevel("R", Page.of(1, 50));  // 红/黄/蓝
warningDao.ackWarning(id, "admin");                                // 标记处理
```

**控制台自检**：`com.bri.dao.DaoSmokeTest` 的 `main` 会把每个方法跑一遍并打印行数与首行样例，跑通即证明 A-8/A-9 可用：

```text
在 IDEA 里右键 DaoSmokeTest → Run 'DaoSmokeTest.main()'（classpath 已含 MySQL 驱动）
或先 mvn -q clean compile 确认能编译，再在 IDE 里运行
```

## 5. 全组约定（合并前必须遵守）

1. 包名统一 `com.bri`，不新建 `com.aaa` / `test` / `myproj`；
2. 一人一组 DAO，只改自己的类：A→CountryDao/ProjectDao/EnterpriseDao/ConsortiumDao/ContractDao/DictDao/MetaDao，
   B→FundDao/ReportDao/WarningDao/WarningRuleDao，C→MilestoneDao/CommitmentDao/OutcomeDao；
3. SQL 一律 `PreparedStatement` 参数化，字符串拼接 SQL 视为不合格；
4. DAO 捕获异常后返回 `Result.fail(中文提示)`，不把堆栈抛给界面；
5. 所有列表查询带 `Page`，排序必须走 `resolveOrderBy` 白名单；
6. 界面类只在 `com.bri.ui`（D 维护），A/B/C 不新建窗口类；
7. 金额用 `BigDecimal`、日期用 `LocalDate`、主键用 `long/Long`，禁止用 `double` 存金额、`int` 存主键；
8. 公共类（`common`）由 A 维护，需要新方法先在群里说一声，避免四人各改一份。

## 6. 验收自检（A-1 / A-2 的 Definition of Done）

- [x] 目录结构与包结构完整，四人拉下来能编译；
- [x] `pom.xml` 只有 `mysql-connector-java 8.0.33` 与 `junit 4.13.2`，无 Spring、无连接池；
- [x] `src/main/resources/db.properties` 存在，Java 代码里没有任何连接信息；
- [x] `DbUtil` 提供 `getConnection()`、`close(ResultSet, Statement, Connection)`、`query`、`update`，全部参数化；
- [x] `Result` / `Page` / `TextUtil` 全部 null 安全；`Page` 有排序白名单；
- [x] 4 个单元测试类覆盖上述公共类（`mvn clean test`）；连库验证放在第 3 章手测。

## 7. 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | 依赖没下全 / 没打包 | `mvn clean package -U`，确认本地仓库有 `mysql-connector-java-8.0.33.jar` |
| `The server time zone value ... is unrecognized` | 连接串没有 `serverTimezone` | 已在 `db.properties` 里带上 `serverTimezone=Asia/Shanghai` |
| `Public Key Retrieval is not allowed` | MySQL 8 默认 `caching_sha2_password` | 已在连接串里带 `allowPublicKeyRetrieval=true` |
| `Access denied for user 'root'@'localhost'` | 账号密码不对 | 改 `db.properties` 的 `jdbc.username / jdbc.password` |
| 中文乱码 | 源文件或库表字符集不对 | 源码 UTF-8（pom 已设）；库/表 `utf8mb4`；连接串 `characterEncoding=utf8` |
| 启动卡 5 秒后提示连接失败 | MySQL 服务没启动 | 启动 MySQL 服务；登录超时固定 5 秒，不会卡死界面 |
| `数据库配置不可用：classpath 下没有找到 db.properties` | 配置文件没在 `src/main/resources` 下 | 移到该目录后 `mvn clean` 重新构建 |
| 命令行执行 SQL 报 `source` 找不到文件 | Windows 路径 | 用正斜杠，例如 `source D:/数据库三级项目/bri-pm/sql/01_schema.sql` |

## 8. A 的交付清单与验证方式

A 的 9 个交付物已全部完成，逐项对应：

| 编号 | 交付物 | 验证方式 | 预期结果 |
| --- | --- | --- | --- |
| A-1 | `pom.xml` + 目录 + `db.properties` | `mvn clean test` | `BUILD SUCCESS` |
| A-2 | `common/`（DbUtil/Result/Page/TextUtil/DataAccessException） | `mvn clean test`（4 个测试类） | 全部用例通过 |
| A-3 | `sql/01_schema.sql` | 执行脚本后 `SHOW TABLES` | **24 张表**，`SHOW CREATE TABLE` 约束齐全 |
| A-4 | `docs/A4-ER图与关系模式.md` | GitHub 预览 / mermaid.live 导出 PNG | ER 图 + 24 行关系模式 |
| A-5 | `docs/A5-范式论证表.md` | 阅读并对照 `SHOW CREATE TABLE` | 24 表逐表范式结论 |
| A-6 | `sql/02_dict_and_user.sql` | `SELECT COUNT(*) FROM t_dict_type / t_dict_item / t_user` | **10 / 46 / 5** |
| A-7 | `sql/03_seed_basic.sql` | `SELECT COUNT(*) FROM t_country / t_enterprise / t_project / t_contract / t_project_participant` | **61 / 35 / 40 / 80 / 100** |
| A-8 | `dao/` 5 个 DAO + `vo/` 14 个 VO | 运行 `DaoSmokeTest.main` | 每个方法打印行数，与 §4.5 注释一致 |
| A-9 | `dao/MetaDao` | `DaoSmokeTest` 的 X4 段 | 表 24 张、`t_project_participant` 6 个字段、6 个约束（1 主键 + 3 外键 + 2 检查） |

**B / C 要补的**：`04_seed_business.sql`（各自段落）、`05_views_fund.sql`、`06_views_commitment.sql`。

> 全组底座已就位：D 可以把假数据换成真数据（§4.5 的 DAO），B/C 可以对着 `01_schema.sql` + `03_seed_basic.sql` 写 DAO 与视图。
