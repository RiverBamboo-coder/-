package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.common.Page;
import com.bri.common.Result;
import com.bri.common.TextUtil;
import com.bri.vo.DisbVO;
import com.bri.vo.FundPlanVO;
import com.bri.vo.OverrunRow;
import com.bri.vo.RateRow;
import com.bri.vo.SourceRow;
import com.bri.vo.YearRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 资金 DAO（M4 · t_fund_plan / t_fund_disbursement）。B 负责。
 *
 * <p>接口契约见任务书 5.2，D 直接按这些方法名调用；计划相关方法在本类里做转发，
 * 这样 D 只依赖 {@code FundDao} 一个入口，具体实现仍分在 {@link FundPlanDao}。</p>
 *
 * <p><b>本类的核心是“算在 SQL 里，不在 Java 里循环”</b>：
 * 到位率、来源占比、年度计划对比、超计划比例四个统计全部是一次 SQL 聚合 + 窗口函数，
 * 200 多行拨付数据也只是 4 次查询，不存在 N+1。</p>
 *
 * @author B
 */
public class FundDao {

    private final FundPlanDao planDao = new FundPlanDao();

    // ------------------------------------------------------------------
    // 一、年度计划（转发给 FundPlanDao，保持 D 只认 FundDao 一个入口）
    // ------------------------------------------------------------------

    /** 某项目的年度计划列表。 */
    public List<FundPlanVO> findPlansByProject(long projectId) {
        return planDao.findByProject(projectId);
    }

    /** 保存年度计划（有则更新、无则插入）。 */
    public Result<Long> upsertPlan(FundPlanVO plan) {
        return planDao.upsert(plan);
    }

    // ------------------------------------------------------------------
    // 二、拨付流水
    // ------------------------------------------------------------------

    private static final String SQL_DISB_SELECT = """
            SELECT d.disb_id, d.project_id, p.project_code, p.project_name,
                   d.source_id, s.source_name, d.plan_id, pl.plan_year,
                   d.disb_date, d.disb_amount, d.currency_code, d.purpose
            FROM t_fund_disbursement d
            JOIN t_project        p  ON p.project_id = d.project_id
            JOIN t_funding_source s  ON s.source_id  = d.source_id
            LEFT JOIN t_fund_plan pl ON pl.plan_id   = d.plan_id
            """;

    /**
     * 某项目的拨付流水（分页，默认按日期倒序）。
     *
     * @param projectId 项目主键
     * @param page      分页与排序（白名单：disb_date / disb_amount / purpose）
     * @return 当前页数据
     */
    public List<DisbVO> findDisbByProject(long projectId, Page page) {
        String order = page.resolveOrderBy("disb_date", "disb_amount", "purpose");
        String sql = SQL_DISB_SELECT
                + " WHERE d.project_id = ?"
                + (order == null ? " ORDER BY d.disb_date DESC, d.disb_id DESC" : " " + order)
                + " LIMIT ? OFFSET ?";
        return queryDisb(sql, projectId, page.getPageSize(), page.getOffset());
    }

    /** 某项目的拨付流水条数（分页页码用）。 */
    public long countDisbByProject(long projectId) {
        String sql = "SELECT COUNT(*) FROM t_fund_disbursement WHERE project_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计拨付笔数失败：" + e.getMessage(), e);
        }
    }

    /** 按拨付日期区间查询（M4 顶部“2024-01-01 ~ 2024-12-31”筛选）。 */
    public List<DisbVO> findDisbByDate(LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(SQL_DISB_SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (from != null) {
            sql.append(" AND d.disb_date >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND d.disb_date <= ?");
            params.add(to);
        }
        sql.append(" ORDER BY d.disb_date DESC, d.disb_id DESC");
        return queryDisb(sql.toString(), params.toArray());
    }

    /**
     * 登记一笔拨付。
     *
     * @param vo 拨付数据（projectId / sourceId / disbDate / disbAmount 必填）
     * @return 成功返回新主键，失败返回中文原因
     */
    public Result<Long> insertDisbursement(DisbVO vo) {
        if (vo == null || vo.getProjectId() <= 0) {
            return Result.fail("登记失败：请选择项目");
        }
        if (vo.getSourceId() <= 0) {
            return Result.fail("登记失败：请选择资金来源");
        }
        if (vo.getDisbDate() == null) {
            return Result.fail("登记失败：拨付日期不能为空");
        }
        if (vo.getDisbAmount() == null || vo.getDisbAmount().signum() <= 0) {
            return Result.fail("登记失败：拨付金额必须大于 0（数据库有 ck_disb_amount_positive 约束）");
        }
        String sql = """
                INSERT INTO t_fund_disbursement
                    (project_id, source_id, plan_id, disb_date, disb_amount, currency_code, purpose)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            DbUtil.setParameters(ps,
                    vo.getProjectId(),
                    vo.getSourceId(),
                    vo.getPlanId(),
                    vo.getDisbDate(),
                    vo.getDisbAmount(),
                    TextUtil.defaultIfBlank(vo.getCurrencyCode(), "CNY"),
                    TextUtil.trimToNull(vo.getPurpose()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return Result.ok(keys.getLong(1), "拨付已登记");
                }
            }
            return Result.ok(null, "拨付已登记");
        } catch (SQLException e) {
            return Result.fail("登记拨付失败：" + friendly(e));
        }
    }

    /**
     * 删除一笔拨付（界面须二次确认）。
     *
     * @param disbId 拨付主键
     * @return 受影响行数
     */
    public Result<Integer> deleteDisbursement(long disbId) {
        String sql = "DELETE FROM t_fund_disbursement WHERE disb_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, disbId);
            int rows = ps.executeUpdate();
            return Result.ok(rows, rows == 0 ? "没有找到该笔拨付（可能已被删除）" : "拨付已删除");
        } catch (SQLException e) {
            return Result.fail("删除拨付失败：" + friendly(e));
        }
    }

    // ------------------------------------------------------------------
    // 三、业务统计（Q4-1 ~ Q4-5）：一次 SQL 出结果
    // ------------------------------------------------------------------

    private static final String SQL_RATE = """
            SELECT project_id, project_code, project_name, status,
                   plan_total, actual_total, rate_pct
            FROM v_fund_rate
            """;

    /** Q4-1 项目资金到位率（汇总口径：取全周期计划合计 vs 拨付合计）。 */
    public List<RateRow> statFundRateByProject() {
        String sql = SQL_RATE + " ORDER BY rate_pct IS NULL, rate_pct, project_code";
        return queryRate(sql);
    }

    /**
     * Q4-2 到位率低于阈值的项目（阈值由界面输入，例如 60 表示 60%）。
     *
     * @param threshold 百分比阈值
     */
    public List<RateRow> findLowRateProjects(double threshold) {
        String sql = SQL_RATE + " WHERE rate_pct IS NOT NULL AND rate_pct < ? ORDER BY rate_pct, project_code";
        return queryRate(sql, BigDecimal.valueOf(threshold));
    }

    /**
     * Q4-3 资金来源占比：窗口函数一次算合计占比与排名。
     *
     * <p>{@code SUM(SUM(x)) OVER ()} 是“聚合后再开窗”，MySQL 8 支持；
     * 不用它就得再套一层子查询。</p>
     */
    public List<SourceRow> statBySource() {
        String sql = """
                SELECT s.source_id, s.source_name, s.source_type,
                       SUM(d.disb_amount)                                              AS actual_total,
                       ROUND(100.0 * SUM(d.disb_amount) / SUM(SUM(d.disb_amount)) OVER (), 2) AS share_pct,
                       RANK() OVER (ORDER BY SUM(d.disb_amount) DESC)                   AS rank_no
                FROM t_fund_disbursement d
                JOIN t_funding_source s ON s.source_id = d.source_id
                GROUP BY s.source_id, s.source_name, s.source_type
                ORDER BY actual_total DESC
                """;
        List<SourceRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SourceRow row = new SourceRow();
                row.setSourceId(rs.getLong("source_id"));
                row.setSourceName(rs.getString("source_name"));
                row.setSourceType(rs.getString("source_type"));
                row.setActualTotal(rs.getBigDecimal("actual_total"));
                row.setSharePct(rs.getBigDecimal("share_pct"));
                row.setRankNo(rs.getInt("rank_no"));
                list.add(row);
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("统计资金来源占比失败：" + e.getMessage(), e);
        }
    }

    /**
     * Q4-4 年度计划 vs 实际拨付。
     *
     * <p>先把每个计划的拨付合计聚成一行再 JOIN 年度，避免“计划金额被多笔拨付重复计数”。</p>
     */
    public List<YearRow> statYearPlanVsActual() {
        String sql = """
                SELECT pl.plan_year,
                       SUM(pl.plan_amount)                                          AS plan_total,
                       COALESCE(SUM(a.actual_total), 0)                             AS actual_total,
                       CASE WHEN SUM(pl.plan_amount) = 0 THEN NULL
                            ELSE ROUND(100.0 * COALESCE(SUM(a.actual_total), 0) / SUM(pl.plan_amount), 2)
                       END                                                          AS rate_pct,
                       COUNT(DISTINCT pl.project_id)                                AS project_cnt
                FROM t_fund_plan pl
                LEFT JOIN (SELECT plan_id, SUM(disb_amount) AS actual_total
                           FROM t_fund_disbursement
                           WHERE plan_id IS NOT NULL
                           GROUP BY plan_id) a ON a.plan_id = pl.plan_id
                GROUP BY pl.plan_year
                ORDER BY pl.plan_year
                """;
        List<YearRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                YearRow row = new YearRow();
                row.setPlanYear(rs.getInt("plan_year"));
                row.setPlanTotal(rs.getBigDecimal("plan_total"));
                row.setActualTotal(rs.getBigDecimal("actual_total"));
                row.setRatePct(rs.getBigDecimal("rate_pct"));
                row.setProjectCnt(rs.getInt("project_cnt"));
                list.add(row);
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("统计年度计划与实际失败：" + e.getMessage(), e);
        }
    }

    /**
     * Q4-5 超计划拨付：同项目同年度，实际拨付超过计划的幅度大于 {@code pct}%。
     *
     * <p>口径说明：合同金额在 A 的 t_contract 里，本查询用“年度计划”作为控制数，
     * 两者若要对齐，只需把下面的 plan_amount 换成合同金额（见 docs/B-7 第 6 节）。</p>
     */
    public List<OverrunRow> findOverrunProjects(double pct) {
        String sql = """
                SELECT p.project_id, p.project_code, p.project_name,
                       pl.plan_year, pl.plan_amount,
                       COALESCE(a.actual_total, 0)                                  AS actual_amount,
                       CASE WHEN pl.plan_amount = 0 THEN NULL
                            ELSE ROUND(100.0 * (COALESCE(a.actual_total, 0) - pl.plan_amount) / pl.plan_amount, 2)
                       END                                                          AS overrun_pct
                FROM t_fund_plan pl
                JOIN t_project p ON p.project_id = pl.project_id
                LEFT JOIN (SELECT plan_id, SUM(disb_amount) AS actual_total
                           FROM t_fund_disbursement
                           WHERE plan_id IS NOT NULL
                           GROUP BY plan_id) a ON a.plan_id = pl.plan_id
                WHERE pl.plan_amount > 0
                  AND COALESCE(a.actual_total, 0) > pl.plan_amount * (1 + ? / 100.0)
                ORDER BY overrun_pct DESC, p.project_code
                """;
        List<OverrunRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, BigDecimal.valueOf(pct));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OverrunRow row = new OverrunRow();
                    row.setProjectId(rs.getLong("project_id"));
                    row.setProjectCode(rs.getString("project_code"));
                    row.setProjectName(rs.getString("project_name"));
                    row.setPlanYear(rs.getInt("plan_year"));
                    row.setPlanAmount(rs.getBigDecimal("plan_amount"));
                    row.setActualAmount(rs.getBigDecimal("actual_amount"));
                    row.setOverrunPct(rs.getBigDecimal("overrun_pct"));
                    list.add(row);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查询超计划拨付失败：" + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // 四、内部工具
    // ------------------------------------------------------------------

    private List<DisbVO> queryDisb(String sql, Object... params) {
        List<DisbVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDisb(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查询拨付流水失败：" + e.getMessage(), e);
        }
    }

    private List<RateRow> queryRate(String sql, Object... params) {
        List<RateRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RateRow row = new RateRow();
                    row.setProjectId(rs.getLong("project_id"));
                    row.setProjectCode(rs.getString("project_code"));
                    row.setProjectName(rs.getString("project_name"));
                    row.setStatus(rs.getString("status"));
                    row.setPlanTotal(rs.getBigDecimal("plan_total"));
                    row.setActualTotal(rs.getBigDecimal("actual_total"));
                    row.setRatePct(rs.getBigDecimal("rate_pct"));
                    list.add(row);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("统计资金到位率失败：" + e.getMessage(), e);
        }
    }

    private static DisbVO mapDisb(ResultSet rs) throws SQLException {
        DisbVO vo = new DisbVO();
        vo.setDisbId(rs.getLong("disb_id"));
        vo.setProjectId(rs.getLong("project_id"));
        vo.setProjectCode(rs.getString("project_code"));
        vo.setProjectName(rs.getString("project_name"));
        vo.setSourceId(rs.getLong("source_id"));
        vo.setSourceName(rs.getString("source_name"));
        long planId = rs.getLong("plan_id");
        vo.setPlanId(rs.wasNull() ? null : planId);
        int planYear = rs.getInt("plan_year");
        vo.setPlanYear(rs.wasNull() ? null : planYear);
        Date disbDate = rs.getDate("disb_date");
        vo.setDisbDate(disbDate == null ? null : disbDate.toLocalDate());
        vo.setDisbAmount(rs.getBigDecimal("disb_amount"));
        vo.setCurrencyCode(rs.getString("currency_code"));
        vo.setPurpose(rs.getString("purpose"));
        return vo;
    }

    private static String friendly(SQLException e) {
        switch (e.getErrorCode()) {
            case 1452:
                return "项目、资金来源或年度计划不存在（外键校验失败）";
            case 3819:
                return "数据不满足检查约束（拨付金额必须大于 0）";
            case 1048:
                return "必填字段为空（拨付日期/金额/币种不能为空）";
            default:
                return e.getMessage();
        }
    }
}
