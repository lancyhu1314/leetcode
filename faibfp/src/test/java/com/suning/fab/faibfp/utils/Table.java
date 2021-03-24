package com.suning.fab.faibfp.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 19043955
 * @Date 2019/10/15 19:05
 * @Version 1.0
 */
public class Table {

    private String tableName;

    private String tableName_CH;

    private String tableSpace;

    private List<TableStructure> structures = new ArrayList<>();

    private List<String> uniqueFields = new ArrayList<>();

    private List<List<String>> queueFields = new ArrayList<>();

    public String getTableName_CH() {
        return tableName_CH;
    }

    public void setTableName_CH(String tableName_CH) {
        this.tableName_CH = tableName_CH;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setStructures(TableStructure structure){
        structures.add(structure);
    }

    public void setUniqueFields(String uniqueField) {
        this.uniqueFields.add(uniqueField);
    }

    public void setQueueFields(List<String> queueField) {
        queueFields.add(queueField);
    }

    public List<TableStructure> getStructures() {
        return structures;
    }

    public List<String> getUniqueFields() {
        return uniqueFields;
    }

    public List<List<String>> getQueueFields() {
        return queueFields;
    }

    public String getTableSpace() {
        return tableSpace;
    }

    public void setTableSpace(String tableSpace) {
        this.tableSpace = tableSpace;
    }
}
