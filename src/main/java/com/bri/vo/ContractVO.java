package com.bri.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目合同（M2 详情页）
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class ContractVO {

    /** 合同 ID */
    private long contractId;

    /** 项目 ID */
    private long projectId;

    /** 合同编号 */
    private String contractNo;

    /** 合同类型 */
    private String contractType;

    /** 承接方企业 */
    private String partyBName;

    /** 合同金额 */
    private BigDecimal amount;

    /** 币种 */
    private String currencyCode;

    /** 签约日期 */
    private LocalDate signDate;


    public long getContractId() {
        return contractId;
    }

    public void setContractId(long contractId) {
        this.contractId = contractId;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getPartyBName() {
        return partyBName;
    }

    public void setPartyBName(String partyBName) {
        this.partyBName = partyBName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public LocalDate getSignDate() {
        return signDate;
    }

    public void setSignDate(LocalDate signDate) {
        this.signDate = signDate;
    }
}
