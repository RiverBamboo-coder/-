package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.vo.CompareVO;
import com.bri.vo.CommitRateRow;
import com.bri.vo.CommitVO;
import com.bri.vo.ContraRow;
import com.bri.vo.ReconRow;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 履约承诺比对 DAO（C 负责，对应 M7，本任务书最高优先级的代码）。
 *
 * <p>两张表：{@code t_commitment}（承诺，目标值+比较方向+基准日做成数据）+
 * {@code t_commitment_result}（比对结果，四档结论）。核心是
 * {@link #calcAll()}：把 {@code v_commitment_actual} 视图算出的实际值与目标值比对，
 * 用 CASE 收敛为 已履约/部分履约/未履约/待评估 四档，幂等写入结果表。</p>
 *
 * <p>四个难点（答辩核心，见 06_views_commitment.sql）：多源对齐、三值逻辑、除零保护、基准日。</p>
 *
 * @author C
 */
public class CommitmentDao {

    /**
     * 查某项目的承诺清单（含已计算出的比对结果）。
     *
     * @param projectId 项目主键
     * @return 承诺列表，永不为 null
     */
    public List<CommitVO> findCommitments(long projectId) {
        String sql = "SELECT c.commit_id, c.project_id, c.commit_type, c.target_value, c.compare_op, "
                + "       c.base_date, c.clause_text, c.sign_date, c.remark, "
                + "       r.actual_value, r.gap_value, r.commit_status "
                + "FROM t_commitment c "
                + "LEFT JOIN t_commitment_result r ON r.commit_id = c.commit_id "
                + "WHERE c.project_id = ? "
                + "ORDER BY c.commit_id";
        List<CommitVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CommitVO vo = new CommitVO();
                    vo.setCommitId(rs.getLong("commit_id"));
                    vo.setProjectId(rs.getLong("project_id"));
                    vo.setCommitType(rs.getString("commit_type"));
                    vo.setTargetValue(rs.getBigDecimal("target_value"));
                    vo.setCompareOp(rs.getString("compare_op"));
                    vo.setBaseDate(toLocalDate(rs, "base_date"));
                    vo.setClauseText(rs.getString("clause_text"));
                    vo.setSignDate(toLocalDate(rs, "sign_date"));
                    vo.setRemark(rs.getString("remark"));
                    vo.setActualValue(rs.getBigDecimal("actual_value"));
                    vo.setGapValue(rs.getBigDecimal("gap_value"));
                    vo.setCommitStatus(rs.getString("commit_status"));
                    list.add(vo);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询项目承诺失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 全量比对计算：读 {@code v_commitment_actual}，把四类承诺的实际值与目标值比对，
     * 结论写入 {@code t_commitment_result}（幂等：同 commit_id 覆盖更新）。
     *
     * <p>结论判定顺序（NULL 必须先判，顺序不能反）：</p>
     * <ol>
     *   <li>实际值为 NULL → 待评估（NULL 不是 0，不能当作没达标）；</li>
     *   <li>满足比较方向 → 已履约；</li>
     *   <li>在 20% 容差内 → 部分履约；</li>
     *   <li>其余 → 未履约。</li>
     * </ol>
     *
     * @return 比对出的承诺结果条数
     * @throws DataAccessException 计算失败（通常因为视图未创建，message 会提示）
     */
    public int calcAll() {
        String sql = "INSERT INTO t_commitment_result (commit_id, actual_value, gap_value, commit_status) "
                + "SELECT c.commit_id, "
                + "       a.actual_value, "
                + "       a.actual_value - c.target_value, "
                + "       CASE "
                + "         WHEN a.actual_value IS NULL THEN '待评估' "
                + "         WHEN (c.compare_op = '<=' AND a.actual_value <= c.target_value) "
                + "           OR (c.compare_op = '>=' AND a.actual_value >= c.target_value) THEN '已履约' "
                + "         WHEN (c.compare_op = '<=' AND a.actual_value <= c.target_value * 1.2) "
                + "           OR (c.compare_op = '>=' AND a.actual_value >= c.target_value * 0.8) THEN '部分履约' "
                + "         ELSE '未履约' "
                + "       END "
                + "FROM t_commitment c "
                + "JOIN v_commitment_actual a ON a.commit_id = c.commit_id "
                + "ON DUPLICATE KEY UPDATE "
                + "  actual_value  = VALUES(actual_value), "
                + "  gap_value     = VALUES(gap_value), "
                + "  commit_status = VALUES(commit_status), "
                + "  calc_time     = CURRENT_TIMESTAMP";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("履约承诺比对计算失败（请先执行 sql/06_views_commitment.sql 创建视图）："
                    + e.getMessage(), e);
        }
        return (int) DbUtil.queryCount("SELECT COUNT(*) FROM t_commitment_result");
    }

    /**
     * 按结论筛选比对结果（未履约/部分履约/已履约/待评估）。
     *
     * @param status 结论：已履约/部分履约/未履约/待评估
     * @return 比对结果列表
     */
    public List<CompareVO> findByStatus(String status) {
        String sql = "SELECT p.project_code, p.project_name, c.commit_type, c.target_value, c.compare_op, "
                + "       r.actual_value, r.gap_value, r.commit_status, c.clause_text "
                + "FROM t_commitment_result r "
                + "JOIN t_commitment c ON c.commit_id = r.commit_id "
                + "JOIN t_project p ON p.project_id = c.project_id "
                + "WHERE r.commit_status = ? "
                + "ORDER BY p.project_code, c.commit_type";
        List<CompareVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CompareVO vo = new CompareVO();
                    vo.setProjectCode(rs.getString("project_code"));
                    vo.setProjectName(rs.getString("project_name"));
                    vo.setCommitType(rs.getString("commit_type"));
                    vo.setTargetValue(rs.getBigDecimal("target_value"));
                    vo.setCompareOp(rs.getString("compare_op"));
                    vo.setActualValue(rs.getBigDecimal("actual_value"));
                    vo.setGapValue(rs.getBigDecimal("gap_value"));
                    vo.setCommitStatus(rs.getString("commit_status"));
                    vo.setClauseText(rs.getString("clause_text"));
                    list.add(vo);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("按结论筛选承诺失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q7-2：按承诺类型统计履约率（已履约数 / 承诺总数）。
     *
     * @return 各承诺类型履约率行
     */
    public List<CommitRateRow> statRateByType() {
        String sql = "SELECT c.commit_type AS commit_type, "
                + "       COUNT(*) AS total_cnt, "
                + "       CAST(COALESCE(SUM(CASE WHEN r.commit_status = '已履约' THEN 1 ELSE 0 END), 0) AS SIGNED) AS fulfilled_cnt, "
                + "       ROUND(100.0 * COALESCE(SUM(CASE WHEN r.commit_status = '已履约' THEN 1 ELSE 0 END), 0) "
                + "             / NULLIF(COUNT(*), 0), 2) AS rate_pct "
                + "FROM t_commitment c "
                + "LEFT JOIN t_commitment_result r ON r.commit_id = c.commit_id "
                + "GROUP BY c.commit_type "
                + "ORDER BY rate_pct DESC";
        List<CommitRateRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CommitRateRow row = new CommitRateRow();
                row.setCommitType(rs.getString("commit_type"));
                row.setTotalCnt(rs.getLong("total_cnt"));
                row.setFulfilledCnt(rs.getLong("fulfilled_cnt"));
                row.setRatePct(rs.getBigDecimal("rate_pct"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计承诺履约率失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q7-3：交叉矛盾项目——同一项目里既存在“已履约”又存在“未履约”的承诺。
     *
     * @return 矛盾项目列表
     */
    public List<ContraRow> findCrossContradiction() {
        String sql = "SELECT p.project_id, p.project_code, p.project_name, "
                + "       COUNT(*) AS total_cnt, "
                + "       CAST(COALESCE(SUM(CASE WHEN r.commit_status = '已履约' THEN 1 ELSE 0 END), 0) AS SIGNED) AS fulfilled_cnt, "
                + "       CAST(COALESCE(SUM(CASE WHEN r.commit_status = '未履约' THEN 1 ELSE 0 END), 0) AS SIGNED) AS breached_cnt, "
                + "       CAST(COALESCE(SUM(CASE WHEN r.commit_status = '部分履约' THEN 1 ELSE 0 END), 0) AS SIGNED) AS partial_cnt "
                + "FROM t_commitment c "
                + "JOIN t_commitment_result r ON r.commit_id = c.commit_id "
                + "JOIN t_project p ON p.project_id = c.project_id "
                + "GROUP BY p.project_id, p.project_code, p.project_name "
                + "HAVING breached_cnt > 0 AND fulfilled_cnt > 0 "
                + "ORDER BY breached_cnt DESC, fulfilled_cnt DESC";
        List<ContraRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ContraRow row = new ContraRow();
                row.setProjectId(rs.getLong("project_id"));
                row.setProjectCode(rs.getString("project_code"));
                row.setProjectName(rs.getString("project_name"));
                row.setTotalCnt(rs.getLong("total_cnt"));
                row.setFulfilledCnt(rs.getLong("fulfilled_cnt"));
                row.setBreachedCnt(rs.getLong("breached_cnt"));
                row.setPartialCnt(rs.getLong("partial_cnt"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询交叉矛盾项目失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q7-4：国别履约对账表——按东道国汇总承诺数量与履约情况。
     *
     * @return 国别对账行列表，按履约率降序
     */
    public List<ReconRow> statCountryReconcile() {
        String sql = "SELECT c.country_code, c.country_name, "
                + "       COUNT(*) AS commit_cnt, "
                + "       CAST(COALESCE(SUM(CASE WHEN r.commit_status = '已履约' THEN 1 ELSE 0 END), 0) AS SIGNED) AS fulfilled_cnt, "
                + "       CAST(COALESCE(SUM(CASE WHEN r.commit_status = '未履约' THEN 1 ELSE 0 END), 0) AS SIGNED) AS breached_cnt, "
                + "       CAST(COALESCE(SUM(CASE WHEN r.commit_status = '待评估' THEN 1 ELSE 0 END), 0) AS SIGNED) AS pending_cnt, "
                + "       ROUND(100.0 * COALESCE(SUM(CASE WHEN r.commit_status = '已履约' THEN 1 ELSE 0 END), 0) "
                + "             / NULLIF(COUNT(*), 0), 2) AS rate_pct "
                + "FROM t_commitment cm "
                + "JOIN t_commitment_result r ON r.commit_id = cm.commit_id "
                + "JOIN t_project p ON p.project_id = cm.project_id "
                + "JOIN t_country c ON c.country_code = p.country_code "
                + "GROUP BY c.country_code, c.country_name "
                + "ORDER BY rate_pct DESC";
        List<ReconRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ReconRow row = new ReconRow();
                row.setCountryCode(rs.getString("country_code"));
                row.setCountryName(rs.getString("country_name"));
                row.setCommitCnt(rs.getLong("commit_cnt"));
                row.setFulfilledCnt(rs.getLong("fulfilled_cnt"));
                row.setBreachedCnt(rs.getLong("breached_cnt"));
                row.setPendingCnt(rs.getLong("pending_cnt"));
                row.setRatePct(rs.getBigDecimal("rate_pct"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计国别履约对账失败：" + e.getMessage(), e);
        }
        return list;
    }

    /** 把结果集里的 DATE 列读成 LocalDate，null 安全。 */
    private static LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }
}
