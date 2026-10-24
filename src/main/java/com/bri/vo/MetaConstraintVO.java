package com.bri.vo;



/**
 * 数据字典 X4：约束清单（主键/外键/唯一/检查）
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class MetaConstraintVO {

    /** 表名 */
    private String tableName;

    /** 约束名 */
    private String constraintName;

    /** 类型：PRIMARY KEY/FOREIGN KEY/UNIQUE/CHECK */
    private String constraintType;

    /** 约束列（逗号分隔） */
    private String columns;

    /** 外键引用表 */
    private String refTable;

    /** 外键引用列 */
    private String refColumns;


    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public String getConstraintType() {
        return constraintType;
    }

    public void setConstraintType(String constraintType) {
        this.constraintType = constraintType;
    }

    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public String getRefTable() {
        return refTable;
    }

    public void setRefTable(String refTable) {
        this.refTable = refTable;
    }

    public String getRefColumns() {
        return refColumns;
    }

    public void setRefColumns(String refColumns) {
        this.refColumns = refColumns;
    }
}
