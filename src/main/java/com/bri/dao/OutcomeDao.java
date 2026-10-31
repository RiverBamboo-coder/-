package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.vo.GreenRow;
import com.bri.vo.HireRow;
import com.bri.vo.OutcomeVO;
import com.bri.vo.RegionOutRow;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目成效 DAO（C 负责，对应 M9）。
 *
 * <p>一张表 {@code t_project_outcome}（与项目 1:1），承担三条思政查询：
 * 思政 1 共商共建共享（大洲汇总）、思政 2 授人以渔（属地用工+培训双达标）、
 * 思政 3 绿色丝绸之路（绿色装机）。查询结果供 D 的看板以表格/图表展示。</p>
 *
 * @author C
 */
public class OutcomeDao {

    /**
     * 查某项目的成效详情。
     *
     * @param projectId 项目主键
     * @return 成效对象，无数据时返回 null
     */
    public OutcomeVO findByProject(long projectId) {
        String sql = "SELECT project_id, beneficiary_k, jobs_created, training_ppl, "
                + "       green_mw, standards_out, update_date, remark "
                + "FROM t_project_outcome WHERE project_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                OutcomeVO vo = new OutcomeVO();
                vo.setProjectId(rs.getLong("project_id"));
                vo.setBeneficiaryK(rs.getBigDecimal("beneficiary_k"));
                vo.setJobsCreated(readInteger(rs, "jobs_created"));
                vo.setTrainingPpl(readInteger(rs, "training_ppl"));
                vo.setGreenMw(rs.getBigDecimal("green_mw"));
                vo.setStandardsOut(readInteger(rs, "standards_out"));
                vo.setUpdateDate(toLocalDate(rs, "update_date"));
                vo.setRemark(rs.getString("remark"));
                return vo;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询项目成效失败：" + e.getMessage(), e);
        }
    }

    /**
     * 思政 1“共商共建共享”：按大洲统计共建国家的项目数、受益人口与就业岗位。
     *
     * @return 大洲成效统计行，按项目数降序
     */
    public List<RegionOutRow> statByRegion() {
        String sql = "SELECT COALESCE(cont.region_name, reg.region_name) AS region_name, "
                + "       COUNT(DISTINCT p.project_id) AS proj_cnt, "
                + "       COALESCE(SUM(o.beneficiary_k), 0) AS benefit_k, "
                + "       CAST(COALESCE(SUM(o.jobs_created), 0) AS SIGNED) AS jobs "
                + "FROM t_project p "
                + "JOIN t_country c ON c.country_code = p.country_code "
                + "JOIN t_region reg ON reg.region_id = c.region_id "
                + "LEFT JOIN t_region cont ON cont.region_id = reg.parent_id "
                + "LEFT JOIN t_project_outcome o ON o.project_id = p.project_id "
                + "WHERE c.is_belt_road = 1 "
                + "GROUP BY COALESCE(cont.region_name, reg.region_name) "
                + "ORDER BY proj_cnt DESC";
        List<RegionOutRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RegionOutRow row = new RegionOutRow();
                row.setRegionName(rs.getString("region_name"));
                row.setProjCnt(rs.getLong("proj_cnt"));
                row.setBenefitK(rs.getBigDecimal("benefit_k"));
                row.setJobs(rs.getLong("jobs"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计大洲成效失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 思政 2“授人以渔”：属地用工占比与培训人次双达标的项目。
     *
     * <p>直接复用 B 的 {@link HireRow}（其字段即“最新一期月报”的属地用工），
     * 不新建同名类；“培训人次”作为第二个筛选条件（取自 {@code t_project_outcome.training_ppl}），
     * 只用于过滤、不额外塞进 VO。</p>
     *
     * @param localHirePct 属地用工占比阈值（百分比数值，如 70）
     * @param trainingPpl  培训人次阈值（如 1000）
     * @return 达标项目列表，按属地用工占比降序
     */
    public List<HireRow> findHighLocalHire(double localHirePct, int trainingPpl) {
        String sql = "SELECT p.project_id, p.project_code, p.project_name, "
                + "       lr.period_ym, lr.local_staff, lr.on_site_staff, "
                + "       ROUND(100.0 * lr.local_staff / NULLIF(lr.on_site_staff, 0), 1) AS local_hire_pct "
                + "FROM t_project p "
                + "JOIN (SELECT project_id, MAX(period_ym) AS ym "
                + "      FROM t_progress_report GROUP BY project_id) mx ON mx.project_id = p.project_id "
                + "JOIN t_progress_report lr ON lr.project_id = p.project_id AND lr.period_ym = mx.ym "
                + "JOIN t_project_outcome o ON o.project_id = p.project_id "
                + "WHERE ROUND(100.0 * lr.local_staff / NULLIF(lr.on_site_staff, 0), 1) > ? "
                + "  AND o.training_ppl > ? "
                + "ORDER BY local_hire_pct DESC";
        List<HireRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, localHirePct);
            ps.setInt(2, trainingPpl);
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
        } catch (SQLException e) {
            throw new DataAccessException("查询属地用工成效失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 思政 3“绿色丝绸之路”：按领域汇总绿色装机容量与输出标准数量。
     *
     * @return 绿色成效行，按绿色装机降序
     */
    public List<GreenRow> statGreenByField() {
        String sql = "SELECT p.field_code, "
                + "       COALESCE(d.item_name, p.field_code) AS field_name, "
                + "       COALESCE(SUM(o.green_mw), 0) AS green_mw, "
                + "       CAST(COALESCE(SUM(o.standards_out), 0) AS SIGNED) AS standards "
                + "FROM t_project p "
                + "JOIN t_project_outcome o ON o.project_id = p.project_id "
                + "LEFT JOIN t_dict_type dt ON dt.type_code = 'FIELD' "
                + "LEFT JOIN t_dict_item d ON d.dict_type_id = dt.dict_type_id AND d.item_code = p.field_code "
                + "GROUP BY p.field_code, d.item_name "
                + "ORDER BY green_mw DESC";
        List<GreenRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GreenRow row = new GreenRow();
                row.setFieldCode(rs.getString("field_code"));
                row.setFieldName(rs.getString("field_name"));
                row.setGreenMw(rs.getBigDecimal("green_mw"));
                row.setStandards(rs.getLong("standards"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计绿色丝绸之路成效失败：" + e.getMessage(), e);
        }
        return list;
    }

    /** 读取可空 INT 列，null 安全。 */
    private static Integer readInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** 把结果集里的 DATE 列读成 LocalDate，null 安全。 */
    private static LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }
}
