package com.bri.common;

import java.io.Serializable;

/**
 * 统一返回值（A-2 公共类之一，全员共用）。
 *
 * <p>约定：A/B/C 的 DAO 方法一律返回 {@code Result<T>}，不要“成功返回对象、失败抛异常”，
 * 也不要把 {@code SQLException} 的堆栈直接丢给界面。界面（D）只需要看两个东西：</p>
 *
 * <pre>
 * Result&lt;Long&gt; r = projectDao.insert(vo);
 * if (r.isSuccess()) {
 *     MessageUtil.success(this, r.getMessage());
 * } else {
 *     MessageUtil.error(this, r.getMessage());   // message 已经是中文提示
 * }
 * </pre>
 *
 * <p>典型用法：</p>
 * <pre>
 * public Result&lt;Void&gt; deleteDisbursement(long disbId) {
 *     if (disbId &lt;= 0) {
 *         return Result.fail("拨付单号不合法");
 *     }
 *     return Result.ok(null, "删除成功");
 * }
 * </pre>
 *
 * @param <T> 返回数据的类型，无数据时用 {@link Void}
 * @author A
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 默认成功提示 */
    public static final String DEFAULT_OK_MESSAGE = "操作成功";

    /** 默认失败提示 */
    public static final String DEFAULT_FAIL_MESSAGE = "操作失败";

    /** 是否成功：true=成功，false=失败 */
    private boolean success;

    /** 提示信息：失败时必须是可直接展示给用户的中文原因，禁止为空 */
    private String message;

    /** 业务数据：失败时为 null */
    private T data;

    public Result() {
        this.message = "";
    }

    public Result(boolean success, String message, T data) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.data = data;
    }

    /**
     * 成功并携带数据。
     *
     * @param data 业务数据，可为 null
     * @param <T>  数据类型
     * @return 成功的 Result
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(true, DEFAULT_OK_MESSAGE, data);
    }

    /**
     * 成功并携带数据与自定义提示（例如“新增成功，项目编码 P2024-007”）。
     *
     * @param data    业务数据，可为 null
     * @param message 成功提示
     * @param <T>     数据类型
     * @return 成功的 Result
     */
    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(true, message == null ? DEFAULT_OK_MESSAGE : message, data);
    }

    /**
     * 成功但不返回数据（新增/修改/删除场景）。
     *
     * @param <T> 数据类型
     * @return 成功的 Result
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 失败。
     *
     * @param message 可直接展示的中文失败原因
     * @param <T>     数据类型
     * @return 失败的 Result
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(false, message == null || message.isEmpty() ? DEFAULT_FAIL_MESSAGE : message, null);
    }

    /**
     * 失败并携带回显数据（例如表单回填）。
     *
     * @param message 可直接展示的中文失败原因
     * @param data    回显数据，可为 null
     * @param <T>     数据类型
     * @return 失败的 Result
     */
    public static <T> Result<T> fail(String message, T data) {
        return new Result<>(false, message == null || message.isEmpty() ? DEFAULT_FAIL_MESSAGE : message, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Result{success=" + success + ", message='" + message + "', data=" + data + '}';
    }
}
