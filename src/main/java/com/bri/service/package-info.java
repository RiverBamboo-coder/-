/**
 * 可选业务层：放“跨 DAO / 跨模块”的计算，例如资金到位率、承诺比对结论汇总、预警刷新编排。
 *
 * <p>约定：</p>
 * <ul>
 *   <li>单表 CRUD 不进 Service，DAO 直接对外；只有需要组合多个 DAO 或做业务判断时才建 Service；</li>
 *   <li>能在 SQL 里算的（比率、计数、差值）优先在 SQL 里算，Service 只做编排；</li>
 *   <li>界面（D）可以调 DAO，也可以调 Service，但都不写 SQL。</li>
 * </ul>
 *
 * @author A
 */
package com.bri.service;
