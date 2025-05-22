package com.suning.fab.loan.backup;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.hbase.Constants;
import com.suning.hbase.client.HBaseClient;

@Repository
public class HbaseConnection implements InitializingBean,DisposableBean{

	//用户名
	@Value("${hadoopUserName}")
	private String hadoopUserName;
	//登录名
	@Value("${hadoopGroupName}")
	private String hadoopGroupName;
	//zk连接地址
	@Value("${hadoopQuorum}")
	private String hadoopQuorum;
	//zk连接端口
	@Value("${hadoopPort}")
	private String hadoopPort;
	//项目名称
	@Value("${hadoopProjectName}")
	private String hadoopProjectName;
	//环境标识
	@Value("${hadoopEnvIsPrd}")
	private String hadoopEnvIsPrd;

	private HBaseClient suningHBaseClient;

	@Override
	public void afterPropertiesSet(){

		//初始化conf
		Configuration conf = HBaseConfiguration.create();

		//配置zk连接信息
		conf.set(HConstants.ZOOKEEPER_QUORUM, hadoopQuorum);
		conf.set(HConstants.ZOOKEEPER_CLIENT_PORT, hadoopPort);
		//缺省值，一般可以不配置
		conf.set(HConstants.ZOOKEEPER_ZNODE_PARENT, "/hbase");

		//环境设置，缺省为true，即PRD环境。设置为false为测试环境。
		conf.set(Constants.HA_CLIENT_ENV_IS_PRD, hadoopEnvIsPrd);

		//是否为HA模式，缺省为false
		conf.set(Constants.HA_CLIENT_MODE, "true");

		//设置为HA模式时，是否对Get请求开启hedge_read，缺省为false
		conf.set(Constants.HA_CLIENT_HEDGED_READ, "false");

		//设置HBase用户
		conf.set(Constants.HBASE_CLIENT_USER, hadoopUserName);

		//创建client，projectName为HBase管理员提供的项目名称
		try {
			suningHBaseClient = new HBaseClient(hadoopProjectName, conf);
			LoggerUtil.info("suningHBaseClient初始化成功");
		} catch (IOException e) {
			LoggerUtil.error("初始化suningHBaseClient失败IOException:", e);
		} catch (KeeperException e) {
			LoggerUtil.error("初始化suningHBaseClient失败KeeperException:", e);
			//报错记录，不抛异常
		}catch (NumberFormatException e){
			LoggerUtil.error("初始化suningHBaseClient失败NumberFormatException:", e);
		}catch (Exception e){
			LoggerUtil.error("初始化suningHBaseClient失败Exception:", e);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (suningHBaseClient != null) {
			try {
				suningHBaseClient.close();
			} catch (Exception e) {
				suningHBaseClient = null;
				LoggerUtil.error("关闭suningHBaseClient异常:", e);
			}
		}
	}

	public HBaseClient getSuningHBaseClient() {
		return suningHBaseClient;
	}

	public void setSuningHBaseClient(HBaseClient suningHBaseClient) {
		this.suningHBaseClient = suningHBaseClient;
	}

}
