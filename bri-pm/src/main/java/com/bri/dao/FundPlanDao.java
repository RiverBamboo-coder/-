package com.bri.dao;

import com.bri.common.DbUtil;
import com.bri.common.DataAccessException;
import com.bri.common.Page;
import com.bri.common.Result;
import com.bri.common.TextUtil;
import com.bri.vo.FundPlanVO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * 年度资金计划 DAO（M4 · t_fund_plan）。B 负责。
 *
 * <p>要点：</p>
 * <ul>
 *   <li>一年一条由数据库的 {@code uk_plan_project_year} 保证，所以保存用
 *       {@code INSERT ... ON DUPLICATE KEY UPDATE}（upsert），Java 侧不需要“先查再判”；</li>
 *   <li>写完返回 {@link com.bri.common.Result}，界面直接弹 message，堆栈不出 DAO；</li>
 *   <li>所有 SQL 都是 {@link PreparedStatement} 参数化，没有任何字符串拼接。</li>
 * </ul>
 *
 * @author B
 */
public class FundPlanDao {

    private static final String SQL_SELECT = """
            SELECT pl.plan_id, pl.project_id, p.project_code, p.project_name,
                   pl.plan_year, pl.plan_amount, pl.currency_code, pl.approved_date, pl.remark
            FROM t_fund_plan pl
            JOIN t_project p ON p.project_id = pl.project_id
            """;

    /**
     * 某个项目的全部年度计划（按年度升序）。
     *
     * @param projectId 项目主键
     * @return 计划列表，永不为 null
     * @throws DataAccessException 查询失败
     */
    public List<FundPlanVO> findByProject(long projectId) {
        String sql = SQL_SELECT + " WHERE pl.project_id = ? ORDER BY pl.plan_year";
        List<FundPlanVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查询年度资金计划失败：" + e.getMessage(), e);
        }
    }

    /**
     * 分页查询年度计划（可按年度过滤，{@code planYear} 为 null 表示不限）。
     *
     * @param planYear 年度，可为 null
     * @param page     分页与排序参数（排序白名单：plan_year / plan_amount）
     * @return 当前页数据
     */
    public List<FundPlanVO> findPage(Integer planYear, Page page) {
        StringBuilder sql = new StringBuilder(SQL_SELECT);
        List<Object> params = new ArrayList<>();
        if (planYear != null) {
            sql.append(" WHERE pl.plan_year = ?");
            params.add(planYear);
        }
        String order = page.resolveOrderBy("plan_year", "plan_amount");
        sql.append(order == null ? " ORDER BY pl.plan_year DESC, pl.project_id" : " " + order);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(page.getPageSize());
        params.add(page.getOffset());

        List<FundPlanVO> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DbUtil.setParameters(ps, params.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("分页查询年度资金计划失败：" + e.getMessage(), e);
        }
    }

    /**
     * 保存年度计划：有则更新，无则插入（依赖 {@code uk_plan_project_year}）。
     *
     * @param plan 计划，projectId / planYear / planAmount 必填
     * @return 成功返回计划主键（新增与更新都会返回），失败返回中文原因
     */
    public com.bri.common.Result<Long> upsert(FundPlanVO plan) {
        if (plan == null || plan.getProjectId() <= 0 || plan.getPlanYear() <= 0) {
            return Result.fail("保存失败：项目与计划年度必须填写");
        }
        if (plan.getPlanAmount() == null || plan.getPlanAmount().signum() <= 0) {
            return Result.fail("保存失败：计划金额必须大于 0（数据库有 ck_plan_amount_positive 约束）");
        }
        String sql = """
                INSERT INTO t_fund_plan (project_id, plan_year, plan_amount, currency_code, approved_date, remark)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    plan_id       = LAST_INSERT_ID(plan_id),
                    plan_amount   = VALUES(plan_amount),
                    currency_code = VALUES(currency_code),
                    approved_date = VALUES(approved_date),
                    remark        = VALUES(remark)
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            DbUtil.setParameters(ps,
                    plan.getProjectId(),
                    plan.getPlanYear(),
                    plan.getPlanAmount(),
                    TextUtil.defaultIfBlank(plan.getCurrencyCode(), "CNY"),
                    plan.getApprovedDate(),
                    TextUtil.trimToNull(plan.getRemark()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return Result.ok(keys.getLong(1), "年度计划已保存");
                }
            }
            return Result.ok(null, "年度计划已保存");
        } catch (SQLException e) {
            return Result.fail("保存年度计划失败：" + friendly(e));
        }
    }

    /**
     * 删除某项目某年度的计划。
     *
     * @param projectId 项目主键
     * @param planYear  年度
     * @return 受影响行数
     */
    public com.bri.common.Result<Integer> delete(long projectId, int planYear) {
        String sql = "DELETE FROM t_fund_plan WHERE project_id = ? AND plan_year = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DbUtil.setParameters(ps, projectId, planYear);
            int rows = ps.executeUpdate();
            return Result.ok(rows, rows == 0 ? "没有找到该年度的计划" : "已删除该年度计划");
        } catch (SQLException e) {
            return Result.fail("删除年度计划失败：" + friendly(e));
        }
    }

    /**
     * 把驱动抛出的 SQLException 翻成一句人话（界面直接弹）。
     * 名称冲突/外键/检查约束是最常见的三类，单独给中文提示。
     */
    private static String friendly(SQLException e) {
        switch (e.getErrorCode()) {
            case 1062:
                return "该项目该年度已有计划（唯一约束 uk_plan_project_year），请改为修改";
            case 1452:
                return "项目不存在（外键 fk_plan_project 校验失败）";
            case 3819:
                return "数据不满足检查约束（金额必须大于 0，年度必须在 2000–2100 之间）";
            default:
                return e.getMessage();
        }
    }

    private static FundPlanVO map(ResultSet rs) throws SQLException {
        FundPlanVO vo = new FundPlanVO();
        vo.setPlanId(rs.getLong("plan_id"));
        vo.setProjectId(rs.getLong("project_id"));
        vo.setProjectCode(rs.getString("project_code"));
        vo.setProjectName(rs.getString("project_name"));
        vo.setPlanYear(rs.getInt("plan_year"));
        BigDecimal amount = rs.getBigDecimal("plan_amount");
        vo.setPlanAmount(amount);
        vo.setCurrencyCode(rs.getString("currency_code"));
        Date approved = rs.getDate("approved_date");
        vo.setApprovedDate(approved == null ? null : approved.toLocalDate());
        vo.setRemark(rs.getString("remark"));
        return vo;
    }

    /** 供其它 DAO 复用的参数绑定（可空 Integer）。 */
    static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
