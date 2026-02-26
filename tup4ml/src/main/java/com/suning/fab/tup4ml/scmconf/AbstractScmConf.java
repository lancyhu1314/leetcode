package com.suning.fab.tup4ml.scmconf;

import com.suning.fab.fatapclient.util.LoggerUtil;
import com.suning.framework.scm.client.SCMClient;
import com.suning.framework.scm.client.SCMClientFactory;
import com.suning.framework.scm.client.SCMException;
import com.suning.framework.scm.client.SCMListener;
import com.suning.framework.scm.client.SCMNode;

/**
 * scm配置抽象，实现scm配置缓存到本地SCMNode
 *
 * @author 17060915
 * @date 2018年1月23日下午2:48:49
 * @since 1.0
 */
public abstract class AbstractScmConf {

    // scm数据节点，缓存scm配置到本地
    protected SCMNode node = null;

    // scm路径
    protected String scmPath;

    public String getScmPath() {
        return scmPath;
    }

    public void setScmPath(String scmPath) {
        this.scmPath = scmPath;
    }

    public SCMNode getNode() {
        return node;
    }

    /**
     * 从scm同步配置到本地
     */
    public void init() {

        if (scmPath == null || "".equals(scmPath)) {
            LoggerUtil.warn("scm config path is null!");
        }

        String configInfo = null;

        // 同步scm到本地，并读取整个配置数据
        try {
            if (node == null) {
                SCMClient scmClient = SCMClientFactory.getSCMClient();
                node = scmClient.getConfig(scmPath);
            }
            node.sync();
            configInfo = node.getValue();
        } catch (SCMException scmException) {
            LoggerUtil.error("{} SCMException: {}", scmPath, scmException);
        }

        LoggerUtil.info("scm config file {} get data: {}", scmPath, configInfo);

        if (configInfo == null || "".equals(configInfo)) {
            LoggerUtil.warn("scm config file {} get data is null!", scmPath);
            return;
        }

        formatReader();
        
        node.monitor(node.getValue(),scmConfigListener);
    }

    protected abstract void formatReader();
    
    protected abstract void reinitConf();
    
    private final SCMListener scmConfigListener = new SCMListener() {

        @Override
        public void execute(String oldValue, String newValue) {
            if (newValue == null || "".equals(newValue.trim())) {
                return;
            }
            formatReader();
        }
    };

}
