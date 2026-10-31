/**
 * 表示层（D 负责）：主框架 MainFrame、9 个业务面板、X1 看板、X4 数据字典页，以及通用组件。
 *
 * <p>A/B/C 不在本包新增类，界面需要什么数据就按第 5 章接口契约表的方法签名向对应负责人要。</p>
 *
 * <p>界面层铁律：</p>
 * <ol>
 *   <li>不写 SQL，只调用 DAO/Service 的方法（展示型临时查询可用
 *       {@link com.bri.common.DbUtil#query(String, Object...)}，但业务逻辑必须落在 DAO）；</li>
 *   <li>所有面板实现统一接口 {@code ModulePanel { void refresh(); }}，切换菜单时刷新数据；</li>
 *   <li>数据库连不上时给出中文提示（{@link com.bri.common.DbUtil#getConnection()} 抛出的 message 已可直接展示），
 *       不允许直接把异常堆栈弹给用户。</li>
 * </ol>
 *
 * @author A
 */
package com.bri.ui;
