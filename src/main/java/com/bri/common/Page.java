package com.bri.common;

import java.io.Serializable;
import java.util.Locale;

/**
 * 统一分页参数（A-2 公共类之一，所有列表查询都带）。
 *
 * <p>两个职责：</p>
 * <ol>
 *   <li>提供 {@code pageNo / pageSize / offset}，配合 {@code LIMIT ? OFFSET ?} 使用；</li>
 *   <li>提供 {@link #resolveOrderBy(String...)}，把界面传来的排序字段按<b>白名单</b>校验后再拼成
 *       {@code ORDER BY ...}，杜绝把用户输入直接拼进 SQL（SQL 注入的高发点）。</li>
 * </ol>
 *
 * <p>DAO 标准写法：</p>
 * <pre>
 * String order = page.resolveOrderBy("project_code", "project_name", "sign_date");
 * String sql = "SELECT project_id, project_code, project_name, status FROM t_project"
 *         + " WHERE status = ?"
 *         + (order == null ? " ORDER BY project_id" : " " + order)
 *         + " LIMIT ? OFFSET ?";
 * ps.setString(1, status);
 * ps.setInt(2, page.getPageSize());
 * ps.setInt(3, page.getOffset());
 * </pre>
 *
 * <p>说明：{@code pageNo / pageSize} 在 setter 里做了上下限保护（页码 ≥1、每页 1~500 条），
 * 防止界面传 0 或超大值导致 SQL 出错或一次拉全表。</p>
 *
 * @author A
 */
public class Page implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 默认页码 */
    public static final int DEFAULT_PAGE_NO = 1;

    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 每页最少条数 */
    public static final int MIN_PAGE_SIZE = 1;

    /** 每页最多条数（防止一次拉全表） */
    public static final int MAX_PAGE_SIZE = 500;

    /** 最大页码（防止 offset 溢出） */
    public static final int MAX_PAGE_NO = 1000000;

    /** 页码，从 1 开始 */
    private int pageNo = DEFAULT_PAGE_NO;

    /** 每页条数 */
    private int pageSize = DEFAULT_PAGE_SIZE;

    /** 排序字段（原始值，可能为空；只有经过白名单校验后才能进 SQL） */
    private String orderBy;

    public Page() {
        // 使用默认分页参数
    }

    public Page(int pageNo, int pageSize) {
        setPageNo(pageNo);
        setPageSize(pageSize);
    }

    public Page(int pageNo, int pageSize, String orderBy) {
        this(pageNo, pageSize);
        setOrderBy(orderBy);
    }

    /**
     * 静态工厂：{@code Page.of(2, 20)}。
     *
     * @param pageNo   页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页参数
     */
    public static Page of(int pageNo, int pageSize) {
        return new Page(pageNo, pageSize);
    }

    /**
     * 静态工厂：{@code Page.of(1, 20, "sign_date DESC")}。
     *
     * @param pageNo   页码，从 1 开始
     * @param pageSize 每页条数
     * @param orderBy  排序字段（还要经过 {@link #resolveOrderBy(String...)} 白名单校验）
     * @return 分页参数
     */
    public static Page of(int pageNo, int pageSize, String orderBy) {
        return new Page(pageNo, pageSize, orderBy);
    }

    public int getPageNo() {
        return pageNo;
    }

    /**
     * 设置页码，小于 1 按 1 处理，大于 {@link #MAX_PAGE_NO} 按上限处理。
     *
     * @param pageNo 页码
     */
    public void setPageNo(int pageNo) {
        this.pageNo = pageNo < DEFAULT_PAGE_NO ? DEFAULT_PAGE_NO : Math.min(pageNo, MAX_PAGE_NO);
    }

    public int getPageSize() {
        return pageSize;
    }

    /**
     * 设置每页条数，取值范围 {@link #MIN_PAGE_SIZE} ~ {@link #MAX_PAGE_SIZE}。
     *
     * @param pageSize 每页条数
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize < MIN_PAGE_SIZE ? MIN_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public String getOrderBy() {
        return orderBy;
    }

    /**
     * 设置排序字段（空白字符串按“不排序”处理）。
     *
     * @param orderBy 形如 {@code sign_date DESC}、{@code project_code}、{@code sign_date DESC, project_name}
     */
    public void setOrderBy(String orderBy) {
        this.orderBy = TextUtil.trimToNull(orderBy);
    }

    /**
     * 是否设置了排序字段。
     *
     * @return true=调用方要求排序
     */
    public boolean hasOrderBy() {
        return orderBy != null;
    }

    /**
     * LIMIT 的偏移量：{@code (pageNo - 1) * pageSize}。
     *
     * @return 偏移量，第一页为 0
     */
    public int getOffset() {
        return (pageNo - 1) * pageSize;
    }

    /**
     * 把 {@link #getOrderBy()} 按白名单翻译成可以拼进 SQL 的 {@code ORDER BY} 片段。
     *
     * <p>规则：</p>
     * <ul>
     *   <li>没有设置排序字段时返回 {@code null}，由 DAO 使用默认排序；</li>
     *   <li>支持多列，用逗号分隔，例如 {@code sign_date DESC, project_name}；</li>
     *   <li>列名必须在白名单内（忽略大小写），且最终拼进 SQL 的是<b>白名单里的原始列名</b>，
     *       不是用户传入的字符串；</li>
     *   <li>方向只允许 {@code ASC / DESC}，不写时默认 {@code ASC}；</li>
     *   <li>任何不合法输入都抛 {@link IllegalArgumentException}，由 DAO 转成
     *       {@code Result.fail("排序字段不合法：...")}，绝不静默放行。</li>
     * </ul>
     *
     * @param allowedColumns 允许排序的列名白名单
     * @return 形如 {@code ORDER BY sign_date DESC, project_name ASC} 的片段；未设置排序时返回 null
     * @throws IllegalArgumentException 没有传白名单、列名不在白名单、排序方向非法、写法非法
     */
    public String resolveOrderBy(String... allowedColumns) {
        if (orderBy == null) {
            return null;
        }
        if (allowedColumns == null || allowedColumns.length == 0) {
            throw new IllegalArgumentException("使用 orderBy 之前必须提供排序字段白名单，当前值：" + orderBy);
        }
        StringBuilder fragment = new StringBuilder();
        String[] items = orderBy.split(",");
        for (int i = 0; i < items.length; i++) {
            String[] parts = items[i].trim().split("\\s+");
            if (parts.length == 0 || parts[0].isEmpty()) {
                continue;
            }
            if (parts.length > 2) {
                throw new IllegalArgumentException(
                        "排序写法错误：" + items[i].trim() + "（正确写法：project_code DESC）");
            }
            String column = matchColumn(parts[0], allowedColumns);
            String direction = "ASC";
            if (parts.length == 2) {
                direction = parts[1].toUpperCase(Locale.ROOT);
                if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
                    throw new IllegalArgumentException("排序方向只能写 ASC 或 DESC：" + items[i].trim());
                }
            }
            if (fragment.length() > 0) {
                fragment.append(", ");
            }
            fragment.append(column).append(' ').append(direction);
        }
        return fragment.length() == 0 ? null : "ORDER BY " + fragment;
    }

    private static String matchColumn(String column, String[] allowedColumns) {
        String candidate = column.trim();
        for (int i = 0; i < allowedColumns.length; i++) {
            String allowed = allowedColumns[i];
            if (allowed != null && allowed.trim().equalsIgnoreCase(candidate)) {
                return allowed.trim();
            }
        }
        StringBuilder hint = new StringBuilder();
        for (int i = 0; i < allowedColumns.length; i++) {
            if (allowedColumns[i] == null) {
                continue;
            }
            if (hint.length() > 0) {
                hint.append(", ");
            }
            hint.append(allowedColumns[i].trim());
        }
        throw new IllegalArgumentException("排序字段不在白名单中：" + candidate + "（可用：" + hint + "）");
    }

    @Override
    public String toString() {
        return "Page{pageNo=" + pageNo + ", pageSize=" + pageSize + ", orderBy='" + orderBy + "'}";
    }
}
