package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.vo.MetaColumnVO;
import com.bri.vo.MetaConstraintVO;
import com.bri.vo.MetaTableVO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * X4 数据字典（A-9）：读 {@code information_schema} 元数据，列出全部表、字段与约束。
 *
 * <p>这是“数据字典界面”的数据源，也是答辩时展示“数据库对象关系完整”的证据：
 * 界面（D）先调 {@link #listTables()} 出表清单，选中某表再调
 * {@link #listColumns(String)} 与 {@link #listConstraints(String)}。</p>
 *
 * <p>只读：不提供任何 DDL 能力，也不拼接表名进 SQL（表名一律作为 {@code ?} 参数传入）。</p>
 *
 * @author A
 */
public class MetaDao {

    /**
     * 当前库（{@code DATABASE()}）的全部表。
     */
    public List<MetaTableVO> listTables() {
        String sql = "SELECT table_name, table_comment, table_rows"
                + " FROM information_schema.tables"
                + " WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'"
                + " ORDER BY table_name";
        List<MetaTableVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MetaTableVO vo = new MetaTableVO();
                vo.setTableName(rs.getString("table_name"));
                vo.setTableComment(rs.getString("table_comment"));
                long rows = rs.getLong("table_rows");
                // InnoDB 的 table_rows 是估算值，可能为 NULL（统计未采集）；用 -1 表示“未知”
                vo.setRowCount(rs.wasNull() ? -1L : rows);
                list.add(vo);
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询表清单失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 某张表的字段清单（含类型、可空、键、生成列标记、注释）。
     *
     * @param tableName 表名，必须存在于当前库
     */
    public List<MetaColumnVO> listColumns(String tableName) {
        String sql = "SELECT table_name, column_name, column_type, is_nullable, column_key, extra,"
                + " column_comment, ordinal_position"
                + " FROM information_schema.columns"
                + " WHERE table_schema = DATABASE() AND table_name = ?"
                + " ORDER BY ordinal_position";
        List<MetaColumnVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MetaColumnVO vo = new MetaColumnVO();
                    vo.setTableName(rs.getString("table_name"));
                    vo.setColumnName(rs.getString("column_name"));
                    vo.setColumnType(rs.getString("column_type"));
                    vo.setNullable("YES".equalsIgnoreCase(rs.getString("is_nullable")));
                    vo.setColumnKey(rs.getString("column_key"));
                    vo.setExtra(rs.getString("extra"));
                    vo.setColumnComment(rs.getString("column_comment"));
                    vo.setOrdinal(rs.getInt("ordinal_position"));
                    list.add(vo);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询字段清单失败（表 " + tableName + "）：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 某张表的约束清单：主键、外键（含引用表/列）、唯一键、检查约束。
     *
     * <p>{@code columns} 是逗号分隔的列名；外键行额外带回 {@code refTable/refColumns}。</p>
     *
     * @param tableName 表名
     */
    public List<MetaConstraintVO> listConstraints(String tableName) {
        String sql = "SELECT tc.table_name, tc.constraint_name, tc.constraint_type,"
                + " GROUP_CONCAT(k.column_name ORDER BY k.ordinal_position) AS cols,"
                + " k.referenced_table_name AS ref_table,"
                + " GROUP_CONCAT(k.referenced_column_name ORDER BY k.ordinal_position) AS ref_cols"
                + " FROM information_schema.table_constraints tc"
                + " LEFT JOIN information_schema.key_column_usage k"
                + "   ON k.constraint_schema = tc.constraint_schema"
                + "  AND k.constraint_name = tc.constraint_name"
                + "  AND k.table_name = tc.table_name"
                + " WHERE tc.table_schema = DATABASE() AND tc.table_name = ?"
                + " GROUP BY tc.table_name, tc.constraint_name, tc.constraint_type, k.referenced_table_name"
                + " ORDER BY tc.constraint_type, tc.constraint_name";
        List<MetaConstraintVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MetaConstraintVO vo = new MetaConstraintVO();
                    vo.setTableName(rs.getString("table_name"));
                    vo.setConstraintName(rs.getString("constraint_name"));
                    vo.setConstraintType(rs.getString("constraint_type"));
                    vo.setColumns(rs.getString("cols"));
                    vo.setRefTable(rs.getString("ref_table"));
                    vo.setRefColumns(rs.getString("ref_cols"));
                    list.add(vo);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询约束清单失败（表 " + tableName + "）：" + e.getMessage(), e);
        }
        return list;
    }

    /** 当前库的表数量（状态栏“表数量”用） */
    public long countTables() {
        return DbUtil.queryCount("SELECT COUNT(*) FROM information_schema.tables"
                + " WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'");
    }
}
