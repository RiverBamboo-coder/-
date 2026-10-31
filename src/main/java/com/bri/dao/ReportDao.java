package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.common.Page;
import com.bri.common.Result;
import com.bri.common.TextUtil;
import com.bri.vo.HireRow;
import com.bri.vo.MoMRow;
import com.bri.vo.OutRow;
import com.bri.vo.ReportVO;
import com.bri.vo.StagnantRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 履约进度报告 DAO（M6 · t_progress_report）。B 负责。
 *
 * <p>两个“必须有”的数据库级保证：</p>
 * <ol>
 *   <li>{@code uk_report_project_period}：同项目同月只能报一次，DAO 把 1062 翻成
 *       “该项目该报告期已报过，请改为修改”；</li>
 *   <li>{@code ck_report_local_le_onsite}：属地员工不能多于现场总人数。</li>
 * </ol>
 *
 * @author B
 */
public class ReportDao {

    private static final String SQL_SELECT = """
            SELECT r.report_id, r.project_id, p.project_code, p.project_name,
                   r.period_ym, r.progress_pct, r.on_site_staff, r.local_staff,
                   ROUND(100.0 * r.local_staff / NULLIF(r.on_site_staff, 0), 2) AS local_hire_pct,
                   r.output_value, r.report_date, r.reporter, r.remark
            FROM t_progress_report r
            JOIN t_project p ON p.project_id = r.project_id
            """;

    /** 某项目的全部月报（按报告期倒序，界面主列表）。 */
    public List<ReportVO> findReportsByProject(long projectId) {
        String sql = SQL_SELECT + " WHERE r.project_id = ? ORDER BY r.period_ym DESC";
        List<ReportVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查询进度月报失败：" + e.getMessage(), e);
        }
    }

    /** 分页查询月报（{@code periodYm} 可空）。 */
    public List<ReportVO> findPage(Long projectId, String periodYm, Page page) {
        StringBuilder sql = new StringBuilder(SQL_SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (projectId != null && projectId > 0) {
            sql.append(" AND r.project_id = ?");
            params.add(projectId);
        }
        if (TextUtil.isNotBlank(periodYm)) {
            sql.append(" AND r.period_ym = ?");
            params.add(periodYm.trim());
        }
        String order = page.resolveOrderBy("period_ym", "progress_pct", "output_value");
        sql.append(order == null ? " ORDER BY r.period_ym DESC, r.project_id" : " " + order);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(page.getPageSize());
        params.add(page.getOffset());

        List<ReportVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DbUtil.setParameters(ps, params.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("分页查询进度月报失败：" + e.getMessage(), e);
        }
    }

    /** 某项目的最新一期月报，没有报告时返回 null（预警计算要用）。 */
    public ReportVO findLatest(long projectId) {
        String sql = SQL_SELECT + " WHERE r.project_id = ? ORDER BY r.period_ym DESC LIMIT 1";
        List<ReportVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            throw new DataAccessException("查询最新月报失败：" + e.getMessage(), e);
        }
    }

    /**
     * 填报一期月报。
     *
     * @param vo 月报（projectId / periodYm / progressPct 必填）
     * @return 成功返回主键，重复填报返回 {@code Result.fail("该项目 2025-06 已报过进度…")}
     */
    public Result<Long> insertReport(ReportVO vo) {
        if (vo == null || vo.getProjectId() <= 0) {
            return Result.fail("填报失败：请选择项目");
        }
        if (TextUtil.isBlank(vo.getPeriodYm())) {
            return Result.fail("填报失败：报告期不能为空（格式 YYYY-MM）");
        }
        if (vo.getProgressPct() == null) {
            return Result.fail("填报失败：形象进度不能为空");
        }
        String sql = """
                INSERT INTO t_progress_report
                    (project_id, period_ym, progress_pct, on_site_staff, local_staff,
                     output_value, report_date, reporter, remark)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            DbUtil.setParameters(ps,
                    vo.getProjectId(),
                    vo.getPeriodYm().trim(),
                    vo.getProgressPct(),
                    vo.getOnSiteStaff(),
                    vo.getLocalStaff(),
                    vo.getOutputValue() == null ? BigDecimal.ZERO : vo.getOutputValue(),
                    vo.getReportDate(),
                    TextUtil.trimToNull(vo.getReporter()),
                    TextUtil.trimToNull(vo.getRemark()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return Result.ok(keys.getLong(1), "月报已填报");
                }
            }
            return Result.ok(null, "月报已填报");
        } catch (SQLException e) {
            return Result.fail("填报月报失败：" + friendly(e, vo.getPeriodYm()));
        }
    }

    /** 修改月报（报告期不允许改，改期等于换一条记录）。 */
    public Result<Integer> updateReport(ReportVO vo) {
        if (vo == null || vo.getReportId() <= 0) {
            return Result.fail("修改失败：报告主键缺失");
        }
        String sql = """
                UPDATE t_progress_report
                   SET progress_pct = ?, on_site_staff = ?, local_staff = ?,
                       output_value = ?, report_date = ?, reporter = ?, remark = ?
                 WHERE report_id = ?
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps,
                    vo.getProgressPct(),
                    vo.getOnSiteStaff(),
                    vo.getLocalStaff(),
                    vo.getOutputValue() == null ? BigDecimal.ZERO : vo.getOutputValue(),
                    vo.getReportDate(),
                    TextUtil.trimToNull(vo.getReporter()),
                    TextUtil.trimToNull(vo.getRemark()),
                    vo.getReportId());
            int rows = ps.executeUpdate();
            return Result.ok(rows, rows == 0 ? "没有找到要修改的月报" : "月报已修改");
        } catch (SQLException e) {
            return Result.fail("修改月报失败：" + friendly(e, null));
        }
    }

    /** 删除月报（连带 t_report_indicator 由外键 ON DELETE CASCADE 处理）。 */
    public Result<Integer> deleteReport(long reportId) {
        String sql = "DELETE FROM t_progress_report WHERE report_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, reportId);
            int rows = ps.executeUpdate();
            return Result.ok(rows, rows == 0 ? "没有找到该月报" : "月报已删除");
        } catch (SQLException e) {
            return Result.fail("删除月报失败：" + friendly(e, null));
        }
    }

    /**
     * Q6-1 进度环比：LAG 窗口函数一次算出“上一期”和“本期增减”，不用在 Java 里倒序循环。
     *
     * @param projectId 项目主键
     */
    public List<MoMRow> statMonthlyMoM(long projectId) {
        String sql = """
                SELECT r.report_id, r.project_id, p.project_code, r.period_ym, r.progress_pct,
                       LAG(r.progress_pct) OVER (PARTITION BY r.project_id ORDER BY r.period_ym) AS prev_pct,
                       r.progress_pct - LAG(r.progress_pct) OVER (PARTITION BY r.project_id ORDER BY r.period_ym) AS mom_delta
                FROM t_progress_report r
                JOIN t_project p ON p.project_id = r.project_id
                WHERE r.project_id = ?
                ORDER BY r.period_ym
                """;
        List<MoMRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MoMRow row = new MoMRow();
                    row.setReportId(rs.getLong("report_id"));
                    row.setProjectId(rs.getLong("project_id"));
                    row.setProjectCode(rs.getString("project_code"));
                    row.setPeriodYm(rs.getString("period_ym"));
                    row.setProgressPct(rs.getBigDecimal("progress_pct"));
                    row.setPrevPct(rs.getBigDecimal("prev_pct"));
                    row.setMomDelta(rs.getBigDecimal("mom_delta"));
                    list.add(row);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("统计进度环比失败：" + e.getMessage(), e);
        }
    }

    /** Q6-2 国别累计产值（D 的柱状图数据源）。 */
    public List<OutRow> statOutputByCountry() {
        String sql = """
                SELECT c.country_code, c.country_name,
                       COUNT(DISTINCT r.project_id)       AS project_cnt,
                       SUM(r.output_value)                AS output_total,
                       ROUND(AVG(r.progress_pct), 2)      AS avg_progress
                FROM t_progress_report r
                JOIN t_project p ON p.project_id = r.project_id
                JOIN t_country c ON c.country_code = p.country_code
                GROUP BY c.country_code, c.country_name
                ORDER BY output_total DESC
                """;
        List<OutRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                OutRow row = new OutRow();
                row.setCountryCode(rs.getString("country_code"));
                row.setCountryName(rs.getString("country_name"));
                row.setProjectCnt(rs.getInt("project_cnt"));
                row.setOutputTotal(rs.getBigDecimal("output_total"));
                row.setAvgProgress(rs.getBigDecimal("avg_progress"));
                list.add(row);
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("统计国别产值失败：" + e.getMessage(), e);
        }
    }

    /**
     * Q6-3 连续 {@code months} 个月进度不涨的项目（停滞项目）。
     *
     * <p>用 gaps-and-islands：同一项目同一进度值的行号差相等即为一个“连续段”，
     * 再要求这个段的期末等于该项目最新报告期（即“现在还在停”）。
     * 比“用 LAG 数一数”更准：中间涨过再停的，不会被算成一段。</p>
     *
     * @param months 连续月数阈值，例如 3
     */
    public List<StagnantRow> findStagnantProjects(int months) {
        String sql = """
                WITH islands AS (
                    SELECT r.project_id, r.period_ym, r.progress_pct,
                           ROW_NUMBER() OVER (PARTITION BY r.project_id ORDER BY r.period_ym)
                         - ROW_NUMBER() OVER (PARTITION BY r.project_id, r.progress_pct ORDER BY r.period_ym) AS grp
                    FROM t_progress_report r
                ), agg AS (
                    SELECT project_id, grp, progress_pct,
                           COUNT(*)          AS months,
                           MIN(period_ym)    AS start_ym,
                           MAX(period_ym)    AS end_ym
                    FROM islands
                    GROUP BY project_id, grp, progress_pct
                )
                SELECT a.project_id, p.project_code, p.project_name,
                       a.start_ym, a.end_ym, a.months, a.progress_pct
                FROM agg a
                JOIN t_project p ON p.project_id = a.project_id
                JOIN (SELECT project_id, MAX(period_ym) AS ym
                      FROM t_progress_report GROUP BY project_id) l
                  ON l.project_id = a.project_id AND l.ym = a.end_ym
                WHERE a.months >= ?
                ORDER BY a.months DESC, p.project_code
                """;
        List<StagnantRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StagnantRow row = new StagnantRow();
                    row.setProjectId(rs.getLong("project_id"));
                    row.setProjectCode(rs.getString("project_code"));
                    row.setProjectName(rs.getString("project_name"));
                    row.setStartYm(rs.getString("start_ym"));
                    row.setEndYm(rs.getString("end_ym"));
                    row.setMonths(rs.getInt("months"));
                    row.setProgressPct(rs.getBigDecimal("progress_pct"));
                    list.add(row);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查询停滞项目失败：" + e.getMessage(), e);
        }
    }

    /**
     * Q6-4 属地用工占比偏低（取每个项目最新一期月报，避免历史月份重复）。
     *
     * @param pct 百分比阈值，例如 50 表示 50%
     */
    public List<HireRow> findLowLocalHireRate(double pct) {
        String sql = """
                SELECT r.project_id, p.project_code, p.project_name, r.period_ym,
                       r.local_staff, r.on_site_staff,
                       ROUND(100.0 * r.local_staff / NULLIF(r.on_site_staff, 0), 2) AS local_hire_pct
                FROM t_progress_report r
                JOIN t_project p ON p.project_id = r.project_id
                JOIN (SELECT project_id, MAX(period_ym) AS ym
                      FROM t_progress_report GROUP BY project_id) l
                  ON l.project_id = r.project_id AND l.ym = r.period_ym
                WHERE r.on_site_staff > 0
                  AND 100.0 * r.local_staff / r.on_site_staff < ?
                ORDER BY local_hire_pct, p.project_code
                """;
        List<HireRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, BigDecimal.valueOf(pct));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HireRow row = new HireRow();
                    row.setProjectId(rs.getLong("project_id"));
                    row.setProjectCode(rs.getString("project_code"));
                    row.setProjectName(rs.getString("project_name"));
                    row.setPeriodYm(rs.getString("period_ym"));
                    row.setLocalStaff(rs.getInt("local_staff"));
                    row.setOnSiteStaff(rs.getInt("on_site_staff"));
                    row.setLocalHirePct(rs.getBigDecimal("local_hire_pct"));
                    list.add(row);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查询属地用工偏低项目失败：" + e.getMessage(), e);
        }
    }

    private static String friendly(SQLException e, String periodYm) {
        switch (e.getErrorCode()) {
            case 1062:
                return "该项目 " + (periodYm == null ? "该报告期" : periodYm) + " 已报过进度（唯一约束 uk_report_project_period），请改为修改";
            case 1452:
                return "项目不存在（外键 fk_report_project 校验失败）";
            case 3819:
                return "数据不满足检查约束：进度须在 0–100、属地员工不能多于现场总人数、报告期格式须为 YYYY-MM";
            default:
                return e.getMessage();
        }
    }

    private static ReportVO map(ResultSet rs) throws SQLException {
        ReportVO vo = new ReportVO();
        vo.setReportId(rs.getLong("report_id"));
        vo.setProjectId(rs.getLong("project_id"));
        vo.setProjectCode(rs.getString("project_code"));
        vo.setProjectName(rs.getString("project_name"));
        vo.setPeriodYm(rs.getString("period_ym"));
        vo.setProgressPct(rs.getBigDecimal("progress_pct"));
        vo.setOnSiteStaff(rs.getInt("on_site_staff"));
        vo.setLocalStaff(rs.getInt("local_staff"));
        vo.setLocalHirePct(rs.getBigDecimal("local_hire_pct"));
        vo.setOutputValue(rs.getBigDecimal("output_value"));
        Date reportDate = rs.getDate("report_date");
        vo.setReportDate(reportDate == null ? null : reportDate.toLocalDate());
        vo.setReporter(rs.getString("reporter"));
        vo.setRemark(rs.getString("remark"));
        return vo;
    }
}
