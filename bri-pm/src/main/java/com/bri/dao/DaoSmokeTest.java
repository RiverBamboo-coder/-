package com.bri.dao;

import com.bri.common.DataAccessException;
import com.bri.common.Page;
import com.bri.vo.CountryVO;
import com.bri.vo.DictItem;
import com.bri.vo.EnterpriseVO;
import com.bri.vo.GapRow;
import com.bri.vo.MetaColumnVO;
import com.bri.vo.MetaConstraintVO;
import com.bri.vo.MetaTableVO;
import com.bri.vo.ProjectQuery;
import com.bri.vo.ProjectRoleVO;
import com.bri.vo.ProjectVO;
import com.bri.vo.RankRow;
import com.bri.vo.ShareRow;
import com.bri.vo.StatRow;

import java.math.BigDecimal;
import java.util.List;

/**
 * A-8 / A-9 的控制台验证工具（开发期自检，交付后可以删）。
 *
 * <p>运行前先执行 {@code sql/01_schema.sql → 02_dict_and_user.sql → 03_seed_basic.sql}，然后：</p>
 *
 * <pre>
 * mvn -q compile exec:java -Dexec.mainClass=com.bri.dao.DaoSmokeTest
 * 或直接用 IDE 运行本类的 main 方法（classpath 需要 mysql 驱动 + db.properties）
 * </pre>
 *
 * <p>每个 DAO 方法打印“返回行数 + 首行样例”，与 README 里的验证清单逐条对照即可。</p>
 *
 * @author A
 */
public final class DaoSmokeTest {

    private DaoSmokeTest() {
    }

    public static void main(String[] args) {
        System.out.println("=== bri-pm DAO 控制台自检 ===");
        CountryDao countryDao = new CountryDao();
        ProjectDao projectDao = new ProjectDao();
        EnterpriseDao enterpriseDao = new EnterpriseDao();
        ContractDao contractDao = new ContractDao();
        DictDao dictDao = new DictDao();
        MetaDao metaDao = new MetaDao();
        int failed = 0;
        try {
            // ---------- M1 国家 ----------
            List<CountryVO> countries = countryDao.findAll(Page.of(1, 20));
            System.out.printf("[M1] CountryDao.findAll(1,20)           -> %d 行；首行 %s%n",
                    countries.size(), countries.isEmpty() ? "-" : countries.get(0).getCountryCode() + " " + countries.get(0).getCountryName());
            System.out.printf("[M1] CountryDao.countAll()              -> %d（期望 61）%n", countryDao.countAll());
            System.out.printf("[M1] CountryDao.findByRegion(1=亚洲)    -> %d 行%n", countryDao.findByRegion(1).size());
            System.out.printf("[M1] CountryDao.findByCode(KEN)         -> %s%n",
                    countryDao.findByCode("KEN") == null ? "未找到" : countryDao.findByCode("KEN").getCountryName());
            System.out.println("[M1] Q1-1 statProjectsByRegion：");
            for (StatRow row : countryDao.statProjectsByRegion()) {
                System.out.printf("        %-8s %-6s 项目 %2d 个，投资 %s%n",
                        row.getCode(), row.getLabel(), row.getCnt(), row.getTotal());
            }
            System.out.printf("[M1] Q1-2 findCountriesWithoutProject   -> %d 个无项目国家%n",
                    countryDao.findCountriesWithoutProject().size());
            System.out.println("[M1] Q1-3 statRegionCoverage（前 5 行）：");
            List<StatRow> coverage = countryDao.statRegionCoverage();
            for (int i = 0; i < Math.min(5, coverage.size()); i++) {
                StatRow row = coverage.get(i);
                System.out.printf("        %-8s 国家 %2d / 覆盖 %2d，覆盖率 %s%n",
                        row.getLabel(), row.getCnt(), row.getCovered(), row.getPct());
            }

            // ---------- M2 项目 ----------
            List<ProjectVO> projects = projectDao.findByCondition(new ProjectQuery(), Page.of(1, 10));
            System.out.printf("[M2] ProjectDao.findByCondition(全部)   -> %d 行 / 总数 %d（期望 40）%n",
                    projects.size(), projectDao.count(new ProjectQuery()));
            ProjectQuery building = new ProjectQuery();
            building.setStatus("在建");
            System.out.printf("[M2] findByCondition(status=在建)        -> %d 个（期望 22）%n",
                    projectDao.count(building));
            ProjectQuery asiaTrans = new ProjectQuery();
            asiaTrans.setContinentCode("AS");
            asiaTrans.setFieldCode("TRANS");
            System.out.printf("[M2] 四维筛选(亚洲+TRANS)               -> %d 个%n",
                    projectDao.count(asiaTrans));
            System.out.printf("[M2] findById(1)                        -> %s%n",
                    projectDao.findById(1) == null ? "未找到" : projectDao.findById(1).getProjectName());
            System.out.printf("[M2] Q2-1 statByFieldAndStatus          -> %d 行；首行 %s%n",
                    projectDao.statByFieldAndStatus().size(),
                    projectDao.statByFieldAndStatus().get(0).getLabel());
            List<GapRow> gaps = projectDao.findContractGapProjects();
            System.out.printf("[M2] Q2-2 findContractGapProjects       -> %d 个项目合同与投资不一致（期望 5）%n", gaps.size());
            for (GapRow g : gaps) {
                System.out.printf("        %-16s 差额 %s（%s%%）%n", g.getProjectCode(), g.getGap(), g.getGapPct());
            }
            System.out.printf("[M2] ContractDao.findByProject(1)       -> %d 份合同%n", contractDao.findByProject(1).size());
            System.out.printf("[M2] ContractDao.countAll()             -> %d 份（期望 80）%n", contractDao.countAll());

            // ---------- M3 企业 ----------
            List<EnterpriseVO> enterprises = enterpriseDao.findAll(Page.of(1, 20));
            System.out.printf("[M3] EnterpriseDao.findAll(1,20)        -> %d 行 / 总数 %d（期望 35）%n",
                    enterprises.size(), enterpriseDao.countAll());
            List<ProjectRoleVO> roles = enterpriseDao.findProjectsByEnterprise(1);
            System.out.printf("[M3] findProjectsByEnterprise(1)        -> %d 条参与记录%n", roles.size());
            System.out.println("[M3] Q3-1 statEnterpriseRanking(top5)：");
            for (RankRow row : enterpriseDao.statEnterpriseRanking(5)) {
                System.out.printf("        #%d %-22s %-8s 项目 %2d 个，总承包投资 %s%n",
                        row.getRankNo(), row.getEnterpriseName(), row.getNatureName(),
                        row.getProjectCount(), row.getZcAmount());
            }
            System.out.println("[M3] Q3-4 statConsortiumShare：");
            for (ShareRow row : enterpriseDao.statConsortiumShare()) {
                System.out.printf("        %-20s 牵头 %-16s 成员 %d 人，份额合计 %s%n",
                        row.getConsortiumName(), row.getLeaderName(), row.getMemberCount(), row.getShareSum());
            }

            // ---------- M0 字典 ----------
            List<DictItem> statuses = dictDao.listByType("PROJECT_STATUS");
            System.out.printf("[M0] DictDao.listByType(PROJECT_STATUS) -> %d 条：%s%n",
                    statuses.size(), statuses.isEmpty() ? "-" : statuses.get(0).getItemCode());
            System.out.printf("[M0] DictDao.itemName(ROLE, ZC)         -> %s%n", dictDao.itemName("ROLE", "ZC"));

            // ---------- X4 数据字典 ----------
            List<MetaTableVO> tables = metaDao.listTables();
            System.out.printf("[X4] MetaDao.listTables()               -> %d 张表（期望 24）%n", tables.size());
            List<MetaColumnVO> columns = metaDao.listColumns("t_project_participant");
            System.out.printf("[X4] listColumns(t_project_participant) -> %d 个字段%n", columns.size());
            List<MetaConstraintVO> constraints = metaDao.listConstraints("t_project_participant");
            System.out.printf("[X4] listConstraints(t_project_participant) -> %d 个约束%n", constraints.size());
            for (MetaConstraintVO c : constraints) {
                System.out.printf("        %-12s %-20s %s%n", c.getConstraintType(), c.getConstraintName(),
                        c.getRefTable() == null ? c.getColumns() : c.getColumns() + " -> " + c.getRefTable());
            }

            // ---------- 写入闭环（新增 → 修改 → 删除）----------
            CountryVO temp = new CountryVO();
            temp.setCountryCode("ZZZ");
            temp.setCountryName("自检临时国家");
            temp.setRegionId(7);
            temp.setBeltRoad(false);
            temp.setPopulationK(new BigDecimal("1.00"));
            int inserted = countryDao.insert(temp);
            int updated = countryDao.update(temp);
            int deleted = countryDao.deleteByCode("ZZZ");
            System.out.printf("[写] 新增/修改/删除临时国家 ZZZ          -> %d / %d / %d（期望 1/1/1）%n",
                    inserted, updated, deleted);
            System.out.println();
        } catch (DataAccessException e) {
            failed++;
            System.err.println("[FAIL] 数据库访问失败：" + e.getMessage());
            System.err.println("       请先确认 MySQL 已启动、bri_pm 库已建好，并已执行 01→02→03 三个脚本。");
        } catch (RuntimeException e) {
            failed++;
            System.err.println("[FAIL] 未预期异常：" + e);
            e.printStackTrace();
        }
        if (failed > 0) {
            System.out.println("=== 自检失败，见上面的 FAIL 行 ===");
            System.exit(1);
        }
        System.out.println("=== 自检结束：全部方法可用 ===");
    }
}
