package com.suning.fab.loan.utils;

import com.suning.fab.tup4j.utils.LoggerUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

    /**
     * 文件的路径以csv结尾
     *
     * @param filePath
     * @param insertList
     */
    public synchronized static void insertStringToFile(String filePath, List<String> insertList) {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LoggerUtil.info("异常"+e);
            }
        }
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(file, true);
            bw = new BufferedWriter(fw);
            if (insertList.size() > 0) {
                for (int i = 0; i < insertList.size(); i++) {
                    bw.append(insertList.get(i));
                    bw.append("\n");
                }
            }
        } catch (IOException e) {
            LoggerUtil.info("异常"+e);
        } finally {
            try {
                if(bw!=null){
                    bw.close();
                }
                if(fw!=null){
                    fw.close();
                }
            } catch (IOException e) {
                LoggerUtil.info("异常"+e);
            }
        }
    }

}
