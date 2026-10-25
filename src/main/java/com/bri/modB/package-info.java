/**
 * B 的模块代码（M4 资金 / M6 履约报告 / M8 预警）。
 *
 * <p>按任务书 0.1 的约定，A/B/C 不新建窗口类（界面全部在 {@code com.bri.ui}，由 D 维护），
 * 所以这个包只放<b>控制台自检程序</b>，用来在界面接进来之前证明 DAO 逻辑是通的：</p>
 *
 * <pre>
 * java -cp target/bri-pm.jar com.bri.modB.BSelfTest
 * </pre>
 *
 * <p>真正的业务实现都在 {@code com.bri.dao} + {@code com.bri.vo}：
 * FundDao / FundPlanDao / FundingSourceDao / ReportDao / ReportIndicatorDao /
 * WarningDao / WarningRuleDao。</p>
 *
 * @author B
 */
package com.bri.modB;
