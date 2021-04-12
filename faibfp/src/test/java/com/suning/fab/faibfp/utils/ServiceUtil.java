package com.suning.fab.faibfp.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.suning.fab.mulssyn.utils.LoggerUtil;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：
 *
 * @param <K>
 * @param <V>
 * @Author
 * @Date
 * @see
 */
public class ServiceUtil<K, V> {
    /**
     * 还款计划 比对
     * 根据既往比对实际
     *
     * @param
     * @param
     */
    public static void match(Object actualObj, Object expectObj) {
        String actual = actualObj.toString();
        String expect = expectObj.toString();
        //转json
        actual = actual.substring(actual.indexOf("["), actual.indexOf("]") + 1);
        JSONArray actualArray = JSONArray.parseArray(actual);
        expect = expect.substring(expect.indexOf("["), expect.indexOf("]") + 1);
        JSONArray expectArray = JSONArray.parseArray(expect);
        assertEquals("size:" + expectArray.size(), "size:" + actualArray.size());
        for (int i = 0; i < expectArray.size(); i++) {
            JSONObject expectObject = expectArray.getJSONObject(i);
            JSONObject actualObject = actualArray.getJSONObject(i);
            for (Map.Entry<String, Object> expectEntry : expectObject.entrySet()) {
                if (!expectEntry.getValue().equals(actualObject.get(expectEntry.getKey())))
                    LoggerUtil.error(expectEntry.getKey() + " expect:" + expectEntry.getValue() + " actual:" + actualObject.get(expectEntry.getKey()));
                assertEquals(expectEntry.getValue(), actualObject.get(expectEntry.getKey()));


            }
        }
    }


    /**
     * 还款明细对比
     *
     * @param
     * @param
     */
    public static void match_repayList(Map<String, Object> repayCalMap, Map<String, Object> repayMap) {
        assertEquals(repayCalMap.get("prinAmt").toString().trim(), repayMap.get("prinAmt").toString().trim());
        assertEquals(repayCalMap.get("intAmt").toString().trim(), repayMap.get("intAmt").toString().trim());
        assertEquals(repayCalMap.get("forfeitAmt").toString().trim(), repayMap.get("forfeitAmt").toString().trim());
//        assertEquals(repayCalMap.get("endFlag").toString().trim(),repayMap.get("endFlag").toString().trim());
    }
}
