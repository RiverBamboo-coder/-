package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.common.Page;
import com.bri.vo.EnterpriseVO;
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
