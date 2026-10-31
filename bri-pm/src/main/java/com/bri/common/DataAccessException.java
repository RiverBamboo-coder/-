package com.bri.common;

/**
 * 数据访问异常（A-2 公共类之一）。
 *
 * <p>为什么要它：{@link java.sql.SQLException} 是受检异常，如果 DAO 方法都写 {@code throws SQLException}，
 * 界面（D）就要到处 try-catch；如果 DAO 直接吞掉，界面又拿不到失败原因。
 * 所以 {@link DbUtil} 把 {@code SQLException} 统一包装成这个<b>运行时</b>异常，
 * 并且 message 已经整理成可直接展示的中文：</p>
 *
 * <pre>
 * try {
 *     int rows = DbUtil.update("DELETE FROM t_fund_disbursement WHERE disb_id = ?", disbId);
 *     ...
 * } catch (DataAccessException e) {
 *     MessageUtil.error(this, e.getMessage());   // 例：更新失败：Cannot delete or update a parent row ...（SQL：DELETE ...）
 * }
 * </pre>
 *
 * <p>注意：<b>不要</b>在界面里 catch 之后只 {@code e.printStackTrace()} 而不提示用户；
 * 也不要把原始 SQL 异常堆栈整段弹到界面上。</p>
 *
 * @author A
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
