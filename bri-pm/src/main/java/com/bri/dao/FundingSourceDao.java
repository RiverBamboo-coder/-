package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.common.Result;
import com.bri.common.TextUtil;
import com.bri.vo.FundingSourceVO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 资金来源 DAO（M4 · t_funding_source）。B 负责。
 *
 * <p>用途：M4 界面右侧“资金来源”下拉框、Q4-3 来源占比的维度表维护。</p>
 *
 * @author B
 */
public class FundingSourceDao {

    /** 来源类型：与数据库 ck_source_type 一致，界面下拉框直接用这个数组。 */
    public static final String[] SOURCE_TYPES = {"政府援助", "政策性银行", "商业贷款", "企业自筹", "多边机构"};

    private static final String SQL_SELECT = """
            SELECT s.source_id, s.source_code, s.source_name, s.source_type,
                   s.country_code, c.country_name, s.institution, s.remark
            FROM t_funding_source s
            LEFT JOIN t_country c ON c.country_code = s.country_code
            """;

    /** 全部资金来源（界面下拉框用）。 */
    public List<FundingSourceVO> findAll() {
        String sql = SQL_SELECT + " ORDER BY s.source_id";
        List<FundingSourceVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查询资金来源失败：" + e.getMessage(), e);
        }
    }

    /** 按类型查询（如只列“政策性银行”）。 */
    public List<FundingSourceVO> findByType(String sourceType) {
        String sql = SQL_SELECT + " WHERE s.source_type = ? ORDER BY s.source_id";
        List<FundingSourceVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, TextUtil.trimToNull(sourceType));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("按类型查询资金来源失败：" + e.getMessage(), e);
        }
    }

    /** 新增来源，返回自增主键。 */
    public Result<Long> insert(FundingSourceVO vo) {
        if (vo == null || TextUtil.isBlank(vo.getSourceName())) {
            return Result.fail("新增失败：来源名称不能为空");
        }
        if (TextUtil.isBlank(vo.getSourceCode())) {
            return Result.fail("新增失败：来源编码不能为空（如 SRC09）");
        }
        if (TextUtil.isBlank(vo.getSourceType())) {
            return Result.fail("新增失败：请选择来源类型");
        }
        String sql = """
                INSERT INTO t_funding_source (source_code, source_name, source_type, country_code, institution, remark)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            DbUtil.setParameters(ps,
                    vo.getSourceCode().trim(),
                    vo.getSourceName().trim(),
                    vo.getSourceType(),
                    TextUtil.trimToNull(vo.getCountryCode()),
                    TextUtil.trimToNull(vo.getInstitution()),
                    TextUtil.trimToNull(vo.getRemark()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return Result.ok(keys.getLong(1), "资金来源已新增");
                }
            }
            return Result.ok(null, "资金来源已新增");
        } catch (SQLException e) {
            return Result.fail("新增资金来源失败：" + friendly(e));
        }
    }

    /** 修改来源。 */
    public Result<Integer> update(FundingSourceVO vo) {
        if (vo == null || vo.getSourceId() <= 0) {
            return Result.fail("修改失败：主键缺失");
        }
        String sql = """
                UPDATE t_funding_source
                   SET source_name = ?, source_type = ?, country_code = ?, institution = ?, remark = ?
                 WHERE source_id = ?
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps,
                    TextUtil.trimToNull(vo.getSourceName()),
                    vo.getSourceType(),
                    TextUtil.trimToNull(vo.getCountryCode()),
                    TextUtil.trimToNull(vo.getInstitution()),
                    TextUtil.trimToNull(vo.getRemark()),
                    vo.getSourceId());
            int rows = ps.executeUpdate();
            return Result.ok(rows, rows == 0 ? "没有找到要修改的资金来源" : "资金来源已修改");
        } catch (SQLException e) {
            return Result.fail("修改资金来源失败：" + friendly(e));
        }
    }

    private static String friendly(SQLException e) {
        switch (e.getErrorCode()) {
            case 1062:
                return "来源编码已存在（唯一约束 uk_source_code）";
            case 1452:
                return "所属国家不存在（外键 fk_source_country 校验失败）";
            case 3819:
                return "来源类型必须是 政府援助/政策性银行/商业贷款/企业自筹/多边机构 之一";
            default:
                return e.getMessage();
        }
    }

    private static FundingSourceVO map(ResultSet rs) throws SQLException {
        FundingSourceVO vo = new FundingSourceVO();
        vo.setSourceId(rs.getLong("source_id"));
        vo.setSourceCode(rs.getString("source_code"));
        vo.setSourceName(rs.getString("source_name"));
        vo.setSourceType(rs.getString("source_type"));
        vo.setCountryCode(rs.getString("country_code"));
        vo.setCountryName(rs.getString("country_name"));
        vo.setInstitution(rs.getString("institution"));
        vo.setRemark(rs.getString("remark"));
        return vo;
    }
}
