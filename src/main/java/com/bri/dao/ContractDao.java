package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.vo.ContractVO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目合同（M2 详情页）的数据访问对象。
 *
 * <p>合同不属于 A 的“模块主线”（M2 主表才是），但项目详情页要显示合同列表，
 * 而合同表由 A 统一建、字段简单，这里一并提供，避免 D 再写一个临时查询。</p>
 *
 * @author A
 */
public class ContractDao {

    /**
     * 查某个项目的全部合同。
     *
     * @param projectId 项目 ID
     */
    public List<ContractVO> findByProject(long projectId) {
        String sql = "SELECT c.contract_id, c.project_id, c.contract_no, c.contract_type,"
                + " e.enterprise_name AS party_b_name, c.amount, c.currency_code, c.sign_date"
                + " FROM t_contract c"
                + " LEFT JOIN t_enterprise e ON e.enterprise_id = c.party_b_enterprise_id"
                + " WHERE c.project_id = ?"
                + " ORDER BY c.contract_id";
        List<ContractVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ContractVO vo = new ContractVO();
                    vo.setContractId(rs.getLong("contract_id"));
                    vo.setProjectId(rs.getLong("project_id"));
                    vo.setContractNo(rs.getString("contract_no"));
                    vo.setContractType(rs.getString("contract_type"));
                    vo.setPartyBName(rs.getString("party_b_name"));
                    vo.setAmount(rs.getBigDecimal("amount"));
                    vo.setCurrencyCode(rs.getString("currency_code"));
                    java.sql.Date d = rs.getDate("sign_date");
                    vo.setSignDate(d == null ? null : d.toLocalDate());
                    list.add(vo);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询项目合同失败：" + e.getMessage(), e);
        }
        return list;
    }

    /** 合同总份数（验收自检用） */
    public long countAll() {
        return DbUtil.queryCount("SELECT COUNT(*) FROM t_contract");
    }
}
