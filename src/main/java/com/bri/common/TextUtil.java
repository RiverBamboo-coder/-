package com.bri.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 文本与显示格式工具（A-2 公共类之一，全员共用）。
 *
 * <p>本类只做“字符串与显示格式”的事：不访问数据库、不做业务计算。分四组：</p>
 * <ul>
 *   <li><b>空值判断</b>：{@link #isBlank(CharSequence)}、{@link #isNotBlank(CharSequence)}、
 *       {@link #trimToNull(String)}、{@link #nullToEmpty(String)}、{@link #defaultIfBlank(String, String)}、
 *       {@link #safe(Object)}；</li>
 *   <li><b>模糊查询参数</b>：{@link #escapeLike(String)}、{@link #containsLike(String)} 与
 *       {@link #LIKE_ESCAPE_SQL}，防止用户输入的 % 和 _ 变成通配符；</li>
 *   <li><b>界面显示</b>：{@link #toDisplay(Object)}、{@link #formatAmount(BigDecimal)}、
 *       {@link #formatPercent(BigDecimal, int)}、{@link #formatPercent(double, int)}；</li>
 *   <li><b>长文本</b>：{@link #truncate(String, int)}。</li>
 * </ul>
 *
 * <p>约定：所有方法对 {@code null} 安全，不抛 {@link NullPointerException}；
 * 时间格式器是线程安全的（{@link DateTimeFormatter} 不可变）。</p>
 *
 * <p>模糊查询正确用法：</p>
 * <pre>
 * String sql = "SELECT project_id, project_name FROM t_project WHERE project_name LIKE ?"
 *         + TextUtil.LIKE_ESCAPE_SQL;
 * ps.setString(1, TextUtil.containsLike(keyword));   // keyword 为 null/空白时 containsLike 返回 null
 * </pre>
 *
 * @author A
 */
public final class TextUtil {

    /** 日期显示格式：yyyy-MM-dd */
    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA);

    /** 日期时间显示格式：yyyy-MM-dd HH:mm:ss */
    public static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    /** 时间显示格式：HH:mm:ss */
    public static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.CHINA);

    /**
     * 与 {@link #containsLike(String)} 配套的 SQL 片段，写在 {@code LIKE ?} 之后：
     * {@code ESCAPE '\\'}（SQL 里表示反斜杠是转义符）。
     */
    public static final String LIKE_ESCAPE_SQL = " ESCAPE '\\\\'";

    private static final char LIKE_ESCAPE_CHAR = '\\';

    private static final String ELLIPSIS = "...";

    private TextUtil() {
        throw new AssertionError("工具类不允许实例化");
    }

    /**
     * 是否为空白（null、空串、只含空格/制表符/全角空格）。
     *
     * @param text 待判断文本
     * @return true=空白
     */
    public static boolean isBlank(CharSequence text) {
        if (text == null) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否非空白。
     *
     * @param text 待判断文本
     * @return true=有实际内容
     */
    public static boolean isNotBlank(CharSequence text) {
        return !isBlank(text);
    }

    /**
     * null 转空串（拼接、显示时用）。
     *
     * @param text 原文本
     * @return 原文本或空串
     */
    public static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    /**
     * 去首尾空格，空白转 null（表单输入入库前用，保证数据库里不出现空串）。
     *
     * @param text 原文本
     * @return 去空格后的文本；空白返回 null
     */
    public static String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 空白时返回默认值（界面显示“未填写”等）。
     *
     * @param text         原文本
     * @param defaultValue 默认值
     * @return 原文本或默认值
     */
    public static String defaultIfBlank(String text, String defaultValue) {
        return isBlank(text) ? defaultValue : text;
    }

    /**
     * 任意对象转字符串，null 转空串（表格显示用）。
     *
     * @param value 任意值
     * @return 字符串，null 返回空串
     */
    public static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 转义 LIKE 的通配符：{@code \ % _} 前加反斜杠。
     *
     * @param text 原始文本
     * @return 转义后的文本，null 返回 null
     */
    public static String escapeLike(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder escaped = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == LIKE_ESCAPE_CHAR || c == '%' || c == '_') {
                escaped.append(LIKE_ESCAPE_CHAR);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    /**
     * 生成“包含”模糊查询参数：{@code %关键字%}，关键字里的通配符已转义。
     *
     * @param keyword 关键字
     * @return LIKE 参数；关键字为空白时返回 null（表示不加该条件）
     */
    public static String containsLike(String keyword) {
        if (isBlank(keyword)) {
            return null;
        }
        return "%" + escapeLike(keyword.trim()) + "%";
    }

    /**
     * 截断过长文本并加省略号。
     *
     * @param text      原文本
     * @param maxLength 最大长度（不含省略号）
     * @return 截断后的文本；null 返回空串
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (maxLength <= 0) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + ELLIPSIS;
    }

    /**
     * 金额显示：千分位 + 两位小数，例如 {@code 1,234,567.89}。
     *
     * @param amount 金额（数据库 DECIMAL(18,2) 读到的是 BigDecimal）
     * @return 显示文本；null 返回空串
     */
    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(amount);
    }

    /**
     * 百分比显示：例如 {@code 98.5%}。
     *
     * @param percent 已经是百分比数值（例如 98.456 表示 98.456%）
     * @param scale   保留小数位
     * @return 显示文本；null 返回空串
     */
    public static String formatPercent(BigDecimal percent, int scale) {
        if (percent == null) {
            return "";
        }
        int digits = scale < 0 ? 0 : scale;
        return percent.setScale(digits, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    /**
     * 百分比显示（double 重载，方便直接显示 SQL 算好的比率）。
     *
     * @param percent 百分比数值
     * @param scale   保留小数位
     * @return 显示文本；NaN/无穷返回空串
     */
    public static String formatPercent(double percent, int scale) {
        if (Double.isNaN(percent) || Double.isInfinite(percent)) {
            return "";
        }
        return formatPercent(BigDecimal.valueOf(percent), scale);
    }

    /**
     * 把任意数据库值转成表格单元格的显示文本。
     *
     * <p>处理了最常见的几类 JDBC 返回值：{@code java.sql.Date/Timestamp/Time}、
     * {@code LocalDate/LocalDateTime/LocalTime}、{@code BigDecimal}（避免科学计数法），其余走
     * {@link String#valueOf(Object)}。</p>
     *
     * @param value 单元格值
     * @return 显示文本；null 返回空串
     */
    public static String toDisplay(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate().format(DATE_FORMAT);
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().format(DATE_TIME_FORMAT);
        }
        if (value instanceof Time) {
            return ((Time) value).toLocalTime().format(TIME_FORMAT);
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).format(DATE_FORMAT);
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DATE_TIME_FORMAT);
        }
        if (value instanceof LocalTime) {
            return ((LocalTime) value).format(TIME_FORMAT);
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        return String.valueOf(value);
    }
}
