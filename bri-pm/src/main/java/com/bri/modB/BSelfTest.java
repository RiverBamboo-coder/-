package com.bri.modB;

import com.bri.common.DbUtil;
import com.bri.common.Page;
import com.bri.common.Result;
import com.bri.dao.FundDao;
import com.bri.dao.FundingSourceDao;
import com.bri.dao.ReportDao;
import com.bri.dao.ReportIndicatorDao;
import com.bri.dao.WarningDao;
import com.bri.dao.WarningRuleDao;
import com.bri.vo.DisbVO;
import com.bri.vo.FundPlanVO;
import com.bri.vo.HireRow;
import com.bri.vo.MoMRow;
import com.bri.vo.OutRow;
import com.bri.vo.OverrunRow;
import com.bri.vo.RateRow;
import com.bri.vo.ReportVO;
import com.bri.vo.SourceRow;
import com.bri.vo.StagnantRow;
import com.bri.vo.WarningRuleVO;
import com.bri.vo.WarningVO;
import com.bri.vo.YearRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * B 段控制台自检（B-7 的“现场能跑”）。
 *
 * <p>为什么要有它：界面由 D 做，B 的 DAO 在 D 接进来之前必须能独立证明“逻辑是通的”。
 * 这里按 任务书 2.6 的完成定义逐条打勾：</p>
 * <ol>
 *   <li>资金到位率 / 年度计划 vs 实际 / 来源占比 / 超计划 4 类查询能跑出结果；</li>
 *   <li>同项目同月重复报工被唯一约束挡住，并给出中文提示；</li>
 *   <li>改一条预警规则阈值，刷新后预警列表变化；</li>
 *   <li>种子数据量与任务书要求对齐（≥120 拨付、≥200 报告）。</li>
 * </ol>
 *
 * <p>运行方式（不需要界面）：</p>
 * <pre>
 * mvn -q clean compile
 * java -cp target/classes;target/dependency/mysql-connector-java-8.0.33.jar com.bri.modB.BSelfTest
 * 或打包后：java -cp target/bri-pm.jar com.bri.modB.BSelfTest
 * </pre>
 *
 * @author B
 */
public final class BSelfTest {

    private static final FundDao FUND_DAO = new FundDao();
    private static final FundingSourceDao SOURCE_DAO = new FundingSourceDao();
    private static final ReportDao REPORT_DAO = new ReportDao();
    private static final ReportIndicatorDao INDICATOR_DAO = new ReportIndicatorDao();
    private static final WarningDao WARNING_DAO = new WarningDao();
    private static final WarningRuleDao RULE_DAO = new WarningRuleDao();

    private static int pass;
    private static int fail;

    private BSelfTest() {
    }

    public static void main(String[] args) {
        System.out.println("================ B 段自检（M4 资金 / M6 报告 / M8 预警） ================");
        System.out.println("数据库：" + describeDatabase());

        try {
            checkDataVolume();
            checkFundQueries();
            checkReportQueries();
            checkUniqueConstraint();
            checkWarningRefresh();
            checkThresholdChange();
            checkPagingAndOrderBy();
        } catch (RuntimeException e) {
            fail++;
            System.out.println("[FAIL] 自检中断：" + e.getMessage());
            e.printStackTrace(System.out);
        }

        System.out.println("================================================================");
        System.out.println("通过 " + pass + " 项，失败 " + fail + " 项");
        if (fail > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // 1. 数据量：任务书 2.1 / 2.6 的要求
    // ------------------------------------------------------------------
    private static void checkDataVolume() {
        section("1. 种子数据量");
        long sources = countRows("SELECT COUNT(*) FROM t_funding_source");
        long plans = countRows("SELECT COUNT(*) FROM t_fund_plan");
        long disb = countRows("SELECT COUNT(*) FROM t_fund_disbursement");
        long reports = countRows("SELECT COUNT(*) FROM t_progress_report");
        long indicators = countRows("SELECT COUNT(*) FROM t_report_indicator");
        long rules = countRows("SELECT COUNT(*) FROM t_warning_rule");
        System.out.printf("  资金来源 %d 条 / 年度计划 %d 条 / 拨付 %d 条 / 月报 %d 条 / 指标 %d 条 / 规则 %d 条%n",
                sources, plans, disb, reports, indicators, rules);
        assertTrue("拨付 ≥ 120 条", disb >= 120);
        assertTrue("月报 ≥ 200 条", reports >= 200);
        assertTrue("每个项目都有年度计划（40 个项目 × 3 年 = 120）", plans >= 120);
        assertTrue("预警规则 ≥ 4 条", rules >= 4);
        assertTrue("指标是 EAV，2 个指标 × 月报数", indicators >= reports);
        assertTrue("资金来源覆盖 5 种类型", countRows("SELECT COUNT(DISTINCT source_type) FROM t_funding_source") >= 5);
    }

    // ------------------------------------------------------------------
    // 2. M4 四类查询（Q4-1 / Q4-3 / Q4-4 / Q4-5）
    // ------------------------------------------------------------------
    private static void checkFundQueries() {
        section("2. M4 资金统计（Q4-1 ~ Q4-5）");

        List<RateRow> rates = FUND_DAO.statFundRateByProject();
        assertTrue("Q4-1 到位率查询有结果", !rates.isEmpty());
        RateRow first = rates.get(0);
        System.out.printf("  到位率最低：%s 计划 %s 实拨 %s 到位率 %s%%%n",
                first.getProjectCode(), first.getPlanTotal(), first.getActualTotal(), first.getRatePct());

        List<RateRow> low = FUND_DAO.findLowRateProjects(60);
        assertTrue("Q4-2 到位率 <60% 的项目能筛出来", !low.isEmpty());
        System.out.println("  到位率 <60% 的项目 " + low.size() + " 个，例如："
                + low.get(0).getProjectCode() + "（" + low.get(0).getRatePct() + "%）");

        List<SourceRow> sources = FUND_DAO.statBySource();
        assertTrue("Q4-3 来源占比有结果（窗口函数）", !sources.isEmpty());
        BigDecimal shareSum = BigDecimal.ZERO;
        for (SourceRow row : sources) {
            shareSum = shareSum.add(row.getSharePct());
        }
        System.out.println("  来源占比合计 = " + shareSum + "%（应约等于 100%，验证占比公式没算错）");
        assertTrue("Q4-3 占比合计约等于 100%",
                shareSum.subtract(BigDecimal.valueOf(100)).abs().compareTo(BigDecimal.valueOf(0.2)) <= 0);

        List<YearRow> years = FUND_DAO.statYearPlanVsActual();
        assertTrue("Q4-4 年度计划 vs 实际有结果", years.size() >= 3);
        for (YearRow row : years) {
            System.out.printf("  %d 年：计划 %s / 实拨 %s / 到位率 %s%%%n",
                    row.getPlanYear(), row.getPlanTotal(), row.getActualTotal(), row.getRatePct());
        }

        List<OverrunRow> overruns = FUND_DAO.findOverrunProjects(0);
        System.out.println("  超计划年度（超 0%）共 " + overruns.size() + " 条");
        assertTrue("Q4-5 超计划查询可执行（允许为空）", overruns != null);
        if (!overruns.isEmpty()) {
            OverrunRow row = overruns.get(0);
            System.out.printf("  超计划最高：%s %d 年 计划 %s 实拨 %s 超 %s%%%n",
                    row.getProjectCode(), row.getPlanYear(), row.getPlanAmount(),
                    row.getActualAmount(), row.getOverrunPct());
        }

        List<DisbVO> byDate = FUND_DAO.findDisbByDate(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        assertTrue("拨付按日期区间查询可用", !byDate.isEmpty());
        System.out.println("  2024 年拨付 " + byDate.size() + " 笔");
    }

    // ------------------------------------------------------------------
    // 3. M6 报告查询（Q6-1 ~ Q6-4）
    // ------------------------------------------------------------------
    private static void checkReportQueries() {
        section("3. M6 履约报告（Q6-1 ~ Q6-4）");

        ReportVO latest = REPORT_DAO.findLatest(1L);
        assertTrue("能取到 1 号项目的最新月报", latest != null);
        if (latest != null) {
            System.out.printf("  1 号项目最新月报：%s 进度 %s%% 属地 %d/%d 人%n",
                    latest.getPeriodYm(), latest.getProgressPct(),
                    latest.getLocalStaff(), latest.getOnSiteStaff());
        }

        List<MoMRow> mom = REPORT_DAO.statMonthlyMoM(1L);
        assertTrue("Q6-1 环比有结果（LAG）", mom.size() >= 2);
        MoMRow lastMom = mom.get(mom.size() - 1);
        System.out.printf("  Q6-1 最后一期 %s 进度 %s%%（上一期 %s%%，环比 %s）%n",
                lastMom.getPeriodYm(), lastMom.getProgressPct(),
                lastMom.getPrevPct(), lastMom.getMomDelta());
        assertTrue("Q6-1 第一期环比为 null（LAG 无前值）", mom.get(0).getMomDelta() == null);

        List<OutRow> byCountry = REPORT_DAO.statOutputByCountry();
        assertTrue("Q6-2 国别累计产值有结果", !byCountry.isEmpty());
        System.out.println("  Q6-2 产值第一：" + byCountry.get(0).getCountryName()
                + " 累计 " + byCountry.get(0).getOutputTotal() + " 万元");

        List<StagnantRow> stagnant = REPORT_DAO.findStagnantProjects(3);
        assertTrue("Q6-3 能找出连续 3 个月不涨的项目", !stagnant.isEmpty());
        for (StagnantRow row : stagnant) {
            System.out.println("  " + row);
        }

        List<HireRow> lowHire = REPORT_DAO.findLowLocalHireRate(50);
        assertTrue("Q6-4 属地用工占比 <50% 的项目能筛出来", !lowHire.isEmpty());
        System.out.println("  Q6-4 属地用工偏低 " + lowHire.size() + " 个项目，最低 "
                + lowHire.get(0).getProjectCode() + " " + lowHire.get(0).getLocalHirePct() + "%");

        List<com.bri.vo.IndicatorRow> indicators = INDICATOR_DAO.findIndicatorsByReport(latest == null ? 1L : latest.getReportId());
        assertTrue("EAV 指标能按报告读出", !indicators.isEmpty());
        for (com.bri.vo.IndicatorRow row : indicators) {
            System.out.println("  指标 " + row.getIndicatorName() + " = " + row.getIndicatorVal() + (row.getUnit() == null ? "" : row.getUnit()));
        }
    }

    // ------------------------------------------------------------------
    // 4. 唯一约束：同项目同月重复报工必须被挡住
    // ------------------------------------------------------------------
    private static void checkUniqueConstraint() {
        section("4. 同项目同月重复报工（数据库唯一约束）");
        ReportVO duplicate = new ReportVO();
        duplicate.setProjectId(1L);
        duplicate.setPeriodYm("2025-06");
        duplicate.setProgressPct(BigDecimal.valueOf(88.8));
        duplicate.setOnSiteStaff(100);
        duplicate.setLocalStaff(20);
        duplicate.setOutputValue(BigDecimal.valueOf(1000));
        duplicate.setReportDate(LocalDate.of(2025, 6, 28));
        duplicate.setReporter("自检");

        Result<Long> result = REPORT_DAO.insertReport(duplicate);
        System.out.println("  插入结果：" + result.getMessage());
        assertTrue("重复报工被拒绝（success = false）", !result.isSuccess());
        assertTrue("拒绝原因包含中文提示“已报过”", result.getMessage() != null && result.getMessage().contains("已报过"));

        long shouldNotExist = countRows("SELECT COUNT(*) FROM t_progress_report WHERE project_id = 1 AND period_ym = '2025-06'");
        assertTrue("库中仍然只有 1 条记录（没有被插进去）", shouldNotExist == 1);
    }

    // ------------------------------------------------------------------
    // 5. 预警：按规则表刷新，幂等
    // ------------------------------------------------------------------
    private static void checkWarningRefresh() {
        section("5. M8 预警刷新（规则表 + 幂等）");
        List<WarningRuleVO> rules = RULE_DAO.findEnabled();
        assertTrue("规则表里有启用规则", rules.size() >= 4);
        for (WarningRuleVO rule : rules) {
            System.out.println("  " + rule.getRuleCode() + " " + rule.getRuleName()
                    + " [" + rule.getLevelName() + "] 条件：" + rule.conditionDesc());
        }

        WarningDao.RefreshResult first = WARNING_DAO.refresh();
        System.out.println("  第 1 次刷新：" + first);
        int activeAfterFirst = first.getActive();
        assertTrue("刷新产生了预警（资金断档/资金沉淀样本必须命中）", activeAfterFirst > 0);

        WarningDao.RefreshResult second = WARNING_DAO.refresh();
        System.out.println("  第 2 次刷新：" + second);
        assertTrue("刷新幂等：两次刷新未处理条数一致", second.getActive() == activeAfterFirst);

        Map<String, Integer> byLevel = WARNING_DAO.countOpenByLevel();
        System.out.println("  按级别：" + byLevel);
        assertTrue("红/黄/蓝至少两类有预警", countNonZero(byLevel) >= 2);

        List<WarningVO> list = WARNING_DAO.findByLevel("R", Page.of(1, 10));
        assertTrue("按级别筛选红灯预警有结果", !list.isEmpty());
        System.out.println("  红灯示例：" + list.get(0));

        Result<Integer> ack = WARNING_DAO.ackWarning(list.get(0).getWarnId(), "B");
        assertTrue("确认预警成功", ack.isSuccess() && ack.getData() != null && ack.getData() > 0);
        WarningDao.RefreshResult third = WARNING_DAO.refresh();
        List<WarningVO> afterAck = WARNING_DAO.findByLevel("R", Page.of(1, 50));
        boolean stillAck = false;
        for (WarningVO vo : afterAck) {
            if (vo.getWarnId() == list.get(0).getWarnId() && "ACK".equals(vo.getStatus())) {
                stillAck = true;
            }
        }
        System.out.println("  确认后再刷新：" + third);
        assertTrue("已确认的预警不会被刷新改回未处理", stillAck);
    }

    // ------------------------------------------------------------------
    // 6. 改阈值 → 刷新 → 预警变化（B-5 的核心演示）
    // ------------------------------------------------------------------
    private static void checkThresholdChange() {
        section("6. 改阈值 → 刷新 → 预警变化（加分项演示）");
        WarningRuleVO rule = RULE_DAO.findById(1L);
        if (rule == null) {
            assertTrue("找不到规则 F1", false);
            return;
        }
        BigDecimal original = rule.getCond2Value();
        int before = (int) countRows("SELECT COUNT(*) FROM t_warning w JOIN t_warning_rule r ON r.rule_id = w.rule_id WHERE w.status = 'OPEN' AND r.rule_code = 'F1'");

        // 把 F1 的第二个条件“进度 < 50%”放宽到“进度 < 65%”，条件放宽 → 命中的项目变多
        rule.setCond2Value(BigDecimal.valueOf(65));
        Result<Integer> updated = RULE_DAO.update(rule);
        assertTrue("修改 F1 阈值成功", updated.isSuccess());
        WarningDao.RefreshResult afterRelax = WARNING_DAO.refresh();
        int relaxed = (int) countRows("SELECT COUNT(*) FROM t_warning w JOIN t_warning_rule r ON r.rule_id = w.rule_id WHERE w.status = 'OPEN' AND r.rule_code = 'F1'");
        System.out.println("  F1 进度阈值 50 → 65 后，红灯条数：" + before + " → " + relaxed + "（" + afterRelax + "）");
        assertTrue("条件放宽后 F1 预警条数增加", relaxed > before);

        rule.setCond2Value(original);
        RULE_DAO.update(rule);
        WARNING_DAO.refresh();
        int restored = (int) countRows("SELECT COUNT(*) FROM t_warning w JOIN t_warning_rule r ON r.rule_id = w.rule_id WHERE w.status = 'OPEN' AND r.rule_code = 'F1'");
        System.out.println("  阈值恢复 " + original + " 后，红灯条数回到 " + restored);
        assertTrue("阈值恢复后 F1 预警条数回到原值", restored == before);
    }

    // ------------------------------------------------------------------
    // 7. 分页与排序白名单
    // ------------------------------------------------------------------
    private static void checkPagingAndOrderBy() {
        section("7. 分页与排序白名单");
        Page page = Page.of(1, 5, "disb_date DESC");
        List<DisbVO> page1 = FUND_DAO.findDisbByProject(1L, page);
        assertTrue("分页取到第 1 页（≤5 条）", page1.size() <= 5 && !page1.isEmpty());
        System.out.println("  1 号项目第 1 页 " + page1.size() + " 条，首条 " + page1.get(0).getDisbDate());

        Page page2 = Page.of(2, 5, "disb_date DESC");
        List<DisbVO> second = FUND_DAO.findDisbByProject(1L, page2);
        if (!second.isEmpty() && !page1.isEmpty()) {
            assertTrue("第 2 页与第 1 页不重复", second.get(0).getDisbId() != page1.get(0).getDisbId());
        }

        boolean blocked = false;
        try {
            Page evil = Page.of(1, 5, "disb_amount; DROP TABLE t_fund_disbursement");
            FUND_DAO.findDisbByProject(1L, evil);
        } catch (IllegalArgumentException e) {
            blocked = true;
            System.out.println("  非法排序被拦住：" + e.getMessage());
        }
        assertTrue("排序白名单挡住注入式排序字段", blocked);

        List<FundPlanVO> plans = FUND_DAO.findPlansByProject(1L);
        assertTrue("1 号项目有 3 个年度计划", plans.size() == 3);
        FundingSourceDao sources = SOURCE_DAO;
        assertTrue("资金来源下拉可读", !sources.findAll().isEmpty());
    }

    // ------------------------------------------------------------------
    // 工具方法
    // ------------------------------------------------------------------
    private static String describeDatabase() {
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT DATABASE(), VERSION()");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1) + " / MySQL " + rs.getString(2);
            }
            return "未知";
        } catch (Exception e) {
            return "连接失败：" + e.getMessage();
        }
    }

    private static long countRows(String sql) {
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (Exception e) {
            throw new IllegalStateException("统计失败：" + sql + " → " + e.getMessage(), e);
        }
    }

    private static int countNonZero(Map<String, Integer> map) {
        int n = 0;
        for (Integer value : map.values()) {
            if (value != null && value > 0) {
                n++;
            }
        }
        return n;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("---- " + title + " ----");
    }

    private static void assertTrue(String what, boolean condition) {
        if (condition) {
            pass++;
            System.out.println("  [PASS] " + what);
        } else {
            fail++;
            System.out.println("  [FAIL] " + what);
        }
    }
}
