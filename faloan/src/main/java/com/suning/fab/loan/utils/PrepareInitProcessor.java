package com.suning.fab.loan.utils;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.suning.fab.tup4j.utils.LoggerUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 预加载配置项类
 */
@Component
@Scope("singleton")
public class PrepareInitProcessor implements ApplicationListener<ContextRefreshedEvent> {
    /**
     * 执行超时时间，以秒为单位
     */
    protected static final int EXEC_LIMIT = 10 * 1000;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // root application context 没有parent ，它包含其他子容器，
        // 因此这里做整个spring bean容器完成初始化后的工作
        if (event.getApplicationContext().getParent() == null) {
            // 这边只做预加载，不作任何处理
            
            String time = null;
            
            /**
             * 尝试重试5次，超过5次就不再重试
             * 这边对redis连接也做超时判断，不然会一直堵塞容器。
             */
            for(int i = 0 ;i < 5; i++){
                if(null != time){
                    break;
                }
                
                Future<String> f1 = null;
                try {
                    f1 = AcctStatistics.executor.submit(new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            return RedisUtil.getStringTime();
                        }
                    });
                    
                    // future将在execLimit毫秒之后取结果
                    time = f1.get((EXEC_LIMIT + i * 1000), TimeUnit.MILLISECONDS);
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    LoggerUtil.warn("InterExcep:{}",e);
                    f1.cancel(true);// 中断执行此任务的线程
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    LoggerUtil.warn("ExecuExcep:{}",e);
                    f1.cancel(true);
                } catch (TimeoutException e) {
                    AcctStatistics.loggerMethod("timeout", e);
                    f1.cancel(true);
                }catch(Exception e){
                    LoggerUtil.warn("futurExcep:{}",e);
                    
                    if(f1 != null){
                        f1.cancel(true);
                    }
                }
                
            }
                
            LoggerUtil.info("PrepareInitProcessor init finished");
        }
    }
}
