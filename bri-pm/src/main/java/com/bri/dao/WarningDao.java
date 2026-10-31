package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.common.Page;
import com.bri.common.Result;
import com.bri.common.TextUtil;
import com.bri.vo.WarningRuleVO;
import com.bri.vo.WarningVO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预警 DAO（M8 · t_warning / t_warning_rule / v_fund_progress）。B 负责。
 *
 * <p><b>预警不是写死的 if，而是“规则表 + 指标快照 + 刷新”</b>：</p>
 * <ol>
 *   <li>从 t_warning_rule 读出启用规则（阈值是数据，改阈值不用改代码）；</li>
 *   <li>按指标批量取一次快照（4 个指标 = 4 条 SQL，不是每个项目一条）；</li>
 *   <li>逐规则逐项目比大小，命中就 upsert 进 t_warning；</li>
 *   <li>不再命中的 OPEN 预警自动置为 CLOSED（条件消失就解除，不是留一堆僵尸预警）。</li>
 * </ol>
 *
 * <p><b>幂等</b>靠数据库唯一键 {@code uk_warning_rule_project_period}
 * (rule_id, project_id, period_ym) + {@code ON DUPLICATE KEY UPDATE}：
 * 连点 10 次“刷新预警”，结果还是那几条，不会翻倍。</p>
 *
 * <p><b>已确认（ACK）的预警不会被刷新改回 OPEN</b>，避免“刚确认完又冒出来”。</p>
 *
 * @author B
 */
public class WarningDao {

    private final WarningRuleDao ruleDao = new WarningRuleDao();

    /** 一次刷新的统计结果，界面可以用来提示“本次新增 3 条、解除 1 条”。 */
    public static class RefreshResult {

        private final int active;
        private final int matched;
        private final int closed;
        private final List<String> skippedMetrics;

        public RefreshResult(int active, int matched, int closed, List<String> skippedMetrics) {
            this.active = active;
            this.matched = matched;
            this.closed = closed;
            this.skippedMetrics = skippedMetrics;
        }

        /** 刷新后仍处于 OPEN 的预警条数。 */
        public int getActive() {
            return active;
        }

        /** 本轮命中的（规则, 项目）组合数。 */
        public int getMatched() {
            return matched;
        }

        /** 本轮因为条件消失而被解除的预警条数。 */
        public int getClosed() {
            return closed;
        }

        /** 因为数据源还没到位而跳过的指标（例如 C 的里程碑表还没建）。 */
        public List<String> getSkippedMetrics() {
            return skippedMetrics;
        }

        @Override
        public String toString() {
            return "刷新完成：命中 " + matched + " 条，解除 " + closed + " 条，当前未处理 " + active
                    + (skippedMetrics.isEmpty() ? "" : "；跳过的指标：" + skippedMetrics);
        }
    }

    /**
     * 按规则表重算全部预警（任务书 5.2 的契约方法）。
     *
     * @return 刷新后未处理的预警条数
     */
    public int refreshWarnings() {
        return refresh().getActive();
    }

    /**
     * 按规则表重算全部预警，并返回本次刷新的明细（界面提示用）。
     *
     * @return 刷新明细
     * @throws DataAccessException 规则读取失败或写入失败（整个刷新在一个事务里，失败即回滚）
     */
    public RefreshResult refresh() {
        List<WarningRuleVO> rules = ruleDao.findEnabled();
        if (rules.isEmpty()) {
            return new RefreshResult(countOpen(), 0, 0, new ArrayList<>());
        }
        List<String> skipped = new ArrayList<>();
        Map<Long, Snapshot> snapshots = loadSnapshots(skipped);

        Connection conn = null;
        PreparedStatement upsert = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);
            upsert = conn.prepareStatement("""
                    INSERT INTO t_warning (rule_id, project_id, period_ym, warn_level, rule_name, trigger_snapshot, status)
                    VALUES (?, ?, ?, ?, ?, ?, 'OPEN')
                    ON DUPLICATE KEY UPDATE
                        warn_id          = LAST_INSERT_ID(warn_id),
                        warn_level       = VALUES(warn_level),
                        rule_name        = VALUES(rule_name),
                        trigger_snapshot = VALUES(trigger_snapshot),
                        updated_at       = CURRENT_TIMESTAMP
                    """, Statement.RETURN_GENERATED_KEYS);

            List<Long> activeIds = new ArrayList<>();
            int matched = 0;
            for (WarningRuleVO rule : rules) {
                BigDecimal threshold1 = rule.getCond1Value();
                BigDecimal threshold2 = rule.getCond2Value();
                for (Snapshot snap : snapshots.values()) {
                    BigDecimal v1 = snap.value(rule.getCond1Metric());
                    if (v1 == null || threshold1 == null) {
                        continue;
                    }
                    if (!compare(v1, rule.getCond1Op(), threshold1)) {
                        continue;
                    }
                    if (TextUtil.isNotBlank(rule.getCond2Metric())) {
                        BigDecimal v2 = snap.value(rule.getCond2Metric());
                        if (v2 == null || threshold2 == null || !compare(v2, rule.getCond2Op(), threshold2)) {
                            continue;
                        }
                    }
                    String snapshotText = buildSnapshot(rule, snap);
                    upsert.setLong(1, rule.getRuleId());
                    upsert.setLong(2, snap.projectId);
                    upsert.setString(3, snap.periodYm);
                    upsert.setString(4, rule.getLevelCode());
                    upsert.setString(5, rule.getRuleName());
                    upsert.setString(6, snapshotText);
                    upsert.executeUpdate();
                    matched++;
                    try (ResultSet keys = upsert.getGeneratedKeys()) {
                        if (keys.next()) {
                            activeIds.add(keys.getLong(1));
                        }
                    }
                }
            }

            int closed = closeStale(conn, activeIds);
            conn.commit();
            int active = countOpen();
            return new RefreshResult(active, matched, closed, skipped);
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                    // 回滚失败不再覆盖原始异常
                }
            }
            throw new DataAccessException("刷新预警失败（已回滚，数据保持不变）：" + e.getMessage(), e);
        } finally {
            DbUtil.closeQuietly(upsert, conn);
        }
    }

    /**
     * 把不再命中的 OPEN 预警置为 CLOSED。条件消失就解除，避免预警越刷越多。
     *
     * @return 本次解除的条数
     */
    private int closeStale(Connection conn, List<Long> activeIds) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE t_warning SET status = 'CLOSED' WHERE status = 'OPEN'");
        if (!activeIds.isEmpty()) {
            sql.append(" AND warn_id NOT IN (");
            for (int i = 0; i < activeIds.size(); i++) {
                sql.append(i == 0 ? "?" : ", ?");
            }
            sql.append(")");
        }
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < activeIds.size(); i++) {
                ps.setLong(i + 1, activeIds.get(i));
            }
            return ps.executeUpdate();
        }
    }

    /** 未处理预警条数（状态栏红点用）。 */
    public int countOpen() {
        String sql = "SELECT COUNT(*) FROM t_warning WHERE status = 'OPEN'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("统计预警条数失败：" + e.getMessage(), e);
        }
    }

    /** 按级别统计未处理预警：R/Y/B → 条数（状态栏“红灯 n / 黄灯 m”）。 */
    public Map<String, Integer> countOpenByLevel() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("R", 0);
        map.put("Y", 0);
        map.put("B", 0);
        String sql = "SELECT warn_level, COUNT(*) AS cnt FROM t_warning WHERE status = 'OPEN' GROUP BY warn_level";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("warn_level"), rs.getInt("cnt"));
            }
            return map;
        } catch (SQLException e) {
            throw new DataAccessException("按级别统计预警失败：" + e.getMessage(), e);
        }
    }

    /**
     * 预警列表（任务书 5.2 的契约方法）。
     *
     * @param level  级别 R/Y/B；传 null 或 "ALL" 表示全部
     * @param page   分页
     * @return 预警列表，按 红灯→黄灯→蓝灯、时间倒序
     */
    public List<WarningVO> findByLevel(String level, Page page) {
        boolean all = TextUtil.isBlank(level) || "ALL".equalsIgnoreCase(level);
        String sql = """
                SELECT w.warn_id, w.rule_id, r.rule_code, w.rule_name, w.warn_level,
                       w.project_id, p.project_code, p.project_name, c.country_name,
                       w.period_ym, w.trigger_snapshot, w.status,
                       w.created_at, w.ack_at, w.ack_by
                FROM t_warning w
                JOIN t_warning_rule r ON r.rule_id = w.rule_id
                JOIN t_project p      ON p.project_id = w.project_id
                LEFT JOIN t_country c ON c.country_code = p.country_code
                """
                + (all ? "" : " WHERE w.warn_level = ?")
                + " ORDER BY FIELD(w.warn_level, 'R', 'Y', 'B'), w.created_at DESC"
                + " LIMIT ? OFFSET ?";
        List<WarningVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            if (!all) {
                ps.setString(index++, level.trim().toUpperCase());
            }
            ps.setInt(index++, page.getPageSize());
            ps.setInt(index, page.getOffset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapWarning(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查询预警列表失败：" + e.getMessage(), e);
        }
    }

    /** 某个项目的预警（M2 项目详情页签用）。 */
    public List<WarningVO> findByProject(long projectId) {
        String sql = """
                SELECT w.warn_id, w.rule_id, r.rule_code, w.rule_name, w.warn_level,
                       w.project_id, p.project_code, p.project_name, c.country_name,
                       w.period_ym, w.trigger_snapshot, w.status,
                       w.created_at, w.ack_at, w.ack_by
                FROM t_warning w
                JOIN t_warning_rule r ON r.rule_id = w.rule_id
                JOIN t_project p      ON p.project_id = w.project_id
                LEFT JOIN t_country c ON c.country_code = p.country_code
                WHERE w.project_id = ?
                ORDER BY FIELD(w.warn_level, 'R', 'Y', 'B'), w.period_ym DESC
                """;
        List<WarningVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapWarning(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查询项目预警失败：" + e.getMessage(), e);
        }
    }

    /**
     * 确认（处理）一条预警。
     *
     * @param warnId 预警主键
     * @param user   处理人（界面传当前登录用户）
     * @return 受影响行数（0 = 已被别人确认或已解除）
     */
    public Result<Integer> ackWarning(long warnId, String user) {
        String sql = """
                UPDATE t_warning
                   SET status = 'ACK', ack_at = CURRENT_TIMESTAMP, ack_by = ?
                 WHERE warn_id = ? AND status = 'OPEN'
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, TextUtil.defaultIfBlank(user, "系统"), warnId);
            int rows = ps.executeUpdate();
            return Result.ok(rows, rows == 0 ? "该预警已被处理或已解除，请刷新列表" : "预警已确认");
        } catch (SQLException e) {
            return Result.fail("确认预警失败：" + e.getMessage());
        }
    }

    /** 确认一条预警（处理人默认“系统”，兼容任务书 {@code ackWarning(long)} 签名）。 */
    public Result<Integer> ackWarning(long warnId) {
        return ackWarning(warnId, "系统");
    }

    // ------------------------------------------------------------------
    // 指标快照
    // ------------------------------------------------------------------

    /** 一个项目的指标快照：预警规则拿它做比较。 */
    private static final class Snapshot {

        private final long projectId;
        private String periodYm;
        private final Map<String, BigDecimal> metrics = new HashMap<>();

        private Snapshot(long projectId) {
            this.projectId = projectId;
        }

        private BigDecimal value(String metric) {
            return metric == null ? null : metrics.get(metric);
        }
    }

    /**
     * 批量取指标快照：每个指标一条聚合 SQL，绝不按项目循环查库。
     *
     * @param skipped 收集“数据源缺失”的指标名
     */
    private Map<Long, Snapshot> loadSnapshots(List<String> skipped) {
        Map<Long, Snapshot> snapshots = new LinkedHashMap<>();

        // 指标 1/2：资金到位率 + 形象进度（在建项目，来自 v_fund_progress）
        String sqlFund = """
                SELECT project_id, fund_rate, cur_progress, cur_period
                FROM v_fund_progress
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlFund);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long projectId = rs.getLong("project_id");
                Snapshot snap = new Snapshot(projectId);
                snap.periodYm = rs.getString("cur_period");
                snap.metrics.put("FUND_RATE", rs.getBigDecimal("fund_rate"));
                snap.metrics.put("PROGRESS_PCT", rs.getBigDecimal("cur_progress"));
                snapshots.put(projectId, snap);
            }
        } catch (SQLException e) {
            throw new DataAccessException("读取资金-进度视图失败：" + e.getMessage(), e);
        }

        // 指标 3：属地用工占比（每个项目最新一期月报）
        String sqlHire = """
                SELECT r.project_id,
                       ROUND(100.0 * r.local_staff / NULLIF(r.on_site_staff, 0), 2) AS local_hire_pct
                FROM t_progress_report r
                JOIN (SELECT project_id, MAX(period_ym) AS ym
                      FROM t_progress_report GROUP BY project_id) l
                  ON l.project_id = r.project_id AND l.ym = r.period_ym
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlHire);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Snapshot snap = snapshots.get(rs.getLong("project_id"));
                if (snap != null) {
                    snap.metrics.put("LOCAL_HIRE_RATE", rs.getBigDecimal("local_hire_pct"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("读取属地用工占比失败：" + e.getMessage(), e);
        }

        // 指标 4：未报月数（当前月份 - 最新报告期）
        String sqlGap = """
                SELECT project_id,
                       TIMESTAMPDIFF(MONTH,
                           STR_TO_DATE(CONCAT(MAX(period_ym), '-01'), '%Y-%m-%d'),
                           CURDATE()) AS gap_months
                FROM t_progress_report
                GROUP BY project_id
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlGap);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Snapshot snap = snapshots.get(rs.getLong("project_id"));
                if (snap != null) {
                    snap.metrics.put("REPORT_GAP_MONTH", BigDecimal.valueOf(Math.max(0, rs.getInt("gap_months"))));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("读取未报月数失败：" + e.getMessage(), e);
        }

        // 指标 5：超期未完成里程碑（数据在 C 的 t_project_milestone 里；表还没建就跳过，不报错）
        String sqlOverdue = """
                SELECT pm.project_id, COUNT(*) AS overdue_cnt
                FROM t_project_milestone pm
                WHERE pm.actual_date IS NULL AND pm.plan_date < CURDATE()
                GROUP BY pm.project_id
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlOverdue);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Snapshot snap = snapshots.get(rs.getLong("project_id"));
                if (snap != null) {
                    snap.metrics.put("OVERDUE_COUNT", BigDecimal.valueOf(rs.getInt("overdue_cnt")));
                }
            }
        } catch (SQLException e) {
            // 1146 = 表不存在；C 的里程碑表属于第 4–5 周交付，未到位时 F3 规则自然不触发
            if (e.getErrorCode() == 1146) {
                skipped.add("OVERDUE_COUNT（t_project_milestone 尚未建表，F3 规则本轮跳过）");
            } else {
                throw new DataAccessException("读取超期里程碑失败：" + e.getMessage(), e);
            }
        }
        return snapshots;
    }

    private String buildSnapshot(WarningRuleVO rule, Snapshot snap) {
        String text = rule.getMessageTpl();
        BigDecimal v1 = snap.value(rule.getCond1Metric());
        BigDecimal v2 = snap.value(rule.getCond2Metric());
        if (TextUtil.isBlank(text)) {
            return "触发规则 " + rule.getRuleName();
        }
        text = text.replace("{v1}", plain(v1)).replace("{v2}", plain(v2));
        return text;
    }

    private static String plain(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    /** 比较：把比较符翻成 Java 运算，比较符与阈值都来自规则表。 */
    static boolean compare(BigDecimal value, String op, BigDecimal threshold) {
        if (value == null || threshold == null || op == null) {
            return false;
        }
        int cmp = value.compareTo(threshold);
        switch (op) {
            case "LT":
                return cmp < 0;
            case "LE":
                return cmp <= 0;
            case "GT":
                return cmp > 0;
            case "GE":
                return cmp >= 0;
            case "EQ":
                return cmp == 0;
            default:
                return false;
        }
    }

    private static WarningVO mapWarning(ResultSet rs) throws SQLException {
        WarningVO vo = new WarningVO();
        vo.setWarnId(rs.getLong("warn_id"));
        vo.setRuleId(rs.getLong("rule_id"));
        vo.setRuleCode(rs.getString("rule_code"));
        vo.setRuleName(rs.getString("rule_name"));
        vo.setLevelCode(rs.getString("warn_level"));
        vo.setProjectId(rs.getLong("project_id"));
        vo.setProjectCode(rs.getString("project_code"));
        vo.setProjectName(rs.getString("project_name"));
        vo.setCountryName(rs.getString("country_name"));
        vo.setPeriodYm(rs.getString("period_ym"));
        vo.setTriggerSnapshot(rs.getString("trigger_snapshot"));
        vo.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("created_at");
        vo.setCreatedAt(created == null ? null : created.toLocalDateTime());
        Timestamp ack = rs.getTimestamp("ack_at");
        vo.setAckAt(ack == null ? null : ack.toLocalDateTime());
        vo.setAckBy(rs.getString("ack_by"));
        return vo;
    }
}
