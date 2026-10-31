package com.bri.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 数据库工具类（A-2 公共类之一）：全组唯一的取连接入口。
 *
 * <p>设计要点（答辩可直接讲）：</p>
 * <ul>
 *   <li>连接信息集中在 {@code src/main/resources/db.properties}，代码里没有任何地址、账号、密码；</li>
 *   <li>用 {@link DriverManager} 直连（不引连接池），但把“读配置、加载驱动、登录超时、错误翻译”
 *       全部封装在本类里，别人不用关心；</li>
 *   <li>{@link #query(String, Object...)} / {@link #update(String, Object...)} 的入参只有 SQL 和参数，
 *       内部一律 {@link PreparedStatement}，从根源上避免字符串拼接 SQL；</li>
 *   <li>所有资源在 finally 中释放，不泄漏连接；</li>
 *   <li>连不上时抛出的 {@link SQLException} 带中文可读提示，界面直接弹窗即可。</li>
 * </ul>
 *
 * <p>两种用法：</p>
 * <pre>
 * // 1) D 的展示型查询：直接拿 List&lt;Map&gt; 绑 JTable（列名 = SELECT 的别名）
 * List&lt;Map&lt;String, Object&gt;&gt; rows = DbUtil.query(
 *         "SELECT project_code, project_name, status FROM t_project WHERE status = ? ORDER BY project_id",
 *         "在建");
 *
 * // 2) A/B/C 的业务 DAO：自己开连接、自己写 PreparedStatement、映射成 VO（答辩讲“类型安全的对象映射”）
 * try (Connection conn = DbUtil.getConnection();
 *      PreparedStatement ps = conn.prepareStatement("SELECT ... WHERE project_id = ?")) {
 *     DbUtil.setParameters(ps, projectId);
 *     try (ResultSet rs = ps.executeQuery()) {
 *         ...
 *     }
 * }
 * </pre>
 *
 * <p>返回值类型提醒：MySQL 的 {@code DATE} 读出来是 {@link java.sql.Date}，{@code DATETIME} 是
 * {@link Timestamp}，{@code DECIMAL} 是 {@link BigDecimal}。界面显示请统一走
 * {@link TextUtil#toDisplay(Object)}，不要自己 toString()（{@code Timestamp} 会多出 ".0"）。</p>
 *
 * @author A
 */
public final class DbUtil {

    /** 配置文件在 classpath 中的名字（对应 src/main/resources/db.properties） */
    public static final String CONFIG_NAME = "db.properties";

    /** 可用 {@code -Dbri.db.config=绝对路径} 指定外部配置文件，优先级高于 classpath（演示机、答辩机用） */
    public static final String CONFIG_PATH_PROPERTY = "bri.db.config";

    /** 配置项：JDBC 驱动类名 */
    public static final String KEY_DRIVER = "jdbc.driver";

    /** 配置项：连接串 */
    public static final String KEY_URL = "jdbc.url";

    /** 配置项：账号 */
    public static final String KEY_USERNAME = "jdbc.username";

    /** 配置项：密码 */
    public static final String KEY_PASSWORD = "jdbc.password";

    /** 登录超时（秒）：MySQL 没启动时不要卡住界面太久 */
    private static final int LOGIN_TIMEOUT_SECONDS = 5;

    /** 异常信息里 SQL 片段的最大长度 */
    private static final int MAX_SQL_IN_MESSAGE = 200;

    /** 驱动类默认值 */
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    /** 配置只加载一次 */
    private static final Config CONFIG = Config.load();

    /** 驱动只加载一次 */
    private static volatile boolean driverLoaded;

    private DbUtil() {
        throw new AssertionError("工具类不允许实例化");
    }

    /**
     * 取一个数据库连接。调用方负责关闭（推荐 try-with-resources）。
     *
     * @return 新连接
     * @throws SQLException 配置缺失、驱动缺失、MySQL 未启动、账号密码错误等，
     *                      message 为可直接展示的中文提示
     */
    public static Connection getConnection() throws SQLException {
        return CONFIG.open();
    }

    /**
     * 关掉查询用到的三个资源（顺序：ResultSet → Statement → Connection），全部 null 安全。
     *
     * @param rs   结果集，可为 null
     * @param st   语句，可为 null
     * @param conn 连接，可为 null
     */
    public static void close(ResultSet rs, Statement st, Connection conn) {
        closeQuietly(rs, st, conn);
    }

    /**
     * 静默关闭任意数量的资源，关闭过程中的异常被忽略（不影响业务结果）。
     *
     * @param resources 资源，可为 null 元素
     */
    public static void closeQuietly(AutoCloseable... resources) {
        if (resources == null) {
            return;
        }
        for (int i = 0; i < resources.length; i++) {
            AutoCloseable resource = resources[i];
            if (resource == null) {
                continue;
            }
            try {
                resource.close();
            } catch (Exception ignored) {
                // 关闭失败不改变业务结果，这里不记录也不抛出
            }
        }
    }

    /**
     * 给 PreparedStatement 按顺序绑定参数（统一处理 null、日期时间、BigDecimal、枚举）。
     *
     * @param ps     预编译语句
     * @param params 参数，第 i 个绑定到第 i+1 个 ?
     * @throws SQLException 绑定失败
     */
    public static void setParameters(PreparedStatement ps, Object... params) throws SQLException {
        if (ps == null || params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            int index = i + 1;
            Object value = params[i];
            if (value == null) {
                ps.setObject(index, null);
            } else if (value instanceof LocalDate) {
                ps.setDate(index, java.sql.Date.valueOf((LocalDate) value));
            } else if (value instanceof LocalDateTime) {
                ps.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
            } else if (value instanceof LocalTime) {
                ps.setTime(index, java.sql.Time.valueOf((LocalTime) value));
            } else if (value instanceof BigDecimal) {
                ps.setBigDecimal(index, (BigDecimal) value);
            } else if (value instanceof Enum) {
                ps.setString(index, ((Enum<?>) value).name());
            } else {
                ps.setObject(index, value);
            }
        }
    }

    /**
     * 通用查询：返回按 SELECT 列顺序排列的行集合，列名 = SELECT 的别名。
     *
     * <p>给界面（D）直接绑 JTable 用；A/B/C 的业务查询建议自己写 PreparedStatement 映射成 VO。</p>
     *
     * @param sql    参数化 SQL，例如 {@code SELECT * FROM t_project WHERE status = ?}
     * @param params 与 ? 一一对应的参数
     * @return 查询结果，永不为 null；没有数据时返回空集合
     * @throws DataAccessException 查询失败，message 含中文原因与 SQL 片段
     */
    public static List<Map<String, Object>> query(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = prepare(conn, sql, params);
            rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            String[] labels = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                labels[i] = meta.getColumnLabel(i + 1);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>(Math.max(16, columnCount * 2));
                for (int i = 0; i < columnCount; i++) {
                    row.put(labels[i], rs.getObject(i + 1));
                }
                rows.add(row);
            }
            return rows;
        } catch (SQLException e) {
            throw new DataAccessException("查询失败：" + e.getMessage() + describeSql(sql), e);
        } finally {
            close(rs, ps, conn);
        }
    }

    /**
     * 统计查询：执行 {@code SELECT COUNT(*) ...} 并取第一行第一列（分页总条数用）。
     *
     * @param sql    参数化 SQL
     * @param params 参数
     * @return 计数值，无结果或非数字时返回 0
     * @throws DataAccessException 查询失败
     */
    public static long queryCount(String sql, Object... params) {
        List<Map<String, Object>> rows = query(sql, params);
        if (rows.isEmpty()) {
            return 0L;
        }
        Object value = rows.get(0).values().iterator().next();
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 通用更新：INSERT / UPDATE / DELETE 共用，返回受影响行数。
     *
     * @param sql    参数化 SQL
     * @param params 与 ? 一一对应的参数
     * @return 受影响行数
     * @throws DataAccessException 更新失败（含外键冲突、唯一键冲突等），message 为中文提示
     */
    public static int update(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = prepare(conn, sql, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新失败：" + e.getMessage() + describeSql(sql), e);
        } finally {
            close(null, ps, conn);
        }
    }

    private static PreparedStatement prepare(Connection conn, String sql, Object[] params) throws SQLException {
        if (TextUtil.isBlank(sql)) {
            throw new SQLException("SQL 语句不能为空");
        }
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            setParameters(ps, params);
        } catch (SQLException e) {
            closeQuietly(ps);
            throw e;
        }
        return ps;
    }

    private static void loadDriver(String driverClassName) throws SQLException {
        if (driverLoaded) {
            return;
        }
        synchronized (DbUtil.class) {
            if (driverLoaded) {
                return;
            }
            try {
                Class.forName(driverClassName);
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                throw new SQLException("找不到 JDBC 驱动类 " + driverClassName
                        + "，请确认 pom.xml 已引入 mysql-connector-java 8.0.33，并已重新打包（mvn clean package）。", e);
            }
        }
    }

    private static String describeSql(String sql) {
        if (sql == null) {
            return "";
        }
        String compact = sql.replaceAll("\\s+", " ").trim();
        if (compact.length() > MAX_SQL_IN_MESSAGE) {
            compact = compact.substring(0, MAX_SQL_IN_MESSAGE) + "...";
        }
        return "（SQL：" + compact + "）";
    }

    /** 异常信息里隐藏密码等敏感参数 */
    private static String maskUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.replaceAll("(?i)(password|pwd)=[^&]*", "$1=***");
    }

    /**
     * 配置读取结果：加载失败也返回对象（不抛静态初始化异常），把原因留到第一次取连接时提示，
     * 这样界面启动时能弹出可读中文，而不是一屏 {@code ExceptionInInitializerError}。
     */
    private static final class Config {

        private final Properties properties;
        private final String source;
        private final String error;

        private Config(Properties properties, String source, String error) {
            this.properties = properties;
            this.source = source;
            this.error = error;
        }

        static Config load() {
            String external = TextUtil.trimToNull(System.getProperty(CONFIG_PATH_PROPERTY));
            if (external != null) {
                return loadFromFile(Paths.get(external), "系统属性 -D" + CONFIG_PATH_PROPERTY);
            }
            return loadFromClasspath();
        }

        private static Config loadFromClasspath() {
            ClassLoader loader = DbUtil.class.getClassLoader();
            InputStream in = loader == null
                    ? ClassLoader.getSystemResourceAsStream(CONFIG_NAME)
                    : loader.getResourceAsStream(CONFIG_NAME);
            if (in == null) {
                return new Config(new Properties(), null,
                        "classpath 下没有找到 " + CONFIG_NAME + "（应放在 src/main/resources/db.properties）");
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return new Config(read(reader), "classpath:" + CONFIG_NAME, null);
            } catch (IOException e) {
                return new Config(new Properties(), "classpath:" + CONFIG_NAME, "读取失败：" + e.getMessage());
            }
        }

        private static Config loadFromFile(Path path, String desc) {
            if (!Files.isReadable(path)) {
                return new Config(new Properties(), path.toString(),
                        desc + " 指向的文件不存在或不可读：" + path);
            }
            try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                return new Config(read(reader), path.toString(), null);
            } catch (IOException e) {
                return new Config(new Properties(), path.toString(), "读取失败：" + path + "（" + e.getMessage() + "）");
            }
        }

        private static Properties read(Reader reader) throws IOException {
            Properties props = new Properties();
            props.load(reader);
            return props;
        }

        private Connection open() throws SQLException {
            if (error != null) {
                throw new SQLException("数据库配置不可用：" + error + "。当前配置来源："
                        + (source == null ? "无" : source) + "。");
            }
            String driver = property(KEY_DRIVER, DEFAULT_DRIVER);
            String url = property(KEY_URL, null);
            String username = property(KEY_USERNAME, null);
            String password = property(KEY_PASSWORD, "");
            if (url == null) {
                throw new SQLException("db.properties 缺少 " + KEY_URL
                        + "（示例：jdbc:mysql://127.0.0.1:3306/bri_pm?useUnicode=true&characterEncoding=utf8"
                        + "&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true）");
            }
            if (username == null) {
                throw new SQLException("db.properties 缺少 " + KEY_USERNAME + "（MySQL 登录账号）");
            }
            loadDriver(driver);
            try {
                DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
                return DriverManager.getConnection(url, username, password);
            } catch (SQLException e) {
                throw new SQLException("连接数据库失败：" + e.getMessage()
                        + "。请依次检查：① MySQL 服务是否已启动；② 数据库 bri_pm 是否已创建；③ "
                        + CONFIG_NAME + " 里的地址/账号/密码（当前地址 " + maskUrl(url) + "）。",
                        e.getSQLState(), e.getErrorCode(), e);
            }
        }

        private String property(String key, String defaultValue) {
            String value = properties.getProperty(key);
            if (value == null) {
                return defaultValue;
            }
            value = value.trim();
            return value.isEmpty() ? defaultValue : value;
        }
    }
}
