# bri-pm · “一带一路”沿线国家基础设施合作项目管理信息系统

> 《数据库原理》三级项目 · 四人共用的单个 Maven 工程
> 当前进度：**A-1 工程骨架**、**A-2 公共类**、**A-3 建表脚本** 已完成，其余按各自章节推进。

| 编号 | 交付物 | 负责人 | 状态 |
| --- | --- | --- | --- |
| A-1 | Maven 工程 + pom.xml + 目录结构 + db.properties | A | ✅ 已完成 |
| A-2 | 公共类 DbUtil / Result / Page / TextUtil | A | ✅ 已完成 |
| A-3 | `sql/01_schema.sql`（24 张表 + 约束 + 索引，含 M5 生成列） | A | ✅ 已完成 |
| A-4 / A-5 | ER 图 / 范式论证表 | A | ⏳ 第 2 周 |
| A-6 / A-7 | 02 字典与用户、03 基础数据脚本 | A | ⏳ 第 2–3 周（下一步） |
| A-8 | M1–M3 的 DAO | A | ⏳ 第 4 周 |
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
├── sql/                             # 建库 / 种子 / 验收脚本（A-3 起逐步提交）
│   └── README.md                    # 脚本清单、执行顺序、写脚本的硬性要求
└── src/
    ├── main/
    │   ├── java/com/bri/
    │   │   ├── common/              # A：DbUtil、Result、Page、TextUtil、DataAccessException
    │   │   ├── dao/                 # A/B/C：一人一组 DAO，类名不重名
    │   │   ├── vo/                  # A/B/C：与 DAO 一一对应的返回行对象
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

## 8. A 的下一步（按第 1 章交付物清单）

1. **A-3 ✅ 已完成**：`sql/01_schema.sql` 建了 24 张表（A 的 12 张 + B 的 7 张 + C 的 5 张），
   含主键/外键/唯一/检查约束与索引，M5 的 `is_delayed` 为生成列；
2. **A-6**：`02_dict_and_user.sql`（字典 ≥40 条：领域 6 / 模式 5 / 币种 4 / 企业性质 5 / 项目状态 5 / 角色 5，另加用户）；
3. **A-7**：`03_seed_basic.sql`（60 国 / 35 企业 / 40 项目 / 80 合同 / 100 参与记录 / 5 联合体）；
4. **A-4 / A-5**：ER 图（含 M3 的 M:N 带属性与“企业—联合体”递归联系）与范式论证表；
5. **A-8**：M1–M3 的 DAO，每个方法控制台验证。
6. **B / C 要补的**：`04_seed_business.sql`（各自段落）、`05_views_fund.sql`、`06_views_commitment.sql`。

> 顺序建议：先 A-6（半天，能立刻让 D 的下拉框有数据）→ A-7（数据量是“数据 30 分”的底座）→ A-8（DAO）→ 最后 A-4/A-5 文档（表一冻结，图画起来才有意义）。
