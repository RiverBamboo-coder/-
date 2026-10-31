package com.bri.vo;

/**
 * 交叉矛盾项目行（C 负责，对应 M7 的 Q7-3）。
 *
 * <p>含义：同一项目里既存在“已履约”又存在“未履约”的承诺，说明履约进度参差不齐、
 * 存在内部矛盾，需要人工复核。</p>
 *
 * @author C
 */
public class ContraRow {

    /** 项目主键 */
    private long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 承诺总数 */
    private long totalCnt;

    /** 已履约数 */
    private long fulfilledCnt;

    /** 未履约数 */
    private long breachedCnt;

    /** 部分履约数 */
    private long partialCnt;

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public long getTotalCnt() {
        return totalCnt;
    }

    public void setTotalCnt(long totalCnt) {
        this.totalCnt = totalCnt;
    }

    public long getFulfilledCnt() {
        return fulfilledCnt;
    }

    public void setFulfilledCnt(long fulfilledCnt) {
        this.fulfilledCnt = fulfilledCnt;
    }

    public long getBreachedCnt() {
        return breachedCnt;
    }

    public void setBreachedCnt(long breachedCnt) {
        this.breachedCnt = breachedCnt;
    }

    public long getPartialCnt() {
        return partialCnt;
    }

    public void setPartialCnt(long partialCnt) {
        this.partialCnt = partialCnt;
    }
}
