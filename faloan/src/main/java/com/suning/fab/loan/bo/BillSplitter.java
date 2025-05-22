package com.suning.fab.loan.bo;

public class BillSplitter {
    // 账本拆分日期左状态
    private String leftSection;
    // 账本拆分日期
    private String splitDate;
    // 账本拆分日期右状态
    private String rightSection;
    // 拆分类型
    private String splitType;

    public String getLeftSection() {
        return leftSection;
    }

    public void setLeftSection(String leftSection) {
        this.leftSection = leftSection;
    }

    public String getSplitDate() {
        return splitDate;
    }

    public void setSplitDate(String splitDate) {
        this.splitDate = splitDate;
    }

    public String getRightSection() {
        return rightSection;
    }

    public void setRightSection(String rightSection) {
        this.rightSection = rightSection;
    }

    public String getSplitType() {
        return splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }
}
