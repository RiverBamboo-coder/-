package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.DbUtil;
import com.bri.common.Page;
import com.bri.common.TextUtil;
import com.bri.vo.CountryVO;
import com.bri.vo.StatRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 国家与区域（M1）的数据访问对象。
 *
 * <p>查询方法返回 VO 集合，失败时抛 {@link DataAccessException}（message 为可直接展示的中文）；
 * 写入方法失败时同样抛该异常，由界面统一兜底提示。所有 SQL 均为 {@link PreparedStatement} 参数化。</p>
 *
 * <p>对应任务书第 5 章接口契约：{@code findAll(Page)}、{@code findByRegion(int)}、
 * {@code statProjectsByRegion()}、{@code findCountriesWithoutProject()}，另提供编码查询与增删改。</p>
 *
 * @author A
 */
public class CountryDao {

    /** 列表/详情共用的查询骨架（项目数与投资额用相关子查询，避免 GROUP BY 干扰分页） */
    private static final String BASE_SELECT =
            "SELECT c.country_code, c.country_name, c.country_name_en, c.region_id,"
            + " r.region_name, cont.region_name AS continent_name, c.is_belt_road, c.population_k,"
            + " (SELECT COUNT(*) FROM t_project p WHERE p.country_code = c.country_code) AS project_count,"
            + " (SELECT COALESCE(SUM(p.invest_amount), 0) FROM t_project p"
            + "   WHERE p.country_code = c.country_code) AS invest_total"
            + " FROM t_country c"
            + " JOIN t_region r ON r.region_id = c.region_id"
            + " LEFT JOIN t_region cont ON cont.region_id = r.parent_id";

    /** 排序白名单：界面只能按这些字段排序 */
    private static final String[] ORDER_COLUMNS = {
            "country_code", "country_name", "region_name", "is_belt_road", "project_count", "invest_total"
    };

    /** 分页查询国家列表 */
    public List<CountryVO> findAll(Page page) {
        Page p = page == null ? new Page() : page;
        String order = p.resolveOrderBy(ORDER_COLUMNS);
        String sql = BASE_SELECT
                + (order == null ? " ORDER BY c.country_code" : " " + order)
                + " LIMIT ? OFFSET ?";
        List<CountryVO> list = new ArrayList<>();
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
            throw new DataAccessException("查询国家列表失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 按洲或区域筛选（兼容两种入参：传二级区域 ID 或传大洲 ID 都能查）。
     *
     * @param regionId 区域 ID
     */
    public List<CountryVO> findByRegion(int regionId) {
        String sql = BASE_SELECT + " WHERE (c.region_id = ? OR r.parent_id = ?) ORDER BY c.country_code";
        List<CountryVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, regionId);
            ps.setInt(2, regionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("按区域查询国家失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 按 ISO 代码查询单个国家。
     *
     * @param countryCode ISO 三字母代码
     * @return 找不到返回 null
     */
    public CountryVO findByCode(String countryCode) {
        String sql = BASE_SELECT + " WHERE c.country_code = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, countryCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询国家失败：" + e.getMessage(), e);
        }
    }

    /**
     * 新增国家。
     *
     * @return 受影响行数
     */
    public int insert(CountryVO vo) {
        if (vo.getRegionId() == null) {
            throw new DataAccessException("所属区域不能为空，请先选择大洲/区域");
        }
        String sql = "INSERT INTO t_country (country_code, country_name, country_name_en, region_id,"
                + " is_belt_road, population_k) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vo.getCountryCode());
            ps.setString(2, vo.getCountryName());
            ps.setString(3, vo.getCountryNameEn());
            ps.setInt(4, vo.getRegionId());
            ps.setInt(5, vo.isBeltRoad() ? 1 : 0);
            ps.setBigDecimal(6, vo.getPopulationK());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("新增国家失败：" + e.getMessage(), e);
        }
    }

    /**
     * 修改国家（按主键 country_code 更新）。
     *
     * @return 受影响行数
     */
    public int update(CountryVO vo) {
        if (vo.getRegionId() == null) {
            throw new DataAccessException("所属区域不能为空，请先选择大洲/区域");
        }
        String sql = "UPDATE t_country SET country_name = ?, country_name_en = ?, region_id = ?,"
                + " is_belt_road = ?, population_k = ? WHERE country_code = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vo.getCountryName());
            ps.setString(2, vo.getCountryNameEn());
            ps.setInt(3, vo.getRegionId());
            ps.setInt(4, vo.isBeltRoad() ? 1 : 0);
            ps.setBigDecimal(5, vo.getPopulationK());
            ps.setString(6, vo.getCountryCode());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("修改国家失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除国家（有项目引用时会被外键挡住，错误信息会提示）。
     *
     * @return 受影响行数
     */
    public int deleteByCode(String countryCode) {
        String sql = "DELETE FROM t_country WHERE country_code = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, countryCode);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("删除国家失败（可能仍有项目引用该国家）：" + e.getMessage(), e);
        }
    }

    /** 国家总数（分页栏用） */
    public long countAll() {
        return DbUtil.queryCount("SELECT COUNT(*) FROM t_country");
    }

    /**
     * Q1-1：按大洲统计项目数与投资额（只统计一级区域，即洲）。
     */
    public List<StatRow> statProjectsByRegion() {
        String sql = "SELECT cont.region_code AS code, cont.region_name AS label,"
                + " COUNT(DISTINCT p.project_id) AS cnt,"
                + " COALESCE(SUM(p.invest_amount), 0) AS total"
                + " FROM t_region cont"
                + " JOIN t_region sub ON sub.parent_id = cont.region_id"
                + " JOIN t_country c ON c.region_id = sub.region_id"
                + " LEFT JOIN t_project p ON p.country_code = c.country_code"
                + " WHERE cont.level_no = 1"
                + " GROUP BY cont.region_id, cont.region_code, cont.region_name"
                + " ORDER BY cnt DESC, cont.region_id";
        return queryStat(sql);
    }

    /**
     * Q1-2：还没有项目的国家（NOT EXISTS 反连接）。
     */
    public List<CountryVO> findCountriesWithoutProject() {
        String sql = BASE_SELECT + " WHERE NOT EXISTS (SELECT 1 FROM t_project p"
                + " WHERE p.country_code = c.country_code) ORDER BY c.country_code";
        List<CountryVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询无项目国家失败：" + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Q1-3：按区域统计覆盖率。
     *
     * <p>返回行含义：{@code cnt} 国家总数，{@code covered} 有项目的国家数，{@code pct} 覆盖率百分比。</p>
     */
    public List<StatRow> statRegionCoverage() {
        String sql = "SELECT sub.region_code AS code, sub.region_name AS label,"
                + " COUNT(DISTINCT c.country_code) AS cnt,"
                + " COUNT(DISTINCT p.country_code) AS covered,"
                + " ROUND(100.0 * COUNT(DISTINCT p.country_code)"
                + "       / NULLIF(COUNT(DISTINCT c.country_code), 0), 2) AS pct"
                + " FROM t_region sub"
                + " JOIN t_country c ON c.region_id = sub.region_id"
                + " LEFT JOIN t_project p ON p.country_code = c.country_code"
                + " WHERE sub.level_no = 2"
                + " GROUP BY sub.region_id, sub.region_code, sub.region_name"
                + " ORDER BY pct DESC, sub.region_id";
        List<StatRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StatRow row = new StatRow();
                row.setCode(rs.getString("code"));
                row.setLabel(rs.getString("label"));
                row.setCnt(rs.getLong("cnt"));
                row.setCovered(rs.getLong("covered"));
                row.setPct(rs.getBigDecimal("pct"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计区域覆盖率失败：" + e.getMessage(), e);
        }
        return list;
    }

    private List<StatRow> queryStat(String sql) {
        List<StatRow> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StatRow row = new StatRow();
                row.setCode(rs.getString("code"));
                row.setLabel(rs.getString("label"));
                row.setCnt(rs.getLong("cnt"));
                row.setTotal(rs.getBigDecimal("total"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计失败：" + e.getMessage(), e);
        }
        return list;
    }

    private CountryVO map(ResultSet rs) throws SQLException {
        CountryVO vo = new CountryVO();
        vo.setCountryCode(rs.getString("country_code"));
        vo.setCountryName(rs.getString("country_name"));
        vo.setCountryNameEn(rs.getString("country_name_en"));
        vo.setRegionId(rs.getInt("region_id"));
        vo.setRegionName(rs.getString("region_name"));
        vo.setContinentName(rs.getString("continent_name"));
        vo.setBeltRoad(rs.getInt("is_belt_road") == 1);
        vo.setPopulationK(rs.getBigDecimal("population_k"));
        vo.setProjectCount(rs.getLong("project_count"));
        vo.setInvestTotal(rs.getBigDecimal("invest_total"));
        return vo;
    }

    /** 供界面把关键字转成 LIKE 参数（转义 % 和 _） */
    public static String likeKeyword(String keyword) {
        return TextUtil.containsLike(keyword);
    }
}
