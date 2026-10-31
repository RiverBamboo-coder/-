package com.bri.ui;

import com.bri.ui.common.Theme;

import java.util.List;

/**
 * All 9 business pages plus the X4 metadata page, configured with DAO-backed actions.
 */
public final class ModuleCatalog {
    private ModuleCatalog() {
    }

    public static List<ModuleSpec> all(DataService ds) {
        return List.of(
                m1(ds), m2(ds), m3(ds), m4(ds), m5(ds),
                m6(ds), m7(ds), m8(ds), m9(ds), x4(ds)
        );
    }

    private static ModuleSpec m1(DataService ds) {
        return new ModuleSpec(
                "m1", "M1 国家与区域", "国家、区域、共建属性与项目覆盖",
                "基础数据", "globe", Theme.PRIMARY,
                "展示 61 个共建国家的区域分布，并支持查找尚无项目的国家。",
                List.of("国家—项目 1:N", "区域递归联系", "区域覆盖率统计"),
                List.of(
                        new QueryAction("国家列表", "按国家查看区域、项目数和投资额。", ds::countries),
                        new QueryAction("区域项目统计", "按大洲/区域汇总项目数量和投资额。", ds::regionStats),
                        new QueryAction("无项目国家", "找出尚未落地项目的共建国家。", ds::countriesWithoutProject)
                )
        );
    }

    private static ModuleSpec m2(DataService ds) {
        return new ModuleSpec(
                "m2", "M2 项目管理", "项目检索、合同缺口、年度趋势与逾期移交",
                "核心业务", "project", Theme.BLUE,
                "项目管理覆盖立项、签约、建设与移交，是其他业务模块的主线。",
                List.of("项目多条件检索", "投资额与合同额对比", "年度新签趋势"),
                List.of(
                        new QueryAction("项目检索", "可按项目编号/名称关键字检索项目。",
                                "关键字", "", null, ds::projects),
                        new QueryAction("合同缺口", "对比项目投资额与合同金额的差异。", ds::contractGaps),
                        new QueryAction("年度新签趋势", "按年份统计新签项目数和投资额。", ds::projectYearly),
                        new QueryAction("逾期未移交", "查找已过计划竣工日期但尚未移交的项目。", ds::overdueProjects)
                )
        );
    }

    private static ModuleSpec m3(DataService ds) {
        return new ModuleSpec(
                "m3", "M3 企业与联合体", "企业参与项目、排名与联合体协作",
                "参与主体", "enterprise", Theme.PURPLE,
                "企业与项目是多对多联系，联合体成员还带有份额等联系属性。",
                List.of("企业—项目 M:N", "联合体份额属性", "跨洲企业统计"),
                List.of(
                        new QueryAction("企业列表", "查看企业性质、注册国家和项目数。", ds::enterprises),
                        new QueryAction("企业项目排名", "按总包金额和项目数对企业排名。", ds::enterpriseRanking),
                        new QueryAction("联合体份额", "查看联合体成员数、份额合计和项目数。", ds::consortiumShare),
                        new QueryAction("跨洲企业", "查找业务覆盖 3 个洲以上的企业。", ds::crossContinentEnterprises)
                )
        );
    }

    private static ModuleSpec m4(DataService ds) {
        return new ModuleSpec(
                "m4", "M4 资金管理", "资金计划、拨付流水、到位率与来源结构",
                "B 线资金", "fund", Theme.ACCENT,
                "资金线以年度计划和实际拨付为基础，计算项目资金到位率。",
                List.of("计划 vs 实际拨付", "低到位率预警", "资金来源结构"),
                List.of(
                        new QueryAction("资金到位率", "按项目查看计划金额、实际拨付和到位率。", ds::fundRates),
                        new QueryAction("低到位率项目", "筛选到位率低于 60% 的项目。", ds::lowFundRates),
                        new QueryAction("资金来源占比", "按资金源类型统计实际拨付占比。", ds::fundSources),
                        new QueryAction("年度计划对比", "按年度对比计划金额和实际拨付。", ds::fundYearCompare)
                )
        );
    }

    private static ModuleSpec m5(DataService ds) {
        return new ModuleSpec(
                "m5", "M5 进度与里程碑", "里程碑、超期判定和工期对比",
                "C 线进度", "progress", Theme.PRIMARY,
                "里程碑超期由数据库生成列自动判定，界面只读取三态结果。",
                List.of("生成列 is_delayed", "三态：超期/准时/未完成", "计划 vs 实际工期"),
                List.of(
                        new QueryAction("项目里程碑", "输入项目 ID，查看该项目的里程碑列表。",
                                "项目ID", "1", null, ds::milestones),
                        new QueryAction("超期未完成", "查看已超期但尚未补录实际日期的节点。", ds::overdueMilestones),
                        new QueryAction("平均超期", "按里程碑类型统计平均超期天数。", ds::avgMilestoneDelay),
                        new QueryAction("工期对比", "对比项目计划工期与实际工期。", ds::durationCompare)
                )
        );
    }

    private static ModuleSpec m6(DataService ds) {
        return new ModuleSpec(
                "m6", "M6 预警中心", "规则驱动识别资金、进度和履约风险",
                "B 线风控", "warning", Theme.DANGER,
                "预警规则、触发快照和处理状态都由数据库维护，刷新操作可重复执行。",
                List.of("红/黄/蓝三级预警", "规则阈值数据化", "刷新与确认闭环"),
                List.of(
                        new QueryAction("预警列表", "按红/黄/蓝级别筛选预警记录。",
                                "级别", "全部", new String[]{"全部", "R", "Y", "B"}, ds::warnings),
                        new QueryAction("预警规则", "查看规则条件、级别和启用状态。", ds::warningRules)
                )
        );
    }

    private static ModuleSpec m7(DataService ds) {
        return new ModuleSpec(
                "m7", "M7 履约承诺", "多源对齐、三值逻辑与四档履约结论",
                "复杂关联", "commitment", Theme.BLUE,
                "M7 是项目高难度落点：工期、金额、属地用工、培训四类承诺统一比对。",
                List.of("多源对齐", "NULL → 待评估", "幂等 calcAll()"),
                List.of(
                        new QueryAction("按结论查询", "选择已履约/部分履约/未履约/待评估。",
                                "结论", "待评估", new String[]{"待评估", "已履约", "部分履约", "未履约"},
                                ds::commitmentsByStatus),
                        new QueryAction("分类型履约率", "按承诺类型统计履约率。", ds::commitRates),
                        new QueryAction("交叉矛盾", "查找同一项目既有履约又有未履约的情况。", ds::commitContradictions),
                        new QueryAction("国别对账", "按国家汇总承诺履约情况。", ds::countryReconcile)
                )
        );
    }

    private static ModuleSpec m8(DataService ds) {
        return new ModuleSpec(
                "m8", "M8 进度报告", "月度环比、国别产出、停滞项目与属地用工",
                "B 线报告", "report", Theme.PURPLE,
                "进度报告按项目+报告期唯一，支持环比和国别产出分析。",
                List.of("同项目同月唯一", "LAG 环比", "低属地用工筛查"),
                List.of(
                        new QueryAction("月度环比", "输入项目 ID，查看报告期进度环比。",
                                "项目ID", "1", null, ds::monthlyMom),
                        new QueryAction("国别产出", "按国家汇总项目数和产出总额。", ds::outputByCountry),
                        new QueryAction("停滞项目", "查找连续 3 个月没有进度变化的项目。", ds::stagnantProjects),
                        new QueryAction("低属地用工", "筛选属地用工比例低于 50% 的报告。", ds::lowLocalHire)
                )
        );
    }

    private static ModuleSpec m9(DataService ds) {
        return new ModuleSpec(
                "m9", "M9 项目成效", "受益人口、就业、培训与绿色装机",
                "思政成效", "outcome", Theme.SUCCESS,
                "用项目成效数据呈现共商共建共享、授人以渔和绿色丝绸之路。",
                List.of("大洲成效汇总", "双达标就业", "绿色装机与标准输出"),
                List.of(
                        new QueryAction("大洲成效", "按大洲汇总项目数、受益人口和就业。", ds::outcomeByRegion),
                        new QueryAction("双达标就业", "筛选属地用工≥70%且培训≥1000 的项目。", ds::highLocalHire),
                        new QueryAction("绿色装机", "按领域统计绿色装机与标准输出。", ds::greenByField),
                        new QueryAction("项目成效", "输入项目 ID 查看单个项目成效。",
                                "项目ID", "1", null, ds::projectOutcome)
                )
        );
    }

    private static ModuleSpec x4(DataService ds) {
        return new ModuleSpec(
                "x4", "X4 数据字典", "基础字典与数据库元数据",
                "设计加分", "dictionary", Theme.TEXT_SUB,
                "展示字典项、24 张表、字段与约束，方便答辩时解释数据库设计。",
                List.of("46 条字典项", "24 张业务表", "主键/外键/检查约束"),
                List.of(
                        new QueryAction("基础字典项", "查看项目状态等字典项。",
                                "字典类型", "PROJECT_STATUS", null, ds::dictItems),
                        new QueryAction("数据库表清单", "查看表名、注释和行数。", ds::metaTables),
                        new QueryAction("字段结构", "输入表名查看字段类型和空值约束。",
                                "表名", "t_project", null, ds::metaColumns),
                        new QueryAction("约束信息", "输入表名查看主键、外键和检查约束。",
                                "表名", "t_project_participant", null, ds::metaConstraints)
                )
        );
    }
}
