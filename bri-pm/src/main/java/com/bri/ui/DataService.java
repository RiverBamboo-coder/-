package com.bri.ui;

import com.bri.common.DataAccessException;
import com.bri.common.Page;
import com.bri.common.Result;
import com.bri.common.TextUtil;
import com.bri.dao.CommitmentDao;
import com.bri.dao.CountryDao;
import com.bri.dao.DictDao;
import com.bri.dao.EnterpriseDao;
import com.bri.dao.FundDao;
import com.bri.dao.MetaDao;
import com.bri.dao.MilestoneDao;
import com.bri.dao.OutcomeDao;
import com.bri.dao.ProjectDao;
import com.bri.dao.ReportDao;
import com.bri.dao.WarningDao;
import com.bri.dao.WarningRuleDao;
import com.bri.vo.AchievementRateRow;
import com.bri.vo.CommitRateRow;
import com.bri.vo.CommitVO;
import com.bri.vo.CompareVO;
import com.bri.vo.ContinentSpreadRow;
import com.bri.vo.ContraRow;
import com.bri.vo.CountryVO;
import com.bri.vo.DelayRow;
import com.bri.vo.DictItem;
import com.bri.vo.DurationRow;
import com.bri.vo.EnterpriseVO;
import com.bri.vo.GapRow;
import com.bri.vo.GreenRow;
import com.bri.vo.HireRow;
import com.bri.vo.MetaColumnVO;
import com.bri.vo.MetaConstraintVO;
import com.bri.vo.MetaTableVO;
import com.bri.vo.MilestoneVO;
import com.bri.vo.MoMRow;
import com.bri.vo.OutRow;
import com.bri.vo.OutcomeVO;
import com.bri.vo.OverdueProjectRow;
import com.bri.vo.ProjectQuery;
import com.bri.vo.ProjectVO;
import com.bri.vo.RankRow;
import com.bri.vo.RateRow;
import com.bri.vo.ReconRow;
import com.bri.vo.RegionOutRow;
import com.bri.vo.ShareRow;
import com.bri.vo.SourceRow;
import com.bri.vo.StagnantRow;
import com.bri.vo.StatRow;
import com.bri.vo.WarningRuleVO;
import com.bri.vo.WarningVO;
import com.bri.vo.YearRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * UI-facing facade over the team's DAOs.
 *
 * <p>The Swing pages never build SQL. They call this service, and this service
 * calls the DAO that owns the module. If MySQL is unavailable, the service
 * returns a small offline table so the interface remains demonstrable.</p>
 */
public final class DataService {
    private final boolean demoMode;
    private boolean usingFallback;
    private String lastError;

    private final CountryDao countryDao = new CountryDao();
    private final ProjectDao projectDao = new ProjectDao();
    private final EnterpriseDao enterpriseDao = new EnterpriseDao();
    private final FundDao fundDao = new FundDao();
    private final ReportDao reportDao = new ReportDao();
    private final WarningDao warningDao = new WarningDao();
    private final WarningRuleDao warningRuleDao = new WarningRuleDao();
    private final MilestoneDao milestoneDao = new MilestoneDao();
    private final CommitmentDao commitmentDao = new CommitmentDao();
    private final OutcomeDao outcomeDao = new OutcomeDao();
    private final MetaDao metaDao = new MetaDao();
    private final DictDao dictDao = new DictDao();

    public DataService(boolean demoMode) {
        this.demoMode = demoMode;
        this.usingFallback = demoMode;
        this.lastError = demoMode ? "演示模式" : null;
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    public boolean isUsingFallback() {
        return usingFallback;
    }

    public String getLastError() {
        return lastError;
    }

    public TableData countries() {
        return load("m1_countries", () -> {
            List<CountryVO> list = countryDao.findAll(Page.of(1, 200, "country_code"));
            return table(new String[]{"代码", "国家/地区", "大洲", "区域", "共建", "项目数", "投资额"},
                    list, c -> new Object[]{
                            c.getCountryCode(), c.getCountryName(), nz(c.getContinentName()),
                            nz(c.getRegionName()), c.isBeltRoad() ? "是" : "否",
                            c.getProjectCount(), amount(c.getInvestTotal())
                    });
        });
    }

    public TableData regionStats() {
        return load("m1_regions", () -> {
            List<StatRow> list = countryDao.statProjectsByRegion();
            return table(new String[]{"区域", "项目数", "投资额"},
                    list, r -> new Object[]{nz(r.getLabel()), r.getCnt(), amount(r.getTotal())});
        });
    }

    public TableData countriesWithoutProject() {
        return load("m1_without_project", () -> {
            List<CountryVO> list = countryDao.findCountriesWithoutProject();
            return table(new String[]{"代码", "国家/地区", "大洲", "区域"},
                    list, c -> new Object[]{
                            c.getCountryCode(), c.getCountryName(),
                            nz(c.getContinentName()), nz(c.getRegionName())
                    });
        });
    }

    public TableData projects(String keyword) {
        return load("m2_projects", () -> {
            ProjectQuery query = new ProjectQuery();
            if (TextUtil.isNotBlank(keyword)) {
                query.setKeyword(keyword.trim());
            }
            List<ProjectVO> list = projectDao.findByCondition(query, Page.of(1, 200, "sign_date DESC"));
            return table(new String[]{"编号", "项目名称", "国家", "领域", "模式", "状态", "投资额", "签约日期"},
                    list, p -> new Object[]{
                            p.getProjectCode(), p.getProjectName(), nz(p.getCountryName()),
                            nz(p.getFieldName()), nz(p.getModeName()), nz(p.getStatus()),
                            amount(p.getInvestAmount()), date(p.getSignDate())
                    });
        });
    }

    public TableData contractGaps() {
        return load("m2_gaps", () -> {
            List<GapRow> list = projectDao.findContractGapProjects();
            return table(new String[]{"编号", "项目", "投资额", "合同额", "差额", "差额比例"},
                    list, g -> new Object[]{
                            g.getProjectCode(), g.getProjectName(), amount(g.getInvestAmount()),
                            amount(g.getContractSum()), amount(g.getGap()), pct(g.getGapPct())
                    });
        });
    }

    public TableData projectYearly() {
        return load("m2_yearly", () -> {
            List<StatRow> list = projectDao.statNewProjectsByYear();
            return table(new String[]{"年份", "新签项目数", "投资额"},
                    list, r -> new Object[]{nz(r.getLabel()), r.getCnt(), amount(r.getTotal())});
        });
    }

    public TableData overdueProjects() {
        return load("m2_overdue", () -> {
            List<OverdueProjectRow> list = projectDao.findOverdueNotTransferred();
            return table(new String[]{"编号", "项目", "国家", "状态", "计划竣工", "逾期天数"},
                    list, r -> new Object[]{
                            r.getProjectCode(), r.getProjectName(), nz(r.getCountryName()),
                            nz(r.getStatus()), date(r.getPlannedEndDate()), r.getOverdueDays()
                    });
        });
    }

    public TableData enterprises() {
        return load("m3_enterprises", () -> {
            List<EnterpriseVO> list = enterpriseDao.findAll(Page.of(1, 200, "project_count DESC"));
            return table(new String[]{"企业", "性质", "国家", "项目数", "备注"},
                    list, e -> new Object[]{
                            e.getEnterpriseName(), nz(e.getNatureName()), nz(e.getCountryName()),
                            e.getProjectCount(), nz(e.getRemark())
                    });
        });
    }

    public TableData enterpriseRanking() {
        return load("m3_ranking", () -> {
            List<RankRow> list = enterpriseDao.statEnterpriseRanking(10);
            return table(new String[]{"排名", "企业", "性质", "项目数", "总包金额"},
                    list, r -> new Object[]{
                            r.getRankNo(), r.getEnterpriseName(), nz(r.getNatureName()),
                            r.getProjectCount(), amount(r.getZcAmount())
                    });
        });
    }

    public TableData consortiumShare() {
        return load("m3_consortium", () -> {
            List<ShareRow> list = enterpriseDao.statConsortiumShare();
            return table(new String[]{"联合体", "牵头单位", "成员数", "份额合计", "项目数"},
                    list, r -> new Object[]{
                            r.getConsortiumName(), nz(r.getLeaderName()), r.getMemberCount(),
                            pct(r.getShareSum()), r.getProjectCount()
                    });
        });
    }

    public TableData crossContinentEnterprises() {
        return load("m3_cross", () -> {
            List<ContinentSpreadRow> list = enterpriseDao.statEnterprisesAcrossContinents(3);
            return table(new String[]{"企业", "性质", "洲数", "国家数", "项目数", "洲名"},
                    list, r -> new Object[]{
                            r.getEnterpriseName(), nz(r.getNatureName()), r.getContinentCount(),
                            r.getCountryCount(), r.getProjectCount(), nz(r.getContinentNames())
                    });
        });
    }

    public TableData fundRates() {
        return load("m4_rates", () -> {
            List<RateRow> list = fundDao.statFundRateByProject();
            return table(new String[]{"编号", "项目", "状态", "计划金额", "实际拨付", "到位率"},
                    list, r -> new Object[]{
                            r.getProjectCode(), r.getProjectName(), nz(r.getStatus()),
                            amount(r.getPlanTotal()), amount(r.getActualTotal()), pct(r.getRatePct())
                    });
        });
    }

    public TableData lowFundRates() {
        return load("m4_low", () -> {
            List<RateRow> list = fundDao.findLowRateProjects(60);
            return table(new String[]{"编号", "项目", "计划金额", "实际拨付", "到位率"},
                    list, r -> new Object[]{
                            r.getProjectCode(), r.getProjectName(),
                            amount(r.getPlanTotal()), amount(r.getActualTotal()), pct(r.getRatePct())
                    });
        });
    }

    public TableData fundSources() {
        return load("m4_sources", () -> {
            List<SourceRow> list = fundDao.statBySource();
            return table(new String[]{"排名", "来源", "类型", "实际金额", "占比"},
                    list, r -> new Object[]{
                            r.getRankNo(), r.getSourceName(), nz(r.getSourceType()),
                            amount(r.getActualTotal()), pct(r.getSharePct())
                    });
        });
    }

    public TableData fundYearCompare() {
        return load("m4_years", () -> {
            List<YearRow> list = fundDao.statYearPlanVsActual();
            return table(new String[]{"年份", "计划金额", "实际拨付", "到位率", "项目数"},
                    list, r -> new Object[]{
                            r.getPlanYear(), amount(r.getPlanTotal()), amount(r.getActualTotal()),
                            pct(r.getRatePct()), r.getProjectCnt()
                    });
        });
    }

    public TableData milestones(String projectId) {
        return load("m5_milestones", () -> {
            long id = parseLong(projectId, 1);
            List<MilestoneVO> list = milestoneDao.findMilestonesByProject(id);
            return table(new String[]{"里程碑", "计划日期", "实际日期", "是否超期", "备注"},
                    list, r -> new Object[]{
                            r.getMilestoneName(), date(r.getPlanDate()), date(r.getActualDate()),
                            delayedText(r.getIsDelayed()), nz(r.getRemark())
                    });
        });
    }

    public TableData overdueMilestones() {
        return load("m5_overdue", () -> {
            List<DelayRow> list = milestoneDao.findOverdueMilestones();
            return table(new String[]{"项目", "里程碑", "计划日期", "超期天数"},
                    list, r -> new Object[]{
                            nz(r.getProjectName()), nz(r.getMilestoneName()),
                            date(r.getPlanDate()), r.getOverdueDays()
                    });
        });
    }

    public TableData avgMilestoneDelay() {
        return load("m5_avg", () -> {
            List<DelayRow> list = milestoneDao.statAvgDelayByMilestone();
            return table(new String[]{"里程碑", "平均超期(天)", "超期数", "总数"},
                    list, r -> new Object[]{
                            nz(r.getMilestoneName()), decimal(r.getAvgDelayDays()),
                            r.getOverdueCnt(), r.getTotalCnt()
                    });
        });
    }

    public TableData durationCompare() {
        return load("m5_duration", () -> {
            List<DurationRow> list = milestoneDao.compareProjectDuration();
            return table(new String[]{"编号", "项目", "计划开工", "计划竣工", "实际开工", "实际竣工", "计划天数", "实际天数", "偏差"},
                    list, r -> new Object[]{
                            r.getProjectCode(), r.getProjectName(),
                            date(r.getPlannedStartDate()), date(r.getPlannedEndDate()),
                            date(r.getActualStartDate()), date(r.getActualEndDate()),
                            r.getPlannedDays(), r.getActualDays(), r.getDiffDays()
                    });
        });
    }

    public TableData warnings(String level) {
        return load("m6_warnings", () -> {
            String lvl = TextUtil.isBlank(level) || "全部".equals(level) ? "ALL" : level.trim();
            List<WarningVO> list = warningDao.findByLevel(lvl, Page.of(1, 200));
            return table(new String[]{"预警编号", "项目", "规则", "级别", "状态", "触发值", "时间"},
                    list, r -> new Object[]{
                            r.getWarnId(), nz(r.getProjectName()), nz(r.getRuleName()),
                            nz(r.getLevelName()), nz(r.getStatusName()),
                            nz(r.getTriggerSnapshot()), datetime(r.getCreatedAt())
                    });
        });
    }

    public TableData warningRules() {
        return load("m6_rules", () -> {
            List<WarningRuleVO> list = warningRuleDao.findAll();
            return table(new String[]{"规则编号", "规则名称", "级别", "条件1", "条件2", "启用"},
                    list, r -> new Object[]{
                            r.getRuleCode(), r.getRuleName(), nz(r.getLevelName()),
                            condition(r.getCond1Op(), r.getCond1Value()),
                            condition(r.getCond2Op(), r.getCond2Value()),
                            r.isEnabled() ? "是" : "否"
                    });
        });
    }

    public TableData commitmentsByStatus(String status) {
        return load("m7_commitments", () -> {
            String value = TextUtil.defaultIfBlank(status, "待评估");
            List<CompareVO> list = commitmentDao.findByStatus(value);
            return table(new String[]{"项目编号", "项目", "承诺类型", "目标值", "实际值", "偏差", "结论"},
                    list, r -> new Object[]{
                            r.getProjectCode(), r.getProjectName(), r.getCommitType(),
                            decimal(r.getTargetValue()), decimal(r.getActualValue()),
                            decimal(r.getGapValue()), nz(r.getCommitStatus())
                    });
        });
    }

    public TableData commitRates() {
        return load("m7_rates", () -> {
            List<CommitRateRow> list = commitmentDao.statRateByType();
            return table(new String[]{"承诺类型", "总数", "已履约", "履约率"},
                    list, r -> new Object[]{
                            r.getCommitType(), r.getTotalCnt(), r.getFulfilledCnt(), pct(r.getRatePct())
                    });
        });
    }

    public TableData commitContradictions() {
        return load("m7_contra", () -> {
            List<ContraRow> list = commitmentDao.findCrossContradiction();
            return table(new String[]{"项目编号", "项目", "承诺数", "已履约", "未履约", "部分履约"},
                    list, r -> new Object[]{
                            r.getProjectCode(), r.getProjectName(), r.getTotalCnt(),
                            r.getFulfilledCnt(), r.getBreachedCnt(), r.getPartialCnt()
                    });
        });
    }

    public TableData countryReconcile() {
        return load("m7_reconcile", () -> {
            List<ReconRow> list = commitmentDao.statCountryReconcile();
            return table(new String[]{"国家代码", "国家", "承诺数", "已履约", "未履约", "待评估", "履约率"},
                    list, r -> new Object[]{
                            r.getCountryCode(), r.getCountryName(), r.getCommitCnt(),
                            r.getFulfilledCnt(), r.getBreachedCnt(), r.getPendingCnt(), pct(r.getRatePct())
                    });
        });
    }

    public TableData monthlyMom(String projectId) {
        return load("m8_mom", () -> {
            long id = parseLong(projectId, 1);
            List<MoMRow> list = reportDao.statMonthlyMoM(id);
            return table(new String[]{"项目编号", "报告期", "本期进度", "上期进度", "环比"},
                    list, r -> new Object[]{
                            r.getProjectCode(), r.getPeriodYm(), pct(r.getProgressPct()),
                            pct(r.getPrevPct()), decimal(r.getMomDelta())
                    });
        });
    }

    public TableData outputByCountry() {
        return load("m8_output", () -> {
            List<OutRow> list = reportDao.statOutputByCountry();
            return table(new String[]{"国家代码", "国家", "项目数", "产出总额", "平均进度"},
                    list, r -> new Object[]{
                            r.getCountryCode(), r.getCountryName(), r.getProjectCnt(),
                            amount(r.getOutputTotal()), pct(r.getAvgProgress())
                    });
        });
    }

    public TableData stagnantProjects() {
        return load("m8_stagnant", () -> {
            List<StagnantRow> list = reportDao.findStagnantProjects(3);
            return table(new String[]{"项目编号", "项目", "起始期", "结束期", "停滞月数", "进度"},
                    list, r -> new Object[]{
                            r.getProjectCode(), r.getProjectName(), r.getStartYm(),
                            r.getEndYm(), r.getMonths(), pct(r.getProgressPct())
                    });
        });
    }

    public TableData lowLocalHire() {
        return load("m8_hire", () -> {
            List<HireRow> list = reportDao.findLowLocalHireRate(50);
            return hireTable(list);
        });
    }

    public TableData outcomeByRegion() {
        return load("m9_region", () -> {
            List<RegionOutRow> list = outcomeDao.statByRegion();
            return table(new String[]{"大洲", "项目数", "受益人口(万)", "就业岗位"},
                    list, r -> new Object[]{
                            r.getRegionName(), r.getProjCnt(), decimal(r.getBenefitK()), r.getJobs()
                    });
        });
    }

    public TableData highLocalHire() {
        return load("m9_hire", () -> {
            List<HireRow> list = outcomeDao.findHighLocalHire(70, 1000);
            return hireTable(list);
        });
    }

    public TableData greenByField() {
        return load("m9_green", () -> {
            List<GreenRow> list = outcomeDao.statGreenByField();
            return table(new String[]{"领域", "绿色装机(MW)", "标准输出"},
                    list, r -> new Object[]{
                            r.getFieldName(), decimal(r.getGreenMw()), r.getStandards()
                    });
        });
    }

    public TableData projectOutcome(String projectId) {
        return load("m9_project", () -> {
            long id = parseLong(projectId, 1);
            OutcomeVO value = outcomeDao.findByProject(id);
            if (value == null) {
                return TableData.empty("项目ID", "受益人口(万)", "就业岗位", "培训人次",
                        "绿色装机(MW)", "标准输出", "更新日期");
            }
            return new TableData(
                    new String[]{"项目ID", "受益人口(万)", "就业岗位", "培训人次", "绿色装机(MW)", "标准输出", "更新日期"},
                    new Object[][]{{
                            value.getProjectId(), decimal(value.getBeneficiaryK()), value.getJobsCreated(),
                            value.getTrainingPpl(), decimal(value.getGreenMw()), value.getStandardsOut(),
                            date(value.getUpdateDate())
                    }}
            );
        });
    }

    public TableData dictItems(String typeCode) {
        return load("x4_dict", () -> {
            String type = TextUtil.defaultIfBlank(typeCode, "PROJECT_STATUS");
            List<DictItem> list = dictDao.listByType(type);
            return table(new String[]{"类型", "编码", "名称", "排序", "启用"},
                    list, d -> new Object[]{
                            d.getTypeCode(), d.getItemCode(), d.getItemName(),
                            d.getSortNo(), d.isEnabled() ? "是" : "否"
                    });
        });
    }

    public TableData metaTables() {
        return load("x4_tables", () -> {
            List<MetaTableVO> list = metaDao.listTables();
            return table(new String[]{"表名", "注释", "行数"},
                    list, t -> new Object[]{t.getTableName(), nz(t.getTableComment()), t.getRowCount()});
        });
    }

    public TableData metaColumns(String tableName) {
        return load("x4_columns", () -> {
            String table = TextUtil.defaultIfBlank(tableName, "t_project");
            List<MetaColumnVO> list = metaDao.listColumns(table);
            return table(new String[]{"表名", "字段", "类型", "可空", "键", "额外", "注释", "顺序"},
                    list, c -> new Object[]{
                            c.getTableName(), c.getColumnName(), c.getColumnType(),
                            c.isNullable() ? "是" : "否", nz(c.getColumnKey()),
                            nz(c.getExtra()), nz(c.getColumnComment()), c.getOrdinal()
                    });
        });
    }

    public TableData metaConstraints(String tableName) {
        return load("x4_constraints", () -> {
            String table = TextUtil.defaultIfBlank(tableName, "t_project_participant");
            List<MetaConstraintVO> list = metaDao.listConstraints(table);
            return table(new String[]{"表名", "约束名", "类型", "字段", "引用表", "引用字段"},
                    list, c -> new Object[]{
                            c.getTableName(), c.getConstraintName(), c.getConstraintType(),
                            nz(c.getColumns()), nz(c.getRefTable()), nz(c.getRefColumns())
                    });
        });
    }

    public DashboardData dashboard() {
        if (demoMode) {
            usingFallback = true;
            lastError = "演示模式";
            return DemoData.dashboard();
        }
        try {
            int countries = (int) countryDao.countAll();
            int projects = (int) projectDao.count(new ProjectQuery());
            int enterprises = (int) enterpriseDao.countAll();
            int openWarnings = warningDao.countOpen();

            List<StatRow> region = countryDao.statProjectsByRegion();
            String[] regionLabels = region.stream().map(StatRow::getLabel).toArray(String[]::new);
            double[] regionValues = region.stream().mapToDouble(StatRow::getCnt).toArray();

            List<SourceRow> sources = fundDao.statBySource();
            String[] sourceLabels = sources.stream().map(SourceRow::getSourceName).toArray(String[]::new);
            double[] sourceValues = sources.stream()
                    .map(SourceRow::getSharePct)
                    .mapToDouble(DataService::toDouble)
                    .toArray();

            List<ProjectVO> recent = projectDao.findByCondition(
                    new ProjectQuery(), Page.of(1, 6, "project_id DESC"));
            Object[][] projectRows = recent.stream()
                    .map(p -> new Object[]{
                            p.getProjectCode(), p.getProjectName(), nz(p.getCountryName()),
                            nz(p.getStatus()), amount(p.getInvestAmount()), date(p.getSignDate())
                    })
                    .toArray(Object[][]::new);

            usingFallback = false;
            lastError = null;
            return new DashboardData(
                    countries, projects, enterprises, openWarnings,
                    regionLabels, regionValues,
                    sourceLabels, sourceValues,
                    new String[]{"编号", "项目", "国家", "状态", "投资额", "签约日期"},
                    projectRows,
                    false,
                    null
            );
        } catch (RuntimeException e) {
            usingFallback = true;
            lastError = e.getMessage();
            return DemoData.dashboard();
        }
    }

    public Result<Void> recordActual(String projectId, String milestoneId, String dateText) {
        long project = parseLong(projectId, 1);
        int milestone = parseInt(milestoneId, 3);
        LocalDate date;
        try {
            date = TextUtil.isBlank(dateText) ? LocalDate.now() : LocalDate.parse(dateText.trim());
        } catch (DateTimeParseException e) {
            return Result.fail("日期格式应为 yyyy-MM-dd");
        }
        if (demoMode) {
            return Result.ok(null, "演示模式：已模拟补录实际日期");
        }
        return milestoneDao.recordActualByType(project, milestone, date);
    }

    public WarningDao.RefreshResult refreshWarnings() {
        if (demoMode) {
            return new WarningDao.RefreshResult(6, 4, 2, List.of("演示模式"));
        }
        return warningDao.refresh();
    }

    public Result<Integer> ackWarning(long warnId) {
        if (demoMode) {
            return Result.ok(1, "演示模式：已模拟处理预警");
        }
        return warningDao.ackWarning(warnId, "D同学");
    }

    public int calcCommitments() {
        return demoMode ? 136 : commitmentDao.calcAll();
    }

    private TableData hireTable(List<HireRow> list) {
        return table(new String[]{"项目编号", "项目", "报告期", "本地员工", "现场员工", "属地用工率"},
                list, r -> new Object[]{
                        r.getProjectCode(), r.getProjectName(), nz(r.getPeriodYm()),
                        r.getLocalStaff(), r.getOnSiteStaff(), pct(r.getLocalHirePct())
                });
    }

    private TableData load(String demoKey, Supplier<TableData> loader) {
        if (demoMode) {
            usingFallback = true;
            lastError = "演示模式";
            return DemoData.table(demoKey);
        }
        try {
            TableData data = loader.get();
            usingFallback = false;
            lastError = null;
            return data;
        } catch (DataAccessException e) {
            usingFallback = true;
            lastError = e.getMessage();
            return DemoData.table(demoKey);
        } catch (RuntimeException e) {
            usingFallback = true;
            lastError = "查询失败：" + e.getMessage();
            return DemoData.table(demoKey);
        }
    }

    private static <T> TableData table(String[] columns, List<T> list, Function<T, Object[]> mapper) {
        Object[][] rows = new Object[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            rows[i] = mapper.apply(list.get(i));
        }
        return new TableData(columns, rows);
    }

    private static String amount(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        BigDecimal yi = new BigDecimal("100000000");
        if (value.abs().compareTo(yi) >= 0) {
            return "¥ " + value.divide(yi, 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " 亿";
        }
        return "¥ " + TextUtil.formatAmount(value);
    }

    private static String pct(BigDecimal value) {
        return value == null ? "—" : value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "—" : value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String date(LocalDate value) {
        return value == null ? "—" : value.toString();
    }

    private static String datetime(java.time.LocalDateTime value) {
        return value == null ? "—" : value.toLocalDate() + " " + value.toLocalTime().withNano(0);
    }

    private static String delayedText(Boolean value) {
        return value == null ? "—" : value ? "超期" : "准时";
    }

    private static String condition(String op, BigDecimal value) {
        if (TextUtil.isBlank(op) || value == null) {
            return "—";
        }
        return op + " " + decimal(value);
    }

    private static String nz(String value) {
        return TextUtil.defaultIfBlank(value, "—");
    }

    private static long parseLong(String text, long fallback) {
        try {
            return Long.parseLong(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double toDouble(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }

    public static final class DashboardData {
        public final int countryCount;
        public final int projectCount;
        public final int enterpriseCount;
        public final int openWarningCount;
        public final String[] regionLabels;
        public final double[] regionValues;
        public final String[] sourceLabels;
        public final double[] sourceValues;
        public final String[] projectColumns;
        public final Object[][] projectRows;
        public final boolean fallback;
        public final String note;

        public DashboardData(int countryCount, int projectCount, int enterpriseCount,
                             int openWarningCount, String[] regionLabels, double[] regionValues,
                             String[] sourceLabels, double[] sourceValues,
                             String[] projectColumns, Object[][] projectRows,
                             boolean fallback, String note) {
            this.countryCount = countryCount;
            this.projectCount = projectCount;
            this.enterpriseCount = enterpriseCount;
            this.openWarningCount = openWarningCount;
            this.regionLabels = regionLabels;
            this.regionValues = regionValues;
            this.sourceLabels = sourceLabels;
            this.sourceValues = sourceValues;
            this.projectColumns = projectColumns;
            this.projectRows = projectRows;
            this.fallback = fallback;
            this.note = note;
        }
    }
}
