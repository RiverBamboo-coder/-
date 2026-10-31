/**
 * 数据访问层（DAO）：一个模块一个 DAO 类，一人一个包内位置，互不改对方的类。
 *
 * <p>归属约定（防止类名冲突）：</p>
 * <ul>
 *   <li><b>A</b>：CountryDao、ProjectDao、EnterpriseDao、ConsortiumDao、ContractDao、DictDao、MetaDao</li>
 *   <li><b>B</b>：FundDao、ReportDao、WarningDao、WarningRuleDao</li>
 *   <li><b>C</b>：MilestoneDao、CommitmentDao、OutcomeDao</li>
 * </ul>
 *
 * <p>硬性要求：</p>
 * <ol>
 *   <li>SQL 一律 {@link java.sql.PreparedStatement} 参数化，禁止把用户输入拼进 SQL；</li>
 *   <li>列表查询必须接收 {@link com.bri.common.Page}，排序必须走
 *       {@link com.bri.common.Page#resolveOrderBy(String...)} 白名单；</li>
 *   <li>捕获 {@link com.bri.common.DataAccessException} 后返回
 *       {@link com.bri.common.Result#fail(String)}（中文提示），不要把异常堆栈丢给界面；</li>
 *   <li>查询结果映射成 {@code com.bri.vo} 下的 VO，方法是“查询 → 对象映射”，答辩时讲这一点。</li>
 * </ol>
 *
 * @author A
 */
package com.bri.dao;
