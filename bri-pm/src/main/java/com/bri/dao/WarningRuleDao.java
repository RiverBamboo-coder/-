package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.common.Result;
import com.bri.common.TextUtil;
import com.bri.vo.WarningRuleVO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 预警规则 DAO（M8 · t_warning_rule）。B 负责。
 *
 * <p>“改阈值 → 刷新 → 预警变化”这条演示链路的第一环就在这里：
 * 界面把新阈值写进 t_warning_rule，{@link WarningDao#refreshWarnings()} 重新读表算结果。
 * 规则存表而不写死在 Java 里，是本模块最容易被问到的设计点。</p>
 *
 * @author B
 */
public class WarningRuleDao {

    private static final String SQL_SELECT = """
            SELECT rule_id, rule_code, rule_name, level_code,
                   cond1_metric, cond1_op, cond1_value,
                   cond2_metric, cond2_op, cond2_value,
                   is_enabled, message_tpl, updated_at
            FROM t_warning_rule
            """;

    /** 全部规则（界面“规则维护”页）。 */
    public List<WarningRuleVO> findAll() {
        String sql = SQL_SELECT + " ORDER BY rule_id";
        return query(sql);
    }

    /** 只取启用的规则（刷新用；停用的规则不产生预警）。 */
    public List<WarningRuleVO> findEnabled() {
        String sql = SQL_SELECT + " WHERE is_enabled = 1 ORDER BY rule_id";
        return query(sql);
    }

    /** 按主键取一条。 */
    public WarningRuleVO findById(long ruleId) {
        String sql = SQL_SELECT + " WHERE rule_id = ?";
        List<WarningRuleVO> list = query(sql, ruleId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 修改规则（阈值、级别、启用状态、说明模板都可以改）。
     *
     * @param vo 规则，ruleId 必填
     * @return 受影响行数
     */
    public Result<Integer> update(WarningRuleVO vo) {
        if (vo == null || vo.getRuleId() <= 0) {
            return Result.fail("修改规则失败：主键缺失");
        }
        if (vo.getCond1Value() == null) {
            return Result.fail("修改规则失败：条件 1 的阈值不能为空");
        }
        if (TextUtil.isBlank(vo.getCond1Metric()) || TextUtil.isBlank(vo.getCond1Op())) {
            return Result.fail("修改规则失败：条件 1 的指标与比较符不能为空");
        }
        boolean hasSecond = TextUtil.isNotBlank(vo.getCond2Metric());
        String sql = """
                UPDATE t_warning_rule
                   SET rule_name    = ?,
                       level_code   = ?,
                       cond1_metric = ?, cond1_op = ?, cond1_value = ?,
                       cond2_metric = ?, cond2_op = ?, cond2_value = ?,
                       is_enabled   = ?,
                       message_tpl  = ?
                 WHERE rule_id = ?
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps,
                    TextUtil.trimToNull(vo.getRuleName()),
                    vo.getLevelCode(),
                    vo.getCond1Metric(), vo.getCond1Op(), vo.getCond1Value(),
                    hasSecond ? vo.getCond2Metric() : null,
                    hasSecond ? vo.getCond2Op() : null,
                    hasSecond ? vo.getCond2Value() : null,
                    vo.isEnabled() ? 1 : 0,
                    TextUtil.trimToNull(vo.getMessageTpl()),
                    vo.getRuleId());
            int rows = ps.executeUpdate();
            return Result.ok(rows, rows == 0 ? "没有找到要修改的规则" : "规则已修改，点“刷新预警”生效");
        } catch (SQLException e) {
            switch (e.getErrorCode()) {
                case 3819:
                    return Result.fail("修改规则失败：级别只能是 R/Y/B，指标与比较符必须在允许清单内");
                default:
                    return Result.fail("修改规则失败：" + e.getMessage());
            }
        }
    }

    /** 启用/停用一条规则。 */
    public Result<Integer> setEnabled(long ruleId, boolean enabled) {
        String sql = "UPDATE t_warning_rule SET is_enabled = ? WHERE rule_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, enabled ? 1 : 0, ruleId);
            int rows = ps.executeUpdate();
            return Result.ok(rows, rows == 0 ? "没有找到该规则"
                    : (enabled ? "规则已启用" : "规则已停用"));
        } catch (SQLException e) {
            return Result.fail("修改规则状态失败：" + e.getMessage());
        }
    }

    private List<WarningRuleVO> query(String sql, Object... params) {
        List<WarningRuleVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查询预警规则失败：" + e.getMessage(), e);
        }
    }

    private static WarningRuleVO map(ResultSet rs) throws SQLException {
        WarningRuleVO vo = new WarningRuleVO();
        vo.setRuleId(rs.getLong("rule_id"));
        vo.setRuleCode(rs.getString("rule_code"));
        vo.setRuleName(rs.getString("rule_name"));
        vo.setLevelCode(rs.getString("level_code"));
        vo.setCond1Metric(rs.getString("cond1_metric"));
        vo.setCond1Op(rs.getString("cond1_op"));
        vo.setCond1Value(rs.getBigDecimal("cond1_value"));
        vo.setCond2Metric(rs.getString("cond2_metric"));
        vo.setCond2Op(rs.getString("cond2_op"));
        vo.setCond2Value(rs.getBigDecimal("cond2_value"));
        vo.setEnabled(rs.getInt("is_enabled") == 1);
        vo.setMessageTpl(rs.getString("message_tpl"));
        Timestamp updated = rs.getTimestamp("updated_at");
        vo.setUpdatedAt(updated == null ? null : updated.toLocalDateTime());
        return vo;
    }
}
