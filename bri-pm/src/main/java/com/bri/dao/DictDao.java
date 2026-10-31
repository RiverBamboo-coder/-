package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.vo.DictItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据字典（M0 / X4）的数据访问对象：所有下拉框的数据源。
 *
 * <p>用法：{@code dictDao.listByType("PROJECT_STATUS")} 拿到 5 个状态，
 * 界面上直接把 {@code getItemCode()} 作为入库值、{@code getItemName()} 作为显示值。</p>
 *
 * @author A
 */
public class DictDao {

    /**
     * 按字典类型编码取启用的字典项（按 sort_no 排序）。
     *
     * @param typeCode 类型编码：FIELD / MODE / CURRENCY / ENTERPRISE_NATURE / ROLE /
     *                 PROJECT_STATUS / CONTRACT_TYPE / FUND_SOURCE_TYPE / COMMIT_TYPE / WARNING_LEVEL
     */
    public List<DictItem> listByType(String typeCode) {
        String sql = "SELECT i.item_id, t.type_code, i.item_code, i.item_name, i.sort_no, i.enabled"
                + " FROM t_dict_item i"
                + " JOIN t_dict_type t ON t.dict_type_id = i.dict_type_id"
                + " WHERE t.type_code = ? AND i.enabled = 1"
                + " ORDER BY i.sort_no, i.item_code";
        List<DictItem> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, typeCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DictItem item = new DictItem();
                    item.setItemId(rs.getInt("item_id"));
                    item.setTypeCode(rs.getString("type_code"));
                    item.setItemCode(rs.getString("item_code"));
                    item.setItemName(rs.getString("item_name"));
                    item.setSortNo(rs.getInt("sort_no"));
                    item.setEnabled(rs.getInt("enabled") == 1);
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询字典项失败（类型 " + typeCode + "）：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 取某个字典项的显示名（找不到返回入参本身，界面不至于出现空白）。
     *
     * @param typeCode 类型编码
     * @param itemCode 项编码
     */
    public String itemName(String typeCode, String itemCode) {
        String sql = "SELECT i.item_name FROM t_dict_item i"
                + " JOIN t_dict_type t ON t.dict_type_id = i.dict_type_id"
                + " WHERE t.type_code = ? AND i.item_code = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, typeCode);
            ps.setString(2, itemCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : itemCode;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询字典项名称失败：" + e.getMessage(), e);
        }
    }
}
