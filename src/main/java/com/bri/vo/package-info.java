/**
 * 视图对象（VO）：与 DAO 查询一一对应的“行”对象，界面（D）直接绑定表格或图表。
 *
 * <p>命名约定：实体用 {@code XxxVO}（CountryVO、ProjectVO、EnterpriseVO），
 * 统计行用 {@code XxxRow}（StatRow、RateRow、DelayRow、CommitRow），与第 5 章接口契约表一致。</p>
 *
 * <p>字段类型约定：金额/比率用 {@link java.math.BigDecimal}（不要用 double），
 * 日期用 {@link java.time.LocalDate}（不要用 java.util.Date），主键与外键用 {@code long/Long}，
 * 布尔标志用 {@code boolean/Boolean}。字段名与 SQL 别名保持一致，便于对照。</p>
 *
 * @author A
 */
package com.bri.vo;
