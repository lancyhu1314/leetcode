/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: SceneUtil.java
 * Author:   17060915
 * Date:     2018年11月19日 下午7:29:46
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.utils;

import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.elfin.SceneInfo;
import com.suning.fab.tup4ml.service.ServiceTemplate;

/**
 * 场景工具类
 * @author 17060915
 * @date 2018年11月19日下午7:29:46
 * @since 1.0
 */
public class SceneUtil {

    private SceneUtil(){
    }
    
    /**
     * 
     * 功能描述: <br>
     * 根据sceneInfo是否为null，判断如何获取场景
     *
     * @param sceneInfo 场景对象
     * @param cls 当前交易类
     * @return 场景
     * @since 1.0
     */
    public static String getScene(SceneInfo sceneInfo, Class<? extends ServiceTemplate> cls){
        if(null == sceneInfo){
            String name = GetPropUtil.getProperty("transcode." + cls.getSimpleName());
            return null == name? cls.getSimpleName() : name;
        }
        return sceneInfo.getSceneDescribe();
    }
    
    /**
     * 
     * 功能描述: <br>
     * 在交易入口将场景存入ThreadLocal中
     *
     * @param sceneInfo
     * @param cls
     * @since 1.0
     */
    public static void putInThreadLocal(SceneInfo sceneInfo, Class<? extends ServiceTemplate> cls){
        ThreadLocalUtil.set(PlatConstant.PLATCONST.TRANS_SCENE, getScene(sceneInfo, cls));
    }

    /**
     *
     * 功能描述: <br>
     * 将交易流水存入ThreadLocal中
     *
     * @param outSerialNum 交易流水号
     * @since 1.0
     */
    public static void putSerialNoInThreadLocal(String outSerialNum){
        ThreadLocalUtil.set(PlatConstant.PLATCONST.TRANS_SCENE_OUT_SERIAL_NUM, outSerialNum);
    }
    
    /**
     * 
     * 功能描述: <br>
     * 从threadlocal中获取在交易入口存入ThreadLocal中的场景
     *
     * @return 场景
     * @since 1.0
     */
    public static String getSceneFromThreadLocal(){
        String scene = (String) ThreadLocalUtil.get(PlatConstant.PLATCONST.TRANS_SCENE);
        return null == scene ? "" : scene;
    }

    /**
     *
     * 功能描述: <br>
     * 从threadlocal中获取在交易入口存入的流水号
     *
     * @return 交易流水
     * @since 1.0
     */
    public static String getSerialNumFromThreadLocal(){
        return (String)ThreadLocalUtil.get(PlatConstant.PLATCONST.TRANS_SCENE_OUT_SERIAL_NUM);
    }
}
