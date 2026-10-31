package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.common.Page;
import com.bri.common.Result;
import com.bri.common.TextUtil;
import com.bri.vo.GapRow;
import com.bri.vo.OverdueProjectRow;
import com.bri.vo.ProjectQuery;
import com.bri.vo.ProjectVO;
import com.bri.vo.StatRow;
import com.bri.vo.StatusLogRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目主数据（M2）的数据访问对象。
 *
 * <p>对应第 5 章接口契约：{@code findByCondition(Query, Page)}、{@code findById(long)}、
 * {@code insert / update}、{@code statByFieldAndStatus()}、{@code findContractGapProjects()}。</p>
 *
 * <p>组合筛选用动态拼接 WHERE，但占位符全部是 {@code ?}，参数走 {@link DbUtil#setParameters}，
 * 不存在字符串拼接用户输入的情况；排序仍走 {@link Page#resolveOrderBy(String...)} 白名单。</p>
 *
 * @author A
 */
public class ProjectDao {

    private static final String SELECT_LIST =
            "SELECT p.project_id, p.project_code, p.project_name, p.country_code, c.country_name,"
            + " r.region_name, p.owner_enterprise_id, e.enterprise_name AS owner_name,"
            + " p.field_code, df.item_name AS field_name, p.mode_code, dm.item_name AS mode_name,"
            + " p.status, p.invest_amount, p.currency_code, p.sign_date,"
            + " p.planned_start_date, p.planned_end_date, p.actual_start_date, p.actual_end_date, p.remark";

    private static final String FROM =
            " FROM t_project p"
            + " JOIN t_country c ON c.country_code = p.country_code"
            + " LEFT JOIN t_region r ON r.region_id = c.region_id"
            + " LEFT JOIN t_enterprise e ON e.enterprise_id = p.owner_enterprise_id"
            + " LEFT JOIN t_dict_item df ON df.dict_type_id = 1 AND df.item_code = p.field_code"
            + " LEFT JOIN t_dict_item dm ON dm.dict_type_id = 2 AND dm.item_code = p.mode_code";

    private static final String[] ORDER_COLUMNS = {
            "project_code", "project_name", "country_code", "country_name", "status",
            "invest_amount", "sign_date", "planned_end_date", "actual_end_date"
    };

    /**
     * 四维组合筛选 + 关键字 + 日期区间 + 金额区间，支持分页与白名单排序。
     *
     * @param query 筛选条件，可为 null（等价于查全部）
     * @param page  分页与排序
     */
    public List<ProjectVO> findByCondition(ProjectQuery query, Page page) {
        Page p = page == null ? new Page() : page;
        List<Object> params = new ArrayList<>();
        String where = buildWhere(query, params);
        String order = p.resolveOrderBy(ORDER_COLUMNS);
        String sql = SELECT_LIST + FROM + where
                + (order == null ? " ORDER BY p.project_code" : " " + order)
                + " LIMIT ? OFFSET ?";
        params.add(p.getPageSize());
        params.add(p.getOffset());
        List<ProjectVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, params.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("按条件查询项目失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 符合条件的项目总数（分页栏用）。
     *
     * @param query 与列表相同的筛选条件
     */
    public long count(ProjectQuery query) {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(query, params);
        String sql = "SELECT COUNT(*)" + FROM + where;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, params.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计项目数量失败：" + e.getMessage(), e);
        }
    }

    /**
     * 按主键查项目详情。
     *
     * @param projectId 项目 ID
     * @return 找不到返回 null
     */
    public ProjectVO findById(long projectId) {
        String sql = SELECT_LIST + FROM + " WHERE p.project_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询项目详情失败：" + e.getMessage(), e);
        }
    }

    /**
     * 新增项目。
     *
     * @return 成功时 data 为自增主键，失败时 message 为中文原因
     */
    public Result<Long> insert(ProjectVO vo) {
        String invalid = validate(vo);
        if (invalid != null) {
            return Result.fail(invalid);
        }
        String sql = "INSERT INTO t_project (project_code, project_name, country_code, owner_enterprise_id,"
                + " field_code, mode_code, status, invest_amount, currency_code, sign_date,"
                + " planned_start_date, planned_end_date, actual_start_date, actual_end_date, remark)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            bindProject(ps, vo);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                long id = keys.next() ? keys.getLong(1) : 0L;
                return Result.ok(id, "新增项目成功" + (id > 0 ? "，项目 ID=" + id : ""));
            }
        } catch (SQLException e) {
            return Result.fail("新增项目失败：" + e.getMessage());
        }
    }

    /**
     * 修改项目（按 project_id）。
     */
    public Result<Long> update(ProjectVO vo) {
        String invalid = validate(vo);
        if (invalid != null) {
            return Result.fail(invalid);
        }
        String sql = "UPDATE t_project SET project_code = ?, project_name = ?, country_code = ?,"
                + " owner_enterprise_id = ?, field_code = ?, mode_code = ?, status = ?,"
                + " invest_amount = ?, currency_code = ?, sign_date = ?, planned_start_date = ?,"
                + " planned_end_date = ?, actual_start_date = ?, actual_end_date = ?, remark = ?"
                + " WHERE project_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindProject(ps, vo);
            ps.setLong(16, vo.getProjectId());
            int rows = ps.executeUpdate();
            return rows > 0 ? Result.ok(vo.getProjectId(), "修改项目成功") : Result.fail("修改失败：项目不存在或已被删除");
        } catch (SQLException e) {
            return Result.fail("修改项目失败：" + e.getMessage());
        }
    }

    /**
     * Q2-1：领域 × 状态 交叉统计。
     *
     * <p>返回行：{@code code = "领域编码|状态"}、{@code label = "领域名称 / 状态"}、
     * {@code cnt} 项目数、{@code total} 投资额合计。</p>
     */
    public List<StatRow> statByFieldAndStatus() {
        String sql = "SELECT CONCAT(p.field_code, '|', p.status) AS code,"
                + " CONCAT(COALESCE(df.item_name, p.field_code), ' / ', p.status) AS label,"
                + " COUNT(*) AS cnt, COALESCE(SUM(p.invest_amount), 0) AS total"
                + " FROM t_project p"
                + " LEFT JOIN t_dict_item df ON df.dict_type_id = 1 AND df.item_code = p.field_code"
                + " GROUP BY p.field_code, p.status, df.item_name"
                + " ORDER BY p.field_code, p.status";
        List<StatRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StatRow row = new StatRow();
                row.setCode(rs.getString("code"));
                row.setLabel(rs.getString("label"));
                row.setCnt(rs.getLong("cnt"));
                row.setTotal(rs.getBigDecimal("total"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计领域与状态失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q2-2：合同金额合计与项目投资额不一致的项目（业务规则 R7 的一致性校验）。
     */
    public List<GapRow> findContractGapProjects() {
        String sql = "SELECT p.project_id, p.project_code, p.project_name, p.invest_amount,"
                + " COALESCE(SUM(c.amount), 0) AS contract_sum,"
                + " p.invest_amount - COALESCE(SUM(c.amount), 0) AS gap,"
                + " ROUND(100.0 * (p.invest_amount - COALESCE(SUM(c.amount), 0))"
                + "       / NULLIF(p.invest_amount, 0), 2) AS gap_pct"
                + " FROM t_project p LEFT JOIN t_contract c ON c.project_id = p.project_id"
                + " GROUP BY p.project_id, p.project_code, p.project_name, p.invest_amount"
                + " HAVING ABS(p.invest_amount - COALESCE(SUM(c.amount), 0)) > 0.01"
                + " ORDER BY ABS(p.invest_amount - COALESCE(SUM(c.amount), 0)) DESC";
        List<GapRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GapRow row = new GapRow();
                row.setProjectId(rs.getLong("project_id"));
                row.setProjectCode(rs.getString("project_code"));
                row.setProjectName(rs.getString("project_name"));
                row.setInvestAmount(rs.getBigDecimal("invest_amount"));
                row.setContractSum(rs.getBigDecimal("contract_sum"));
                row.setGap(rs.getBigDecimal("gap"));
                row.setGapPct(rs.getBigDecimal("gap_pct"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("一致性校验查询失败：" + e.getMessage(), e);
        }
        return list;
    }

    private String buildWhere(ProjectQuery q, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (q == null) {
            return where.toString();
        }
        if (TextUtil.isNotBlank(q.getKeyword())) {
            String like = TextUtil.containsLike(q.getKeyword());
            where.append(" AND (p.project_code LIKE ?").append(TextUtil.LIKE_ESCAPE_SQL)
                    .append(" OR p.project_name LIKE ?").append(TextUtil.LIKE_ESCAPE_SQL).append(")");
            params.add(like);
            params.add(like);
        }
        if (TextUtil.isNotBlank(q.getCountryCode())) {
            where.append(" AND p.country_code = ?");
            params.add(q.getCountryCode().trim());
        }
        if (TextUtil.isNotBlank(q.getFieldCode())) {
            where.append(" AND p.field_code = ?");
            params.add(q.getFieldCode().trim());
        }
        if (TextUtil.isNotBlank(q.getModeCode())) {
            where.append(" AND p.mode_code = ?");
            params.add(q.getModeCode().trim());
        }
        if (TextUtil.isNotBlank(q.getStatus())) {
            where.append(" AND p.status = ?");
            params.add(q.getStatus().trim());
        }
        if (TextUtil.isNotBlank(q.getContinentCode())) {
            where.append(" AND EXISTS (SELECT 1 FROM t_region cont"
                    + " WHERE cont.region_id = r.parent_id AND cont.region_code = ?)");
            params.add(q.getContinentCode().trim());
        }
        if (q.getInvestMin() != null) {
            where.append(" AND p.invest_amount >= ?");
            params.add(q.getInvestMin());
        }
        if (q.getInvestMax() != null) {
            where.append(" AND p.invest_amount <= ?");
            params.add(q.getInvestMax());
        }
        if (q.getSignFrom() != null) {
            where.append(" AND p.sign_date >= ?");
            params.add(q.getSignFrom());
        }
        if (q.getSignTo() != null) {
            where.append(" AND p.sign_date <= ?");
            params.add(q.getSignTo());
        }
        return where.toString();
    }

    /**
     * 写库前的必填校验：返回 null 表示通过，否则返回可直接展示的中文原因。
     * 这样界面拿到的错误是“总投资必须大于 0”，而不是 MySQL 的 1048/1452 错误码。
     */
    private static String validate(ProjectVO vo) {
        if (vo == null) {
            return "项目数据不能为空";
        }
        if (TextUtil.isBlank(vo.getProjectCode())) {
            return "项目编码不能为空";
        }
        if (TextUtil.isBlank(vo.getProjectName())) {
            return "项目名称不能为空";
        }
        if (TextUtil.isBlank(vo.getCountryCode())) {
            return "东道国不能为空";
        }
        if (TextUtil.isBlank(vo.getFieldCode())) {
            return "领域不能为空";
        }
        if (TextUtil.isBlank(vo.getModeCode())) {
            return "合作模式不能为空";
        }
        if (TextUtil.isBlank(vo.getStatus())) {
            return "项目状态不能为空";
        }
        if (vo.getInvestAmount() == null || vo.getInvestAmount().signum() <= 0) {
            return "总投资必须大于 0";
        }
        return null;
    }

    private void bindProject(PreparedStatement ps, ProjectVO vo) throws SQLException {
        ps.setString(1, vo.getProjectCode());
        ps.setString(2, vo.getProjectName());
        ps.setString(3, vo.getCountryCode());
        if (vo.getOwnerEnterpriseId() == null) {
            ps.setNull(4, Types.BIGINT);
        } else {
            ps.setLong(4, vo.getOwnerEnterpriseId());
        }
        ps.setString(5, vo.getFieldCode());
        ps.setString(6, vo.getModeCode());
        ps.setString(7, vo.getStatus());
        ps.setBigDecimal(8, vo.getInvestAmount());
        // 币种缺省用 CNY：显式绑定 NULL 会绕过列默认值，直接触发 1048
        ps.setString(9, TextUtil.isBlank(vo.getCurrencyCode()) ? "CNY" : vo.getCurrencyCode());
        ps.setDate(10, toSqlDate(vo.getSignDate()));
        ps.setDate(11, toSqlDate(vo.getPlannedStartDate()));
        ps.setDate(12, toSqlDate(vo.getPlannedEndDate()));
        ps.setDate(13, toSqlDate(vo.getActualStartDate()));
        ps.setDate(14, toSqlDate(vo.getActualEndDate()));
        ps.setString(15, vo.getRemark());
    }

    private static java.sql.Date toSqlDate(java.time.LocalDate d) {
        return d == null ? null : java.sql.Date.valueOf(d);
    }

    private ProjectVO map(ResultSet rs) throws SQLException {
        ProjectVO vo = new ProjectVO();
        vo.setProjectId(rs.getLong("project_id"));
        vo.setProjectCode(rs.getString("project_code"));
        vo.setProjectName(rs.getString("project_name"));
        vo.setCountryCode(rs.getString("country_code"));
        vo.setCountryName(rs.getString("country_name"));
        vo.setRegionName(rs.getString("region_name"));
        long ownerId = rs.getLong("owner_enterprise_id");
        vo.setOwnerEnterpriseId(rs.wasNull() ? null : ownerId);
        vo.setOwnerName(rs.getString("owner_name"));
        vo.setFieldCode(rs.getString("field_code"));
        vo.setFieldName(rs.getString("field_name"));
        vo.setModeCode(rs.getString("mode_code"));
        vo.setModeName(rs.getString("mode_name"));
        vo.setStatus(rs.getString("status"));
        vo.setInvestAmount(rs.getBigDecimal("invest_amount"));
        vo.setCurrencyCode(rs.getString("currency_code"));
        vo.setSignDate(toLocalDate(rs.getDate("sign_date")));
        vo.setPlannedStartDate(toLocalDate(rs.getDate("planned_start_date")));
        vo.setPlannedEndDate(toLocalDate(rs.getDate("planned_end_date")));
        vo.setActualStartDate(toLocalDate(rs.getDate("actual_start_date")));
        vo.setActualEndDate(toLocalDate(rs.getDate("actual_end_date")));
        vo.setRemark(rs.getString("remark"));
        return vo;
    }

    // ------------------------------------------------------------------
    // Q2-3 / Q2-4 / Q2-5（本轮补齐：年度趋势、逾期未移交、状态变更轨迹）
    // ------------------------------------------------------------------

    /**
     * Q2-3：按签约年份统计新签项目数与金额（年度趋势）。
     *
     * <p>用 {@code DATE_FORMAT(sign_date,'%Y')} 分组而不是 {@code YEAR()}，
     * 让 SELECT 与 GROUP BY 是同一个表达式，在 {@code ONLY_FULL_GROUP_BY} 下也不会报错。
     * 未签约的项目（sign_date 为空）不参与趋势统计。</p>
     *
     * @return 按年份升序的统计行：code/label 为年份，cnt 为新签项目数，total 为签约金额
     */
    public List<StatRow> statNewProjectsByYear() {
        String sql = "SELECT DATE_FORMAT(p.sign_date, '%Y') AS code,"
                + " CONCAT(DATE_FORMAT(p.sign_date, '%Y'), ' 年') AS label,"
                + " COUNT(*) AS cnt, COALESCE(SUM(p.invest_amount), 0) AS total"
                + " FROM t_project p"
                + " WHERE p.sign_date IS NOT NULL"
                + " GROUP BY DATE_FORMAT(p.sign_date, '%Y')"
                + " ORDER BY code";
        List<StatRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StatRow row = new StatRow();
                row.setCode(rs.getString("code"));
                row.setLabel(rs.getString("label"));
                row.setCnt(rs.getLong("cnt"));
                row.setTotal(rs.getBigDecimal("total"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("按年份统计新签项目失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q2-4：已过计划竣工日期、但仍未移交的项目（工期失控的输入）。
     *
     * <p>判定口径：{@code planned_end_date < CURDATE()} 且 {@code status <> '已移交'}。
     * 逾期天数由数据库算（{@code DATEDIFF}），与里程碑的“超期天数”保持同一口径，
     * 不在 Java 里再算一遍，避免显示值与库中值不一致。</p>
     *
     * @return 按逾期天数降序的项目列表
     */
    public List<OverdueProjectRow> findOverdueNotTransferred() {
        String sql = "SELECT p.project_id, p.project_code, p.project_name,"
                + " c.country_name, p.status, p.planned_end_date,"
                + " DATEDIFF(CURDATE(), p.planned_end_date) AS overdue_days"
                + " FROM t_project p"
                + " JOIN t_country c ON c.country_code = p.country_code"
                + " WHERE p.planned_end_date IS NOT NULL"
                + "   AND p.planned_end_date < CURDATE()"
                + "   AND p.status <> '已移交'"
                + " ORDER BY overdue_days DESC, p.project_code";
        List<OverdueProjectRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                OverdueProjectRow row = new OverdueProjectRow();
                row.setProjectId(rs.getLong("project_id"));
                row.setProjectCode(rs.getString("project_code"));
                row.setProjectName(rs.getString("project_name"));
                row.setCountryName(rs.getString("country_name"));
                row.setStatus(rs.getString("status"));
                row.setPlannedEndDate(toLocalDate(rs.getDate("planned_end_date")));
                row.setOverdueDays(rs.getLong("overdue_days"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询逾期未移交项目失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q2-5：某个项目的状态变更轨迹（界面画“状态时间线”用）。
     *
     * <p>项目主表只存当前状态，历史全部落在 {@code t_project_status_log}，
     * 这样既满足 3NF，又能回答“什么时候从在建变成竣工的”。
     * 按变更日期正序返回，第一条的 from_status 通常为空（项目首次入库）。</p>
     *
     * @param projectId 项目主键
     * @return 轨迹列表；无记录时返回空列表
     */
    public List<StatusLogRow> findStatusLogs(long projectId) {
        String sql = "SELECT l.log_id, l.project_id, p.project_code, p.project_name,"
                + " l.from_status, l.to_status, l.change_date, l.operator, l.remark"
                + " FROM t_project_status_log l"
                + " JOIN t_project p ON p.project_id = l.project_id"
                + " WHERE l.project_id = ?"
                + " ORDER BY l.change_date, l.log_id";
        List<StatusLogRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapStatusLog(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询项目状态轨迹失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q2-5（全局）：所有项目的状态变更轨迹，支持分页与白名单排序。
     *
     * @param page 分页与排序，可为 null（默认第 1 页、20 条、按变更日期倒序）
     * @return 轨迹列表，最近的在最上面
     */
    public List<StatusLogRow> findStatusLogs(Page page) {
        Page p = page == null ? new Page() : page;
        String order = p.resolveOrderBy("change_date", "to_status", "from_status", "operator");
        String sql = "SELECT l.log_id, l.project_id, p.project_code, p.project_name,"
                + " l.from_status, l.to_status, l.change_date, l.operator, l.remark"
                + " FROM t_project_status_log l"
                + " JOIN t_project p ON p.project_id = l.project_id"
                + (order == null ? " ORDER BY l.change_date DESC, l.log_id DESC" : " " + order)
                + " LIMIT ? OFFSET ?";
        List<StatusLogRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getPageSize());
            ps.setInt(2, p.getOffset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapStatusLog(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("分页查询状态轨迹失败：" + e.getMessage(), e);
        }
        return list;
    }

    private StatusLogRow mapStatusLog(ResultSet rs) throws SQLException {
        StatusLogRow row = new StatusLogRow();
        row.setLogId(rs.getLong("log_id"));
        row.setProjectId(rs.getLong("project_id"));
        row.setProjectCode(rs.getString("project_code"));
        row.setProjectName(rs.getString("project_name"));
        row.setFromStatus(rs.getString("from_status"));
        row.setToStatus(rs.getString("to_status"));
        row.setChangeDate(toLocalDateTime(rs.getTimestamp("change_date")));
        row.setOperator(rs.getString("operator"));
        row.setRemark(rs.getString("remark"));
        return row;
    }

    private static java.time.LocalDate toLocalDate(java.sql.Date d) {
        return d == null ? null : d.toLocalDate();
    }

    private static java.time.LocalDateTime toLocalDateTime(java.sql.Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
