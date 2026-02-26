package com.suning.fab.loan.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;

/**
 * 〈JSON 工具类〉
 * 〈功能详细描述〉：
 *
 * @Author 18049705
 * @Date Created in 16:32 2019/2/27
 * @see
 */
public class FaloanJson {

    private JSONObject json;
    public FaloanJson(){
        this.json = new JSONObject();
    }
    public FaloanJson(JSONObject json){
        this.json = json;
    }

    /**
     * String 转化成 FaloanJson(实际上是一个JSONObject对象)
     * @param text String类型的JSON对象
     * @return
     */
    public static FaloanJson parseObject(String text){
        return new FaloanJson(JSONObject.parseObject(text));
    }

    /**
     * 获取json中的一个值
     * @param clazz 返回对象的类型
     * @param firstKey 必传一个key值
     * @param otherKeys 可以传多个key值
     * @param <T> 返回泛型
     * @return
     */
    public <T> T get(Class<T> clazz,String firstKey,String... otherKeys){
         if(otherKeys.length>0){
             JSONObject jsonObject= json.getJSONObject(firstKey);
             int i=0;
             for(;i<otherKeys.length-1;i++){
                 jsonObject = jsonObject.getJSONObject(otherKeys[i]) ;
             }
             return jsonObject.getObject(otherKeys[i],clazz) ;
         }else{
             return  json.getObject(firstKey,clazz) ;
         }
    }
    /**
     * 获取json中的一个字符串
     * @param firstKey 必传一个key值
     * @param otherKeys 可以传多个key值
     * @return String 返回字符串
     */
    public String getString(String firstKey,String... otherKeys){
        return get(String.class,firstKey, otherKeys);
    }
    /**
     * 获取json中的一个BigDecimal
     * @param firstKey 必传一个key值
     * @param otherKeys 可以传多个key值
     * @return BigDecimal
     */
    public BigDecimal getBigDecimal(String firstKey,String... otherKeys){
        return get(BigDecimal.class,firstKey, otherKeys);

    }
    /**
     * 获取json中的一个double
     * @param firstKey 必传一个key值
     * @param otherKeys 可以传多个key值
     * @return double
     */
    public double getDouble(String firstKey,String... otherKeys){
        return get(double.class,firstKey, otherKeys);

    }
    /**
     * 获取FaloanJson(实际上是一个JSONObject对象)
     * @param firstKey 必传一个key值
     * @param otherKeys 可以传多个key值
     * @return JSONArray
     */
    public FaloanJson getFaloanJson(String firstKey,String... otherKeys) {
         JSONObject jsonObject= json.getJSONObject(firstKey);
         for(int i=0;i<otherKeys.length;i++){
             jsonObject = jsonObject.getJSONObject(otherKeys[i]) ;
         }
         return new FaloanJson(jsonObject);
    }

    /**
     * 获取JSONArray
     * @param firstKey 必传一个key值
     * @param otherKeys 可以传多个key值
     * @return JSONArray
     */
    public JSONArray getJSONArray(String firstKey,String... otherKeys) {
         JSONArray result;
         if(otherKeys.length>0){
             JSONObject jsonObject= json.getJSONObject(firstKey);
             int i=0;
             for(;i<otherKeys.length-1;i++){
                 jsonObject = jsonObject.getJSONObject(otherKeys[i]) ;
             }
             result = jsonObject.getJSONArray(otherKeys[i]) ;
         }else{
             result = json.getJSONArray(firstKey) ;
         }
        return result;
    }

    /**
     * 对json修改（相同的 keys）或者添加
     * @param value 值
     * @param firstKey  必传一个key值
     * @param otherKeys 可以传多个key值
     * @return
     */
    public void put(Object value,String firstKey,String... otherKeys ) {
        if(otherKeys.length==0){
            json.put(firstKey, value);
            return ;
        }
        FaloanJson obj = getFaloanJson(firstKey);
        //无firstKey 节点时 新建一个
        if(null==obj.json||obj.json.isEmpty()){
            obj = new FaloanJson();
            json.put(firstKey,obj.json);
        }
        //递归调用put 方法  key值减少了 firstKey
        String[] temp = new String[otherKeys.length-1];
        for(int i = 1; i<otherKeys.length;i++){
            temp[i-1] = otherKeys[i];
        }
        obj.put(value,otherKeys[0],temp );
    }
    @Override
    public String toString(){
        return json.toString();
}

    public JSONObject getJson() {
        return json;
    }

    public void setJson(JSONObject json) {
        this.json = json;
    }

    /**
     * 移除特定的key
     * @param key
     */
    public void removeKey(String key){
        json.remove(key);
    }
}
