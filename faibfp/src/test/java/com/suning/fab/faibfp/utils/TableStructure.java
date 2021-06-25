package com.suning.fab.faibfp.utils;

/**
 * @Author 19043955
 * @Date 2019/10/15 16:59
 * @Version 1.0
 */
public class TableStructure {

    private String fieldName;

    private String fieldType;

    private String fieldLong;

    private boolean nullFlag;

    private String defaultValue;

    private String fieldComment;

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldLong() {
        return fieldLong;
    }

    public void setFieldLong(String fieldLong) {
        this.fieldLong = fieldLong;
    }

    public boolean isNullFlag() {
        return nullFlag;
    }

    public void setNullFlag(boolean nullFlag) {
        this.nullFlag = nullFlag;
    }

    public String getFieldComment() {
        return fieldComment;
    }

    public void setFieldComment(String fieldComment) {
        this.fieldComment = fieldComment;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
