/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: SceneInfo.java
 * Author:   17060915
 * Date:     2018年10月10日 下午3:32:47
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.elfin;

/**
 * 场景信息类
 * @author 17060915
 * @date 2018年10月10日下午3:32:47
 * @since 1.0
 */
public class SceneInfo {
    
    /**
     * 场景描述
     */
    private String sceneDescribe;
    
    /**
     * 场景类型
     */
    private String sceneType = "1";
    

    public SceneInfo(String sceneDescribe){
        this.sceneDescribe = sceneDescribe;
    }


    /**  
     * 获取sceneDescribe  
     * @return sceneDescribe  
     */
    public String getSceneDescribe() {
        return sceneDescribe;
    }


    /**  
     * 设置sceneDescribe  
     * @param sceneDescribe  
     */
    public void setSceneDescribe(String sceneDescribe) {
        this.sceneDescribe = sceneDescribe;
    }


    /**  
     * 获取sceneType  
     * @return sceneType  
     */
    public String getSceneType() {
        return sceneType;
    }


    /**  
     * 设置sceneType  
     * @param sceneType  
     */
    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }
    
}
