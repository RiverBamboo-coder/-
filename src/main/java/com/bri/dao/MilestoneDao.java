package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.common.Result;
import com.bri.vo.AchievementRateRow;
import com.bri.vo.DelayRow;
import com.bri.vo.DurationRow;
import com.bri.vo.MilestoneVO;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 里程碑 DAO（C 负责，对应 M5）。
 *
 * <p>两张表：{@code t_milestone}（里程碑字典，6 类）+ {@code t_project_milestone}（项目里程碑）。
 * 亮点：超期判定由数据库生成列 {@code is_delayed} 完成，Java 只读不算。</p>
 *
 * <p>约定：查询方法在失败时抛 {@link DataAccessException}（message 为中文提示），
 * 增删改方法返回 {@link Result}，不把异常堆栈丢给界面。</p>
 *
 * @author C
 */
public class MilestoneDao {

    /**
     * 查某项目的里程碑清单（M5 列表），按字典建议顺序排列。
     *
     * @param projectId 项目主键
     * @return 里程碑列表，永不为 null
     */
    public List<MilestoneVO> findMilestonesByProject(long projectId) {
        String sql = "SELECT pm.pm_id, pm.project_id, pm.milestone_id, "
                + "       m.milestone_code, m.milestone_name, m.seq_no, "
                + "       pm.plan_date, pm.actual_date, pm.is_delayed, pm.remark "
                + "FROM t_project_milestone pm "
                + "JOIN t_milestone m ON m.milestone_id = pm.milestone_id "
                + "WHERE pm.project_id = ? "
                + "ORDER BY m.seq_no ASC";
        List<MilestoneVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MilestoneVO vo = new MilestoneVO();
                    vo.setPmId(rs.getLong("pm_id"));
                    vo.setProjectId(rs.getLong("project_id"));
                    vo.setMilestoneId(rs.getInt("milestone_id"));
                    vo.setMilestoneCode(rs.getString("milestone_code"));
                    vo.setMilestoneName(rs.getString("milestone_name"));
                    vo.setSeqNo(rs.getInt("seq_no"));
                    vo.setPlanDate(toLocalDate(rs, "plan_date"));
                    vo.setActualDate(toLocalDate(rs, "actual_date"));
                    Object delayed = rs.getObject("is_delayed");
                    vo.setIsDelayed(delayed == null ? null : ((Number) delayed).intValue() != 0);
                    vo.setRemark(rs.getString("remark"));
                    list.add(vo);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询项目里程碑失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 保存/更新里程碑计划日期（计划日期）。
     *
     * <p>依赖 {@code t_project_milestone} 的唯一键 {@code uk_pm_project_milestone (project_id, milestone_id)}：
     * 有则更新计划日期，无则插入。</p>
     *
     * @param pm 里程碑计划（projectId / milestoneId / planDate / remark）
     * @return 成功时携带该条记录的主键 pm_id
     */
    public Result<Long> upsertMilestonePlan(MilestoneVO pm) {
        if (pm == null || pm.getProjectId() <= 0 || pm.getMilestoneId() <= 0) {
            return Result.fail("项目或里程碑类型不合法");
        }
        if (pm.getPlanDate() == null) {
            return Result.fail("计划日期不能为空");
        }
        String sql = "INSERT INTO t_project_milestone (project_id, milestone_id, plan_date, remark) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE plan_date = VALUES(plan_date), remark = VALUES(remark)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, pm.getProjectId());
            ps.setInt(2, pm.getMilestoneId());
            ps.setDate(3, Date.valueOf(pm.getPlanDate()));
            ps.setString(4, pm.getRemark());
            ps.executeUpdate();
        } catch (SQLException e) {
            return Result.fail("保存里程碑计划失败：" + e.getMessage());
        }
        // 回查主键，便于界面刷新定位
        String select = "SELECT pm_id FROM t_project_milestone WHERE project_id = ? AND milestone_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setLong(1, pm.getProjectId());
            ps.setInt(2, pm.getMilestoneId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Result.ok(rs.getLong("pm_id"), "里程碑计划已保存");
                }
            }
        } catch (SQLException e) {
            return Result.fail("保存成功但回查主键失败：" + e.getMessage());
        }
        return Result.ok(null, "里程碑计划已保存");
    }

    /**
     * 补录某条里程碑的实际完成日期。
     *
     * @param pmId       项目里程碑主键
     * @param actualDate 实际完成日期
     * @return 操作结果；记录不存在时返回失败
     */
    public Result<Void> recordActualDate(long pmId, LocalDate actualDate) {
        if (pmId <= 0) {
            return Result.fail("里程碑主键不合法");
        }
        if (actualDate == null) {
            return Result.fail("实际完成日期不能为空");
        }
        String sql = "UPDATE t_project_milestone SET actual_date = ? WHERE pm_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(actualDate));
            ps.setLong(2, pmId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                return Result.fail("未找到该里程碑记录");
            }
            return Result.ok(null, "实际完成日期已补录");
        } catch (SQLException e) {
            return Result.fail("补录实际日期失败：" + e.getMessage());
        }
    }

    /**
     * 按“项目 + 里程碑类型”补录实际完成日期（免去先查 pm_id 的步骤）。
     *
     * @param projectId   项目主键
     * @param milestoneId 里程碑字典主键
     * @param actualDate  实际完成日期
     * @return 操作结果
     */
    public Result<Void> recordActualByType(long projectId, int milestoneId, LocalDate actualDate) {
        if (projectId <= 0 || milestoneId <= 0) {
            return Result.fail("项目或里程碑类型不合法");
        }
        if (actualDate == null) {
            return Result.fail("实际完成日期不能为空");
        }
        String sql = "UPDATE t_project_milestone SET actual_date = ? "
                + "WHERE project_id = ? AND milestone_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(actualDate));
            ps.setLong(2, projectId);
            ps.setInt(3, milestoneId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                return Result.fail("未找到该项目的里程碑计划，请先保存计划日期");
            }
            return Result.ok(null, "实际完成日期已补录");
        } catch (SQLException e) {
            return Result.fail("补录实际日期失败：" + e.getMessage());
        }
    }

    /**
     * Q5-1：按里程碑类型统计平均超期天数（只统计已完成的里程碑）。
     *
     * @return 平均超期天数行
     */
    public List<DelayRow> statAvgDelayByMilestone() {
        String sql = "SELECT m.milestone_id, m.milestone_name, "
                + "       ROUND(AVG(DATEDIFF(pm.actual_date, pm.plan_date)), 1) AS avg_delay_days, "
                + "       COUNT(*) AS total_cnt, "
                + "       CAST(COALESCE(SUM(CASE WHEN pm.is_delayed = 1 THEN 1 ELSE 0 END), 0) AS SIGNED) AS overdue_cnt "
                + "FROM t_project_milestone pm "
                + "JOIN t_milestone m ON m.milestone_id = pm.milestone_id "
                + "WHERE pm.actual_date IS NOT NULL "
                + "GROUP BY m.milestone_id, m.milestone_name "
                + "ORDER BY avg_delay_days DESC";
        List<DelayRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DelayRow row = new DelayRow();
                row.setMilestoneId(rs.getInt("milestone_id"));
                row.setMilestoneName(rs.getString("milestone_name"));
                row.setAvgDelayDays(rs.getBigDecimal("avg_delay_days"));
                row.setTotalCnt(rs.getInt("total_cnt"));
                row.setOverdueCnt(rs.getInt("overdue_cnt"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计平均超期天数失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q5-2：查询已超期但未完成的里程碑（计划日期已过、实际日期为空）。
     *
     * @return 已超期未完成行，按超期天数降序
     */
    public List<DelayRow> findOverdueMilestones() {
        String sql = "SELECT pm.pm_id, p.project_code, p.project_name, m.milestone_name, pm.plan_date, "
                + "       DATEDIFF(CURDATE(), pm.plan_date) AS overdue_days "
                + "FROM t_project_milestone pm "
                + "JOIN t_project p ON p.project_id = pm.project_id "
                + "JOIN t_milestone m ON m.milestone_id = pm.milestone_id "
                + "WHERE pm.actual_date IS NULL AND pm.plan_date < CURDATE() "
                + "ORDER BY overdue_days DESC";
        List<DelayRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DelayRow row = new DelayRow();
                row.setPmId(rs.getLong("pm_id"));
                row.setProjectCode(rs.getString("project_code"));
                row.setProjectName(rs.getString("project_name"));
                row.setMilestoneName(rs.getString("milestone_name"));
                row.setPlanDate(toLocalDate(rs, "plan_date"));
                row.setOverdueDays(rs.getLong("overdue_days"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询已超期里程碑失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q5-3：对比项目计划工期与实际工期（只统计已有实际开工/竣工日的项目）。
     *
     * @return 工期对比行，按工期差降序
     */
    public List<DurationRow> compareProjectDuration() {
        String sql = "SELECT p.project_id, p.project_code, p.project_name, "
                + "       p.planned_start_date, p.planned_end_date, "
                + "       p.actual_start_date, p.actual_end_date, "
                + "       DATEDIFF(p.planned_end_date, p.planned_start_date) AS planned_days, "
                + "       DATEDIFF(p.actual_end_date, p.actual_start_date) AS actual_days, "
                + "       DATEDIFF(p.actual_end_date, p.actual_start_date) "
                + "         - DATEDIFF(p.planned_end_date, p.planned_start_date) AS diff_days "
                + "FROM t_project p "
                + "WHERE p.planned_start_date IS NOT NULL AND p.planned_end_date IS NOT NULL "
                + "  AND p.actual_start_date IS NOT NULL AND p.actual_end_date IS NOT NULL "
                + "ORDER BY diff_days DESC";
        List<DurationRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DurationRow row = new DurationRow();
                row.setProjectId(rs.getLong("project_id"));
                row.setProjectCode(rs.getString("project_code"));
                row.setProjectName(rs.getString("project_name"));
                row.setPlannedStartDate(toLocalDate(rs, "planned_start_date"));
                row.setPlannedEndDate(toLocalDate(rs, "planned_end_date"));
                row.setActualStartDate(toLocalDate(rs, "actual_start_date"));
                row.setActualEndDate(toLocalDate(rs, "actual_end_date"));
                row.setPlannedDays(rs.getLong("planned_days"));
                row.setActualDays(rs.getLong("actual_days"));
                row.setDiffDays(rs.getLong("diff_days"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("对比项目工期失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q5-4：按领域统计里程碑达成率（已完成里程碑数 / 计划里程碑总数）。
     *
     * @return 各领域达成率行，按达成率降序
     */
    public List<AchievementRateRow> statMilestoneRateByField() {
        String sql = "SELECT p.field_code AS field_code, "
                + "       COALESCE(d.item_name, p.field_code) AS field_name, "
                + "       COUNT(pm.pm_id) AS total_cnt, "
                + "       CAST(COALESCE(SUM(CASE WHEN pm.actual_date IS NOT NULL THEN 1 ELSE 0 END), 0) AS SIGNED) AS achieved_cnt, "
                + "       ROUND(100.0 * SUM(CASE WHEN pm.actual_date IS NOT NULL THEN 1 ELSE 0 END) "
                + "             / NULLIF(COUNT(pm.pm_id), 0), 2) AS rate_pct "
                + "FROM t_project p "
                + "JOIN t_project_milestone pm ON pm.project_id = p.project_id "
                + "LEFT JOIN t_dict_type dt ON dt.type_code = 'FIELD' "
                + "LEFT JOIN t_dict_item d ON d.dict_type_id = dt.dict_type_id AND d.item_code = p.field_code "
                + "GROUP BY p.field_code, d.item_name "
                + "ORDER BY rate_pct DESC";
        List<AchievementRateRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AchievementRateRow row = new AchievementRateRow();
                row.setFieldCode(rs.getString("field_code"));
                row.setFieldName(rs.getString("field_name"));
                row.setTotalCnt(rs.getLong("total_cnt"));
                row.setAchievedCnt(rs.getLong("achieved_cnt"));
                row.setRatePct(rs.getBigDecimal("rate_pct"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计各领域里程碑达成率失败：" + e.getMessage(), e);
        }
        return list;
    }

    /** 把结果集里的 DATE 列读成 LocalDate，null 安全。 */
    private static LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }
}
