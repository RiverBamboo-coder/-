package com.bri.vo;



/**
 * 字典项（下拉框数据源）
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class DictItem {

    /** 字典项 ID */
    private int itemId;

    /** 字典类型编码 */
    private String typeCode;

    /** 项编码（入库值） */
    private String itemCode;

    /** 项名称（显示值） */
    private String itemName;

    /** 排序号 */
    private int sortNo;

    /** 是否启用 */
    private boolean enabled;


    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getSortNo() {
        return sortNo;
    }

    public void setSortNo(int sortNo) {
        this.sortNo = sortNo;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
