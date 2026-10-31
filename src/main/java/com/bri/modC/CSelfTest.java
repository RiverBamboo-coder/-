package com.bri.modC;

import com.bri.common.DbUtil;
import com.bri.common.Result;
import com.bri.dao.CommitmentDao;
import com.bri.dao.MilestoneDao;
import com.bri.dao.OutcomeDao;
import com.bri.vo.AchievementRateRow;
import com.bri.vo.CommitRateRow;
import com.bri.vo.CommitVO;
import com.bri.vo.CompareVO;
import com.bri.vo.ContraRow;
import com.bri.vo.DelayRow;
import com.bri.vo.DurationRow;
import com.bri.vo.GreenRow;
import com.bri.vo.HireRow;
import com.bri.vo.MilestoneVO;
import com.bri.vo.OutcomeVO;
import com.bri.vo.ReconRow;
import com.bri.vo.RegionOutRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * C 段控制台自检（M5 里程碑 / M7 履约承诺比对 / M9 成果与思政报表的“现场能跑”）。
 *
 * <p>为什么要有它：界面由 D 做，C 的 DAO 在 D 接进来之前必须能独立证明“逻辑是通的”。
 * 这里按任务书 2.6 的完成定义逐条打勾：</p>
 * <ol>
 *   <li>种子数据量与任务书对齐（里程碑 ≥150、承诺 ≥80、成效 40，且故意制造了 ≥8 条待评估）；</li>
 *   <li>M5 里程碑四类查询（平均超期/超期未完成/工期对比/领域达成率）能跑出结果；</li>
 *   <li>M7 承诺比对 {@code calcAll()} 能算满全表，且“待评估”由 NULL 正确触发；</li>
 *   <li>M9 成果详情 + 三条思政查询能跑出结果；</li>
 *   <li>两个写操作（补排节点、补录实际日期）走通，生成列 {@code is_delayed} 自动生效。</li>
 * </ol>
 *
 * <p>运行方式（不需要界面）：</p>
 * <pre>
 * mvn -q clean compile
 * java -cp target/classes;target/dependency/mysql-connector-java-8.0.33.jar com.bri.modC.CSelfTest
 * 或打包后：java -cp target/bri-pm.jar com.bri.modC.CSelfTest
 * </pre>
 *
 * <p>注意：第 2 段会真的补排/补录一条里程碑（幂等，可重复执行）；要恢复原始种子请重跑
 * {@code sql/04_seed_business.sql} 的 C 段。</p>
 *
 * @author C
 */
public final class CSelfTest {

    private static final MilestoneDao MILESTONE_DAO = new MilestoneDao();
    private static final CommitmentDao COMMITMENT_DAO = new CommitmentDao();
    private static final OutcomeDao OUTCOME_DAO = new OutcomeDao();

    private static int pass;
    private static int fail;

    private CSelfTest() {
    }

    public static void main(String[] args) {
        System.out.println("================ C 段自检（M5 里程碑 / M7 履约承诺 / M9 成效思政） ================");
        System.out.println("数据库：" + describeDatabase());

        try {
            checkDataVolume();
            checkMilestoneQueries();
            checkMilestoneWrites();
            checkCommitmentCalc();
            checkCommitmentQueries();
            checkOutcomeQueries();
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
    // 1. 数据量：任务书 2.1 / 2.6 的要求（C 段 04 种子）
    // ------------------------------------------------------------------
    private static void checkDataVolume() {
        section("1. 种子数据量");
        long dict = countRows("SELECT COUNT(*) FROM t_milestone");
        long dictOk = countRows("SELECT COUNT(*) FROM t_milestone WHERE milestone_name IN "
                + "('开工','主体结构','设备安装','带电通车','竣工移交','试运行')");
        long hasOpen = countRows("SELECT COUNT(*) FROM t_milestone WHERE milestone_name = '开通运营'");
        long milestone = countRows("SELECT COUNT(*) FROM t_project_milestone");
        long milestoneNull = countRows("SELECT COUNT(*) FROM t_project_milestone WHERE actual_date IS NULL");
        long milestoneOverdue = countRows("SELECT COUNT(*) FROM t_project_milestone WHERE is_delayed = 1");
        long commitment = countRows("SELECT COUNT(*) FROM t_commitment");
        long pendingBase = countRows("SELECT COUNT(*) FROM t_commitment WHERE base_date IS NULL");
        long outcome = countRows("SELECT COUNT(*) FROM t_project_outcome");

        System.out.printf("  里程碑字典 %d 条 / 项目里程碑 %d 条 / 履约承诺 %d 条 / 项目成效 %d 条%n",
                dict, milestone, commitment, outcome);
        System.out.printf("  未完成(actual_date 空) %d 条 / 已超期(is_delayed=1) %d 条 / 待评估候选(base_date 空) %d 条%n",
                milestoneNull, milestoneOverdue, pendingBase);

        assertTrue("里程碑字典 6 条，名称含 竣工移交/试运行", dict == 6 && dictOk == 6);
        assertTrue("里程碑字典不含废弃名称 开通运营", hasOpen == 0);
        assertTrue("项目里程碑 ≥150 条（40 项目 × 4 节点）", milestone >= 150);
        assertTrue("至少 20 条里程碑未完成（actual_date 为空）", milestoneNull >= 20);
        assertTrue("至少 15 条里程碑已超期（is_delayed=1）", milestoneOverdue >= 15);
        assertTrue("履约承诺 ≥80 条（四类：工期/金额/属地用工/培训）", commitment >= 80);
        assertTrue("至少 8 条承诺故意造成待评估（base_date 为空）", pendingBase >= 8);
        assertTrue("项目成效与项目 1:1，共 40 条", outcome == 40);
    }

    // ------------------------------------------------------------------
    // 2. M5 里程碑四类查询（Q5-1 ~ Q5-4）
    // ------------------------------------------------------------------
    private static void checkMilestoneQueries() {
        section("2. M5 里程碑（Q5-1 ~ Q5-4）");

        List<MilestoneVO> p1 = MILESTONE_DAO.findMilestonesByProject(1L);
        assertTrue("1 号项目能列出里程碑（seed 为 4 个节点）", p1 != null && p1.size() >= 4);
        if (!p1.isEmpty()) {
            MilestoneVO first = p1.get(0);
            System.out.printf("  1 号项目首个节点：%s（%s）计划 %s 实际 %s 超期 %s%n",
                    first.getMilestoneName(), first.getMilestoneCode(),
                    first.getPlanDate(), first.getActualDate(), first.getIsDelayed());
        }

        List<DelayRow> avgDelay = MILESTONE_DAO.statAvgDelayByMilestone();
        assertTrue("Q5-1 平均超期天数有结果", !avgDelay.isEmpty());
        for (DelayRow row : avgDelay) {
            System.out.printf("  Q5-1 %s：平均超期 %s 天（%d/%d 超期）%n",
                    row.getMilestoneName(), row.getAvgDelayDays(), row.getOverdueCnt(), row.getTotalCnt());
        }

        List<DelayRow> overdue = MILESTONE_DAO.findOverdueMilestones();
        assertTrue("Q5-2 已超期未完成里程碑能筛出来", !overdue.isEmpty());
        System.out.println("  Q5-2 已超期未完成 " + overdue.size() + " 条，例如："
                + overdue.get(0).getProjectCode() + " " + overdue.get(0).getMilestoneName()
                + "（超期 " + overdue.get(0).getOverdueDays() + " 天）");

        List<DurationRow> duration = MILESTONE_DAO.compareProjectDuration();
        assertTrue("Q5-3 计划 vs 实际工期对比有结果（竣工/已移交 14 个项目）", !duration.isEmpty());
        DurationRow firstDur = duration.get(0);
        System.out.printf("  Q5-3 工期差最大：%s 计划 %d 天 / 实际 %d 天 / 差 %d 天%n",
                firstDur.getProjectCode(), firstDur.getPlannedDays(),
                firstDur.getActualDays(), firstDur.getDiffDays());

        List<AchievementRateRow> byField = MILESTONE_DAO.statMilestoneRateByField();
        assertTrue("Q5-4 各领域里程碑达成率有结果", !byField.isEmpty());
        for (AchievementRateRow row : byField) {
            System.out.printf("  Q5-4 %s：达成 %d/%d（%s%%）%n",
                    row.getFieldName(), row.getAchievedCnt(), row.getTotalCnt(), row.getRatePct());
        }
    }

    // ------------------------------------------------------------------
    // 3. M5 两个写操作：补排节点 + 补录实际日期（演示生成列 is_delayed）
    // ------------------------------------------------------------------
    private static void checkMilestoneWrites() {
        section("3. M5 写操作（补排节点 / 补录实际日期）");

        // 3.1 upsert 补排“竣工移交”节点（seq5 不在种子 4 节点内，演示新增节点能力）
        MilestoneVO add = new MilestoneVO();
        add.setProjectId(1L);
        add.setMilestoneId(5);                      // 竣工移交
        add.setPlanDate(LocalDate.of(2017, 8, 1));
        add.setRemark("自检补排");
        Result<Long> upsert = MILESTONE_DAO.upsertMilestonePlan(add);
        assertTrue("补排“竣工移交”节点成功（幂等）", upsert.isSuccess());

        List<MilestoneVO> afterUpsert = MILESTONE_DAO.findMilestonesByProject(1L);
        boolean hasTurnover = false;
        for (MilestoneVO vo : afterUpsert) {
            if (vo.getMilestoneId() == 5) {
                hasTurnover = true;
            }
        }
        assertTrue("1 号项目现在包含“竣工移交”节点", hasTurnover);

        // 3.2 补录“设备安装”实际日期（晚于计划 → 生成列 is_delayed 自动变 true）
        MilestoneVO equip = null;
        for (MilestoneVO vo : afterUpsert) {
            if (vo.getMilestoneId() == 3) {
                equip = vo;
            }
        }
        assertTrue("1 号项目有“设备安装”节点", equip != null);
        if (equip != null) {
            LocalDate late = equip.getPlanDate().plusDays(15);
            Result<Void> rec = MILESTONE_DAO.recordActualByType(1L, 3, late);
            assertTrue("补录实际完成日期成功", rec.isSuccess());
            for (MilestoneVO vo : MILESTONE_DAO.findMilestonesByProject(1L)) {
                if (vo.getMilestoneId() == 3) {
                    System.out.printf("  补录后：actual_date=%s, is_delayed=%s%n",
                            vo.getActualDate(), vo.getIsDelayed());
                    assertTrue("补录后 actual_date 已写入", vo.getActualDate() != null);
                    assertTrue("晚于计划日 → is_delayed 生成列为 true", Boolean.TRUE.equals(vo.getIsDelayed()));
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 4. M7 履约承诺比对：calcAll 算满全表 + 三值逻辑
    // ------------------------------------------------------------------
    private static void checkCommitmentCalc() {
        section("4. M7 承诺比对 calcAll（三值逻辑）");

        long commitments = countRows("SELECT COUNT(*) FROM t_commitment");
        int resultCount = COMMITMENT_DAO.calcAll();
        System.out.println("  calcAll() 写入比对结果 " + resultCount + " 条（承诺 " + commitments + " 条）");
        assertTrue("每条承诺都算出一条比对结果", resultCount == commitments);

        long fulfilled = countRows("SELECT COUNT(*) FROM t_commitment_result WHERE commit_status = '已履约'");
        long pending = countRows("SELECT COUNT(*) FROM t_commitment_result WHERE commit_status = '待评估'");
        long partial = countRows("SELECT COUNT(*) FROM t_commitment_result WHERE commit_status = '部分履约'");
        long breached = countRows("SELECT COUNT(*) FROM t_commitment_result WHERE commit_status = '未履约'");
        System.out.printf("  结论分布：已履约 %d / 部分履约 %d / 未履约 %d / 待评估 %d%n",
                fulfilled, partial, breached, pending);
        assertTrue("待评估 ≥8 条（NULL 首判，未拨付/未填报返回 NULL 而不是 0）", pending >= 8);
        assertTrue("至少有已履约结论（工期类承诺可算出实际值）", fulfilled >= 1);
    }

    // ------------------------------------------------------------------
    // 5. M7 其余查询（Q7-1 ~ Q7-4）
    // ------------------------------------------------------------------
    private static void checkCommitmentQueries() {
        section("5. M7 履约承诺查询（Q7-1 ~ Q7-4）");

        // 项目 3（在建）应具备四类承诺：工期/金额/属地用工/培训
        List<CommitVO> commits = COMMITMENT_DAO.findCommitments(3L);
        assertTrue("3 号项目（在建）有 4 类承诺", commits != null && commits.size() == 4);
        for (CommitVO vo : commits) {
            System.out.printf("  %s：目标 %s（%s）实际 %s → %s%n",
                    vo.getCommitType(), vo.getTargetValue(), vo.getCompareOp(),
                    vo.getActualValue(), vo.getCommitStatus());
        }

        List<CompareVO> pending = COMMITMENT_DAO.findByStatus("待评估");
        assertTrue("按“待评估”筛选有结果", !pending.isEmpty());
        System.out.println("  “待评估”共 " + pending.size() + " 条，例如："
                + pending.get(0).getProjectCode() + " " + pending.get(0).getCommitType());

        List<CommitRateRow> rates = COMMITMENT_DAO.statRateByType();
        assertTrue("Q7-2 按承诺类型履约率有 4 行", rates.size() == 4);
        for (CommitRateRow row : rates) {
            System.out.printf("  Q7-2 %s：履约 %d/%d（%s%%）%n",
                    row.getCommitType(), row.getFulfilledCnt(), row.getTotalCnt(), row.getRatePct());
        }

        List<ContraRow> contra = COMMITMENT_DAO.findCrossContradiction();
        assertTrue("Q7-3 交叉矛盾查询可执行", contra != null);
        System.out.println("  Q7-3 交叉矛盾项目 " + contra.size() + " 个（同项目既有已履约又有未履约）");

        List<ReconRow> recon = COMMITMENT_DAO.statCountryReconcile();
        assertTrue("Q7-4 国别履约对账有结果", !recon.isEmpty());
        System.out.println("  Q7-4 国别对账第一：" + recon.get(0).getCountryName()
                + " 承诺 " + recon.get(0).getCommitCnt() + " 条，履约率 " + recon.get(0).getRatePct() + "%");
    }

    // ------------------------------------------------------------------
    // 6. M9 成果详情 + 三条思政查询
    // ------------------------------------------------------------------
    private static void checkOutcomeQueries() {
        section("6. M9 成果与思政（三条思政查询）");

        OutcomeVO outcome = OUTCOME_DAO.findByProject(1L);
        assertTrue("1 号项目成效详情可取", outcome != null);
        if (outcome != null) {
            System.out.printf("  1 号项目成效：受益 %s 千人 / 就业 %s 岗 / 培训 %s 人次 / 绿色装机 %s MW%n",
                    outcome.getBeneficiaryK(), outcome.getJobsCreated(),
                    outcome.getTrainingPpl(), outcome.getGreenMw());
        }

        List<RegionOutRow> byRegion = OUTCOME_DAO.statByRegion();
        assertTrue("思政 1 共商共建共享（大洲汇总）有结果", !byRegion.isEmpty());
        System.out.println("  思政 1 大洲汇总第一：" + byRegion.get(0).getRegionName()
                + " 项目 " + byRegion.get(0).getProjCnt() + " 个，受益 " + byRegion.get(0).getBenefitK() + " 千人");

        List<HireRow> hire = OUTCOME_DAO.findHighLocalHire(60, 1000);
        assertTrue("思政 2 授人以渔（属地用工+培训双达标）可执行", hire != null);
        System.out.println("  思政 2 双达标项目 " + hire.size() + " 个"
                + "（依赖 B 的 t_progress_report，B 数据入库后此数才会变多）");

        List<GreenRow> green = OUTCOME_DAO.statGreenByField();
        assertTrue("思政 3 绿色丝绸之路（按领域绿色装机）有结果", !green.isEmpty());
        for (GreenRow row : green) {
            System.out.printf("  思政 3 %s：绿色装机 %s MW / 输出标准 %d 项%n",
                    row.getFieldName(), row.getGreenMw(), row.getStandards());
        }
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
        } catch (SQLException e) {
            throw new IllegalStateException("统计失败：" + sql + " → " + e.getMessage(), e);
        }
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
