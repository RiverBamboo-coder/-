package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.common.Page;
import com.bri.vo.ContinentSpreadRow;
import com.bri.vo.DualRoleProjectRow;
import com.bri.vo.EnterpriseVO;
import com.bri.vo.FieldTopEnterpriseRow;
import com.bri.vo.ProjectRoleVO;
import com.bri.vo.RankRow;
import com.bri.vo.ShareRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 参与企业与组织（M3）的数据访问对象。
 *
 * <p>对应第 5 章接口契约：{@code findAll(Page)}、{@code findProjectsByEnterprise(long)}、
 * {@code statEnterpriseRanking(int)}、{@code statConsortiumShare()}。</p>
 *
 * @author A
 */
public class EnterpriseDao {

    private static final String BASE_SELECT =
            "SELECT e.enterprise_id, e.enterprise_name, e.enterprise_name_en, e.nature_code,"
            + " n.item_name AS nature_name, e.country_code, c.country_name,"
            + " e.parent_enterprise_id, pe.enterprise_name AS parent_name,"
            + " (SELECT COUNT(DISTINCT pp.project_id) FROM t_project_participant pp"
            + "   WHERE pp.enterprise_id = e.enterprise_id) AS project_count,"
            + " e.remark"
            + " FROM t_enterprise e"
            + " JOIN t_country c ON c.country_code = e.country_code"
            + " LEFT JOIN t_enterprise pe ON pe.enterprise_id = e.parent_enterprise_id"
            + " LEFT JOIN t_dict_item n ON n.dict_type_id = 4 AND n.item_code = e.nature_code";

    private static final String[] ORDER_COLUMNS = {
            "enterprise_name", "nature_name", "country_name", "project_count", "enterprise_id"
    };

    /** 分页查询企业列表（含参与项目数） */
    public List<EnterpriseVO> findAll(Page page) {
        Page p = page == null ? new Page() : page;
        String order = p.resolveOrderBy(ORDER_COLUMNS);
        String sql = BASE_SELECT
                + (order == null ? " ORDER BY project_count DESC, e.enterprise_name" : " " + order)
                + " LIMIT ? OFFSET ?";
        List<EnterpriseVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getPageSize());
            ps.setInt(2, p.getOffset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询企业列表失败：" + e.getMessage(), e);
        }
        return list;
    }

    /** 企业总数（分页栏用） */
    public long countAll() {
        return DbUtil.queryCount("SELECT COUNT(*) FROM t_enterprise");
    }

    /**
     * 某企业参与的项目与角色（M3 的“选中企业 → 参与项目列表”）。
     *
     * @param enterpriseId 企业 ID
     */
    public List<ProjectRoleVO> findProjectsByEnterprise(long enterpriseId) {
        String sql = "SELECT pp.project_id, p.project_code, p.project_name, c.country_name, p.status,"
                + " pp.role_code, d.item_name AS role_name, pp.share_pct, pp.join_date,"
                + " pp.consortium_id, cs.consortium_name"
                + " FROM t_project_participant pp"
                + " JOIN t_project p ON p.project_id = pp.project_id"
                + " JOIN t_country c ON c.country_code = p.country_code"
                + " LEFT JOIN t_dict_item d ON d.dict_type_id = 5 AND d.item_code = pp.role_code"
                + " LEFT JOIN t_consortium cs ON cs.consortium_id = pp.consortium_id"
                + " WHERE pp.enterprise_id = ?"
                + " ORDER BY p.project_code, pp.role_code";
        List<ProjectRoleVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, enterpriseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProjectRoleVO vo = new ProjectRoleVO();
                    vo.setProjectId(rs.getLong("project_id"));
                    vo.setProjectCode(rs.getString("project_code"));
                    vo.setProjectName(rs.getString("project_name"));
                    vo.setCountryName(rs.getString("country_name"));
                    vo.setStatus(rs.getString("status"));
                    vo.setRoleCode(rs.getString("role_code"));
                    vo.setRoleName(rs.getString("role_name"));
                    vo.setSharePct(rs.getBigDecimal("share_pct"));
                    java.sql.Date joinDate = rs.getDate("join_date");
                    vo.setJoinDate(joinDate == null ? null : joinDate.toLocalDate());
                    long cid = rs.getLong("consortium_id");
                    vo.setConsortiumId(rs.wasNull() ? null : cid);
                    vo.setConsortiumName(rs.getString("consortium_name"));
                    list.add(vo);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询企业参与项目失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q3-1：企业参与度排行（按参与项目数、作为总承包的投资额倒序）。
     *
     * @param topN 取前 N 名
     */
    public List<RankRow> statEnterpriseRanking(int topN) {
        String sql = "SELECT e.enterprise_id, e.enterprise_name,"
                + " COALESCE(n.item_name, e.nature_code) AS nature_name,"
                + " (SELECT COUNT(DISTINCT pp.project_id) FROM t_project_participant pp"
                + "   WHERE pp.enterprise_id = e.enterprise_id) AS proj_cnt,"
                + " (SELECT COALESCE(SUM(p.invest_amount), 0)"
                + "    FROM t_project_participant pp JOIN t_project p ON p.project_id = pp.project_id"
                + "   WHERE pp.enterprise_id = e.enterprise_id AND pp.role_code = 'ZC') AS zc_amount"
                + " FROM t_enterprise e"
                + " LEFT JOIN t_dict_item n ON n.dict_type_id = 4 AND n.item_code = e.nature_code"
                + " ORDER BY proj_cnt DESC, zc_amount DESC, e.enterprise_id"
                + " LIMIT ?";
        List<RankRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, topN <= 0 ? 10 : topN);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 0;
                while (rs.next()) {
                    rank++;
                    RankRow row = new RankRow();
                    row.setRankNo(rank);
                    row.setEnterpriseId(rs.getLong("enterprise_id"));
                    row.setEnterpriseName(rs.getString("enterprise_name"));
                    row.setNatureName(rs.getString("nature_name"));
                    row.setProjectCount(rs.getLong("proj_cnt"));
                    row.setZcAmount(rs.getBigDecimal("zc_amount"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("企业排行统计失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q3-4：联合体成员份额核对（每个联合体成员份额合计应为 100）。
     */
    public List<ShareRow> statConsortiumShare() {
        String sql = "SELECT cs.consortium_id, cs.consortium_name, l.enterprise_name AS leader_name,"
                + " COUNT(m.enterprise_id) AS member_cnt,"
                + " COALESCE(SUM(m.share_pct), 0) AS share_sum,"
                + " (SELECT COUNT(DISTINCT pp.project_id) FROM t_project_participant pp"
                + "   WHERE pp.consortium_id = cs.consortium_id) AS project_count"
                + " FROM t_consortium cs"
                + " JOIN t_enterprise l ON l.enterprise_id = cs.leader_enterprise_id"
                + " LEFT JOIN t_consortium_member m ON m.consortium_id = cs.consortium_id"
                + " GROUP BY cs.consortium_id, cs.consortium_name, l.enterprise_name"
                + " ORDER BY share_sum DESC, cs.consortium_id";
        List<ShareRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ShareRow row = new ShareRow();
                row.setConsortiumId(rs.getLong("consortium_id"));
                row.setConsortiumName(rs.getString("consortium_name"));
                row.setLeaderName(rs.getString("leader_name"));
                row.setMemberCount(rs.getInt("member_cnt"));
                row.setShareSum(rs.getBigDecimal("share_sum"));
                row.setProjectCount(rs.getLong("project_count"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("联合体份额统计失败：" + e.getMessage(), e);
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Q3-2 / Q3-3 / Q3-5（本轮补齐：领域冠军企业、总包+分包并存、跨洲布局）
    // ------------------------------------------------------------------

    /**
     * Q3-2：每个领域承接项目最多的企业（每个领域只返回冠军一行）。
     *
     * <p>用窗口函数 {@code ROW_NUMBER() OVER (PARTITION BY field_code ORDER BY 项目数 DESC)}
     * 取 rn = 1：比“先查最大值再回查”少一次往返，并列时也只取一家（按企业编号升序兜底，
     * 保证结果稳定可复现）。</p>
     *
     * @return 各领域的冠军企业，按项目数降序
     */
    public List<FieldTopEnterpriseRow> statTopEnterpriseByField() {
        String sql = "SELECT field_code, field_name, enterprise_id, enterprise_name,"
                + "       project_cnt, total_invest"
                + " FROM ("
                + "   SELECT p.field_code,"
                + "          COALESCE(d.item_name, p.field_code) AS field_name,"
                + "          e.enterprise_id, e.enterprise_name,"
                + "          COUNT(DISTINCT p.project_id) AS project_cnt,"
                + "          COALESCE(SUM(p.invest_amount), 0) AS total_invest,"
                + "          ROW_NUMBER() OVER (PARTITION BY p.field_code"
                + "                             ORDER BY COUNT(DISTINCT p.project_id) DESC,"
                + "                                      e.enterprise_id) AS rn"
                + "   FROM t_project p"
                + "   JOIN t_project_participant pp ON pp.project_id = p.project_id"
                + "   JOIN t_enterprise e ON e.enterprise_id = pp.enterprise_id"
                + "   LEFT JOIN t_dict_item d ON d.dict_type_id = 1 AND d.item_code = p.field_code"
                + "   GROUP BY p.field_code, d.item_name, e.enterprise_id, e.enterprise_name"
                + " ) t"
                + " WHERE rn = 1"
                + " ORDER BY project_cnt DESC, field_code";
        List<FieldTopEnterpriseRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FieldTopEnterpriseRow row = new FieldTopEnterpriseRow();
                row.setFieldCode(rs.getString("field_code"));
                row.setFieldName(rs.getString("field_name"));
                row.setEnterpriseId(rs.getLong("enterprise_id"));
                row.setEnterpriseName(rs.getString("enterprise_name"));
                row.setProjectCount(rs.getLong("project_cnt"));
                row.setTotalInvest(rs.getBigDecimal("total_invest"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计各领域冠军企业失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q3-3：项目中既存在总承包（ZC）又存在分包（FB）的，用于核对“总包—分包”链条是否完整。
     *
     * <p>用条件聚合 {@code SUM(CASE WHEN role_code = 'ZC' ...)} 在一条 SQL 里同时统计两种角色，
     * 避免自连接两次；{@code HAVING} 同时要求两种角色都 > 0。</p>
     *
     * @return 两种角色并存的项目，按总包数降序
     */
    public List<DualRoleProjectRow> findProjectsWithBothRoles() {
        String sql = "SELECT p.project_id, p.project_code, p.project_name,"
                + "       COUNT(DISTINCT CASE WHEN pp.role_code = 'ZC' THEN pp.enterprise_id END) AS zc_cnt,"
                + "       COUNT(DISTINCT CASE WHEN pp.role_code = 'FB' THEN pp.enterprise_id END) AS fb_cnt,"
                + "       COUNT(DISTINCT pp.enterprise_id) AS participant_cnt"
                + " FROM t_project p"
                + " JOIN t_project_participant pp ON pp.project_id = p.project_id"
                + " GROUP BY p.project_id, p.project_code, p.project_name"
                + " HAVING zc_cnt > 0 AND fb_cnt > 0"
                + " ORDER BY zc_cnt DESC, fb_cnt DESC, p.project_code";
        List<DualRoleProjectRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DualRoleProjectRow row = new DualRoleProjectRow();
                row.setProjectId(rs.getLong("project_id"));
                row.setProjectCode(rs.getString("project_code"));
                row.setProjectName(rs.getString("project_name"));
                row.setZcCount(rs.getLong("zc_cnt"));
                row.setFbCount(rs.getLong("fb_cnt"));
                row.setParticipantCount(rs.getLong("participant_cnt"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询总包与分包并存项目失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q3-5：在至少 N 个“洲”都有参与项目的企业（全球化布局）。
     *
     * <p>国家的 region 可能挂在二级区域下，故用
     * {@code COALESCE(二级.parent_id, 二级.region_id)} 上卷到一级（洲）；
     * {@code COUNT(DISTINCT 洲)} 保证同一洲的多个国家不重复计数。</p>
     *
     * @param minContinents 最少洲数（如 3）
     * @return 满足条件的企业，按洲数、国家数降序
     */
    public List<ContinentSpreadRow> statEnterprisesAcrossContinents(int minContinents) {
        String sql = "SELECT e.enterprise_id, e.enterprise_name,"
                + "       COALESCE(dn.item_name, e.nature_code) AS nature_name,"
                + "       COUNT(DISTINCT COALESCE(r1.region_id, r2.region_id)) AS continent_cnt,"
                + "       COUNT(DISTINCT c.country_code) AS country_cnt,"
                + "       COUNT(DISTINCT pp.project_id) AS project_cnt,"
                + "       GROUP_CONCAT(DISTINCT COALESCE(r1.region_name, r2.region_name)"
                + "                    ORDER BY COALESCE(r1.region_name, r2.region_name)"
                + "                    SEPARATOR '、') AS continent_names"
                + " FROM t_enterprise e"
                + " JOIN t_project_participant pp ON pp.enterprise_id = e.enterprise_id"
                + " JOIN t_project p ON p.project_id = pp.project_id"
                + " JOIN t_country c ON c.country_code = p.country_code"
                + " JOIN t_region r2 ON r2.region_id = c.region_id"
                + " LEFT JOIN t_region r1 ON r1.region_id = r2.parent_id"
                + " LEFT JOIN t_dict_item dn ON dn.dict_type_id = 4 AND dn.item_code = e.nature_code"
                + " GROUP BY e.enterprise_id, e.enterprise_name, dn.item_name, e.nature_code"
                + " HAVING continent_cnt >= ?"
                + " ORDER BY continent_cnt DESC, country_cnt DESC, project_cnt DESC";
        List<ContinentSpreadRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minContinents);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ContinentSpreadRow row = new ContinentSpreadRow();
                    row.setEnterpriseId(rs.getLong("enterprise_id"));
                    row.setEnterpriseName(rs.getString("enterprise_name"));
                    row.setNatureName(rs.getString("nature_name"));
                    row.setContinentCount(rs.getLong("continent_cnt"));
                    row.setCountryCount(rs.getLong("country_cnt"));
                    row.setProjectCount(rs.getLong("project_cnt"));
                    row.setContinentNames(rs.getString("continent_names"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计跨洲布局企业失败：" + e.getMessage(), e);
        }
        return list;
    }

    private EnterpriseVO map(ResultSet rs) throws SQLException {
        EnterpriseVO vo = new EnterpriseVO();
        vo.setEnterpriseId(rs.getLong("enterprise_id"));
        vo.setEnterpriseName(rs.getString("enterprise_name"));
        vo.setEnterpriseNameEn(rs.getString("enterprise_name_en"));
        vo.setNatureCode(rs.getString("nature_code"));
        vo.setNatureName(rs.getString("nature_name"));
        vo.setCountryCode(rs.getString("country_code"));
        vo.setCountryName(rs.getString("country_name"));
        long parentId = rs.getLong("parent_enterprise_id");
        vo.setParentEnterpriseId(rs.wasNull() ? null : parentId);
        vo.setParentName(rs.getString("parent_name"));
        vo.setProjectCount(rs.getLong("project_count"));
        vo.setRemark(rs.getString("remark"));
        return vo;
    }
}
