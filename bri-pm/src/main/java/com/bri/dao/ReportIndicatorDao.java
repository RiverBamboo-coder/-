package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.common.Result;
import com.bri.vo.IndicatorRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 报告指标 DAO（M6 · t_report_indicator，EAV 模型）。B 负责。
 *
 * <p>为什么是 EAV：不同项目要报的指标不一样（培训人次、本地采购额、安全工时…），
 * 用固定列就得不停 ALTER TABLE；用 (report_id, indicator_code) 做主键的
 * “实体-属性-值”表，加指标只是加数据。</p>
 *
 * <p>代价是“查某个项目的培训总人次”要 JOIN + SUM，这正是 C 的 M7 承诺比对在做的事情。</p>
 *
 * @author B
 */
public class ReportIndicatorDao {

    /**
     * 保存一个指标值：同一张报告同一指标只保留一条（存在则更新）。
     *
     * @param reportId      报告主键
     * @param indicatorCode 指标编码，如 TRAINING
     * @param value         指标值，不能为负
     * @return 受影响行数
     */
    public Result<Integer> saveIndicator(long reportId, String indicatorCode, BigDecimal value) {
        if (reportId <= 0) {
            return Result.fail("保存指标失败：报告主键缺失");
        }
        if (indicatorCode == null || indicatorCode.trim().isEmpty()) {
            return Result.fail("保存指标失败：指标编码不能为空");
        }
        if (value == null || value.signum() < 0) {
            return Result.fail("保存指标失败：指标值不能为负（数据库有 ck_indicator_val_nonneg 约束）");
        }
        String sql = """
                INSERT INTO t_report_indicator (report_id, indicator_code, indicator_val, unit)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE indicator_val = VALUES(indicator_val), unit = VALUES(unit)
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, reportId, indicatorCode.trim().toUpperCase(), value,
                    unitOf(indicatorCode));
            int rows = ps.executeUpdate();
            return Result.ok(rows, "指标已保存");
        } catch (SQLException e) {
            switch (e.getErrorCode()) {
                case 1452:
                    return Result.fail("保存指标失败：报告不存在（外键 fk_indicator_report 校验失败）");
                case 3819:
                    return Result.fail("保存指标失败：指标值不能为负");
                default:
                    return Result.fail("保存指标失败：" + e.getMessage());
            }
        }
    }

    /** 某张报告的全部指标（界面“指标明细”表格）。 */
    public List<IndicatorRow> findIndicatorsByReport(long reportId) {
        String sql = """
                SELECT i.report_id, r.period_ym, i.indicator_code, i.indicator_val, i.unit
                FROM t_report_indicator i
                JOIN t_progress_report r ON r.report_id = i.report_id
                WHERE i.report_id = ?
                ORDER BY i.indicator_code
                """;
        List<IndicatorRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IndicatorRow row = new IndicatorRow();
                    row.setReportId(rs.getLong("report_id"));
                    row.setPeriodYm(rs.getString("period_ym"));
                    row.setIndicatorCode(rs.getString("indicator_code"));
                    row.setIndicatorName(IndicatorRow.nameOf(row.getIndicatorCode()));
                    row.setIndicatorVal(rs.getBigDecimal("indicator_val"));
                    row.setUnit(rs.getString("unit"));
                    list.add(row);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查询报告指标失败：" + e.getMessage(), e);
        }
    }

    /** 某项目某指标的累计值（C 的 M7 承诺比对也会用同样的 SUM 口径）。 */
    public BigDecimal sumByProject(long projectId, String indicatorCode) {
        String sql = """
                SELECT COALESCE(SUM(i.indicator_val), 0)
                FROM t_report_indicator i
                JOIN t_progress_report r ON r.report_id = i.report_id
                WHERE r.project_id = ? AND i.indicator_code = ?
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, projectId, indicatorCode.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DataAccessException("累计报告指标失败：" + e.getMessage(), e);
        }
    }

    /** 删除某张报告的全部指标。 */
    public Result<Integer> deleteByReport(long reportId) {
        String sql = "DELETE FROM t_report_indicator WHERE report_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, reportId);
            int rows = ps.executeUpdate();
            return Result.ok(rows, rows == 0 ? "该报告没有指标数据" : "指标已清空");
        } catch (SQLException e) {
            return Result.fail("清空指标失败：" + e.getMessage());
        }
    }

    /** 指标默认单位（录入时自动带出，可被界面覆盖）。 */
    public static String unitOf(String indicatorCode) {
        if (indicatorCode == null) {
            return null;
        }
        switch (indicatorCode.trim().toUpperCase()) {
            case "TRAINING":
                return "人次";
            case "LOCAL_SUPPLY":
                return "万元";
            default:
                return null;
        }
    }
}
