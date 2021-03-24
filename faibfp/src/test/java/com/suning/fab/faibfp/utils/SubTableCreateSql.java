package com.suning.fab.faibfp.utils;

import com.suning.fab.mulssyn.utils.VarChecker;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author 19043955
 * @Date 2019/10/15 16:31
 * @Version 1.0
 */
public class SubTableCreateSql {

    public static void main(String[] args) {

//        String path = "F:/需求文档/FABDOC/2_文档汇总/170207_贷款_苏宁小贷账务核心2.0/产品开发测试/新模型开发/金融账务核心-贷款系统表结构.xlsx";
        String path = "F:/需求文档/FABDOC/2_文档汇总/170207_贷款_苏宁小贷账务核心2.0/产品开发测试/贷款预处理系统/贷款预处理系统表结构.xlsx";
        // 从哪张表开始【
        int startTables = 1;
        // 到哪张表结束）
        int tableNums = 5;
        String schema = "FALOAUSR";

        List<Table> tableInfo = readTableFrom(path, tableNums, startTables);

//        makeCreateSql(tableInfo, schema);
        makeSingleCreateSql(tableInfo, schema);

        // 统一刷新关闭流
        for (FileWriter writer : writerMap.values()) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static void makeSingleCreateSql(List<Table> tableInfo, String schema){

        for (Table table : tableInfo) {
            // 获取索引sql
            StringBuilder queueBuilder = getIndexSql(schema, table, "");
            // 建表sql()
            StringBuilder sql = createSql(schema, table, "");
            sql.append(queueBuilder);
            outputToFile(1, sql);
        }
    }

    /**
     * 创建sql及comment
     *
     * @param tableInfo
     * @param schema
     */
    private static void makeCreateSql(List<Table> tableInfo, String schema) {

        // int tableNum = 1;

        for (Table table : tableInfo) {
            for (int i = 0; i < 512; i++) {
                // 获取索引sql
                StringBuilder queueBuilder = getIndexSql(schema, table, String.valueOf(i));
                // 建表sql()
                StringBuilder sql = createSql(schema, table, String.valueOf(i));
                sql.append(queueBuilder);

//                StringBuilder sql = getDropSql(schema, table, i);
                outputToFile(i, sql);
            }
        }
    }

    private static StringBuilder getDropSql(String schema, Table table, int tableNum) {

        StringBuilder builder = new StringBuilder("DROP TABLE ");
        builder.append(schema + "." + table.getTableName() + tableNum);
        builder.append(";");
        return builder;

    }

    /**
     * 将sql写入到文件中
     *
     * @param tableId
     * @param sql
     */
    public static void outputToFile(int tableId, StringBuilder sql) {

        FileWriter writer = getWriter(tableId);
        try {
            writer.write(sql.toString());
            writer.write("\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static Map<Integer, FileWriter> writerMap = new HashMap<>();

    public static FileWriter getWriter(int tableId) {

        int dataBaseId = getDataBaseByTableId(tableId);
        FileWriter writer = null;
        if (writerMap.containsKey(dataBaseId)) {
            writer = writerMap.get(dataBaseId);
            return writer;
        }
        String fileName = "d:/sql/" + dataBaseId + ".sql";
        try {
            writer = new FileWriter(fileName, true);
            writerMap.put(dataBaseId, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return writer;
    }

    public static int getDataBaseByTableId(int tableId) {
        /*** 9个库算法 ****/

        /*int j;
        if (tableId == 255 || tableId == 511) {
            j = 9;
        } else {
            j = tableId % 8 + 1; // 计算放在那个库里
        }*/

        /*** 2个库算法 ****/
//         int j = tableId % 2 + 1;

        return 1;
    }

    private static StringBuilder createSql(String schema, Table table, String tableNum) {
        // CREATE TABLE Persons(Id_P int,LastName varchar(255),FirstName varchar(255),Address varchar(255),City
        // varchar(255))
        StringBuilder builder = new StringBuilder("CREATE TABLE ");
        builder.append(schema + "." + table.getTableName() + tableNum);
        builder.append("(");

        StringBuilder commentBuilder = new StringBuilder("COMMENT ON TABLE ");
        commentBuilder.append(schema + "." + table.getTableName() + tableNum);
        commentBuilder.append(" IS '" + table.getTableName_CH() + "';\r\n");

        for (TableStructure structure : table.getStructures()) {

            builder.append(" " + structure.getFieldName() + " " + structure.getFieldType());
            if (VarChecker.isEmpty(structure.getFieldLong())) {
                builder.append(",");
            } else {
                builder.append("(" + structure.getFieldLong() + "),");
            }
            commentBuilder.append("COMMENT ON COLUMN ");
            commentBuilder.append(schema + "." + table.getTableName() + tableNum);
            commentBuilder.append("." + structure.getFieldName());
            commentBuilder.append(" IS " + "'" + structure.getFieldComment() + "'" + ";\r\n");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(")");
        builder.append(" in " + table.getTableSpace() + " INDEX IN TBS_IDX;");
        builder.append("\r\n");
        builder.append(commentBuilder);
        // System.out.println(builder.toString());
        return builder;
    }

    /**
     * 创建索引
     *
     * @param schema
     * @param table
     * @return
     */
    private static StringBuilder getIndexSql(String schema, Table table, String tableNum) {
        StringBuilder queueBuilder = new StringBuilder("CREATE UNIQUE INDEX ");
        queueBuilder.append(schema + "." + table.getTableName() + tableNum);
        queueBuilder.append("_IDX ON " + schema + "." + table.getTableName() + tableNum);
        queueBuilder.append("(");
        for (String key : table.getUniqueFields()) {
            queueBuilder.append(" " + key + ",");
        }
        queueBuilder.deleteCharAt(queueBuilder.length() - 1);
        queueBuilder.append(");\r\n");
        int i = 0;
        for (List<String> queList : table.getQueueFields()) {
            if (null == queList) {
                break;
            }
            i++;
            queueBuilder.append("CREATE INDEX ");
            queueBuilder.append(schema + "." + table.getTableName() + tableNum);
            queueBuilder.append("_IDX" + i + " ON " + schema + "." + table.getTableName() + tableNum);
            queueBuilder.append("(");
            for (String key : queList) {
                queueBuilder.append(" " + key + ",");
            }
            queueBuilder.deleteCharAt(queueBuilder.length() - 1);
            queueBuilder.append(");\r\n");
        }
        return queueBuilder;
    }

    /**
     * 从execl中获取表结构、唯一索引，普通索引信息
     *
     * @param path
     * @param tableNums
     * @param startTables
     * @return
     */
    private static List<Table> readTableFrom(String path, int tableNums, int startTables) {
        FileInputStream fis = null;
        // 表结构
        List<Table> map = new ArrayList<>();
        try {
            fis = new FileInputStream(path);
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            // 读取execel文件，获取表结构及索引信息
            for (int i = startTables; i < tableNums; i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                Table table = new Table();
                // 普通索引1
                List<String> list1 = null;
                // 普通索引2
                List<String> list2 = null;
                // 普通索引3
                List<String> list3 = null;
                table.setTableName(sheet.getSheetName());
                table.setTableName_CH(sheet.getRow(0).getCell(1).getStringCellValue());
                table.setTableSpace(sheet.getRow(0).getCell(5).getStringCellValue());
                for (int p = 2; p <= sheet.getLastRowNum(); p++) {
                    XSSFRow row = sheet.getRow(p);
                    TableStructure structure = new TableStructure();
                    structure.setFieldName(row.getCell(0).getStringCellValue());
                    structure.setFieldType(row.getCell(1).getStringCellValue());
                    structure.setFieldLong(row.getCell(2).toString());
                    structure.setNullFlag(!"no".equalsIgnoreCase(row.getCell(3).getStringCellValue().trim()));
                    structure.setDefaultValue(row.getCell(4).getStringCellValue());
                    structure.setFieldComment(row.getCell(5).getStringCellValue());
                    table.setStructures(structure);
                    if ("YES".equalsIgnoreCase(row.getCell(6).getStringCellValue().trim())) {
                        table.setUniqueFields(row.getCell(0).getStringCellValue());
                    }
                    if ("YES".equalsIgnoreCase(row.getCell(9).getStringCellValue().trim())) {
                        if (null == list1) {
                            list1 = new ArrayList<>();
                        }
                        list1.add(row.getCell(0).getStringCellValue());
                    }
                    if ("YES".equalsIgnoreCase(row.getCell(10).getStringCellValue().trim())) {
                        if (null == list2) {
                            list2 = new ArrayList<>();
                        }
                        list2.add(row.getCell(0).getStringCellValue());
                    }
                    if ("YES".equalsIgnoreCase(row.getCell(11).getStringCellValue().trim())) {
                        if (null == list3) {
                            list3 = new ArrayList<>();
                        }
                        list3.add(row.getCell(0).getStringCellValue());
                    }
                }
                if (null != list1)
                    table.setQueueFields(list1);
                if (null != list2)
                    table.setQueueFields(list2);
                if (null != list3)
                    table.setQueueFields(list3);
                map.add(table);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return map;
    }
}
