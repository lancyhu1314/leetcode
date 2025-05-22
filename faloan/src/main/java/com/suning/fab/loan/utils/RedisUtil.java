package com.suning.fab.loan.utils;

import com.suning.framework.sedis.JedisAction;
import com.suning.framework.sedis.JedisClient;
import com.suning.framework.sedis.impl.JedisClientImpl;

import java.util.List;

import redis.clients.jedis.Jedis;

/**
 * redis工具类:使用了公司的sedis框架，通过scm动态配置
 *
 * 1、业务系统初始化Sedis框架时会先去SCM平台上解析配置文件， 并且跟SCM平台建立监听连接。 classpath下添加配置文件:scm.properties 
 * 2、Sedis 框架根据配置文件中的 Sentinel 信息，访问Sentinel 集群，获取 Redis 服务器信息，
 *      完成框架连接池的搭建。此外，框架还与 Sentinel 集群建立监 听链接，用于主从切换后、框架及时调整连接池。 
 * 3、Sentinel 集群中的每台 Sentinel
 *      服务器会监听所有 Redis 服务器， 当主服务器 出现宕机后，Sentinel 成员之间通过流言协议、投票协议，完成主从切换。 
 * 4、系统在访问 Redis 服务器时，框架会先路由到所在分片（shard） ，并根据读写分离策略，选择对应 Redis 服务器，通过连接池访问。
 */
public abstract class RedisUtil {

    protected static JedisClient jedisClient;

    // 配置文件redis.conf位于scm平台上
    static {
        
        
        jedisClient = new JedisClientImpl("redis.conf");
    }

    /*********************************** 配置数据redis操作 ***********************************/

    /**
     * 
     * 功能描述: <br>
     * 〈功能详细描述〉 将序列化对象值value关联到key， 如果key已经持有其他值，SET就覆写旧值，无视类型
     * 
     * @param key
     * @param value
     * @return
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public static String set(final String key, final String value) {
        return jedisClient.execute(new JedisAction<String>() {
            @Override
            public String doAction(Jedis jedis) {
                return jedis.set(key, value);
            }
        });
    }

    /**
     * 
     * 功能描述: <br>
     * 设置key值过期时间
     *
     * @param key
     * @param value
     * @param expireTime
     * @return
     * @since 1.0
     */
    public static String setWithExpire(final String key, final String value, final int expireTime) {
        return jedisClient.execute(new JedisAction<String>() {
            @Override
            public String doAction(Jedis jedis) {

                return jedis.setex(key, expireTime, value);
            }
        });
    }

    /**
     * 
     * 功能描述: <br>
     * 执行方法
     *
     * @return
     * @since 1.0
     */
    public static boolean execute(JedisAction<Boolean> jedisAction) {
        return jedisClient.execute(jedisAction);
    }

    /**
     * 
     * 功能描述: <br>
     * 获取当前redis的时间，转化成11111111.222222格式
     * 
     * @param key
     * @return
     * @since 1.0
     */
    public static double getTime() {
        List<String> timeList = jedisClient.execute(new JedisAction<List<String>>() {
            @Override
            public List<String> doAction(Jedis jedis) {
                return jedis.time();
            }
        });

        return Double.parseDouble(timeList.get(0) + "." + timeList.get(1));
    }

    /**
     * 
     * 功能描述: <br>
     * 获取当前redis的时间，转化成11111111222格式,以毫秒为度量单位
     * 
     * @param jedis
     * @return 返回当前系统时间
     * @since 1.0
     */
    public static final double getTime(Jedis jedis) {
        List<String> timeList = jedis.time();
        return Double.parseDouble(timeList.get(0) + "." + timeList.get(1));
    }

    /**
     * 
     * 功能描述: <br>
     * 获取当前redis的时间，转化成11111111222格式,以毫秒为度量单位
     * 
     * @param jedis
     * @return 返回当前系统时间
     * @since 1.0
     */
    public static final Double getInitTime(Jedis jedis) {
        List<String> timeList = jedis.time();
        return Double.parseDouble(timeList.get(0) + ".000");
    }
    
    /**
     * 
     * 功能描述: <br>
     * 获取当前redis的时间，以string类型返回
     * 
     * @param jedis
     * @return 返回当前系统时间
     * @since 1.0
     */
    public static final String getStringTime() {
        List<String> timeList = jedisClient.execute(new JedisAction<List<String>>() {
            @Override
            public List<String> doAction(Jedis jedis) {
                return jedis.time();
            }
        });

        return timeList.get(0);
    }

    /**
     * 
     * 功能描述: <br>
     * 获取初始化时间
     *
     * @return
     * @since 1.0
     */
    public static final Double getInitTime() {

        List<String> timeList = jedisClient.execute(new JedisAction<List<String>>() {
            @Override
            public List<String> doAction(Jedis jedis) {
                return jedis.time();
            }
        });

        return Double.parseDouble(timeList.get(0) + ".000");
    }

    /**
     * 
     * 功能描述: <br>
     * 〈功能详细描述〉 返回key所关联的序列化对象。如果key不存在则返回null
     * 
     * @param key
     * @return
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public static String get(final String key) {
        return jedisClient.execute(new JedisAction<String>() {
            @Override
            public String doAction(Jedis jedis) {
                return jedis.get(key);
            }
        });
    }

    /**
     * 
     * 功能描述: <br>
     * 〈功能详细描述〉 删除key所关联的序列化对象。
     * 
     * @param key
     * @return
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public static Long del(final String key) {
        return jedisClient.execute(new JedisAction<Long>() {
            @Override
            public Long doAction(Jedis jedis) {
                return jedis.del(key);
            }
        });
    }

    /**
     * 
     * 功能描述: <br>
     * 〈功能详细描述〉 删除哈希表key中的指定域，不存在的域将被忽略。
     * 
     * @param key
     * @param fields
     * @return
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public static Long hdel(final String key, final String... fields) {
        return jedisClient.execute(new JedisAction<Long>() {
            @Override
            public Long doAction(Jedis jedis) {
                return jedis.hdel(key, fields);
            }
        });
    }

    /**
     * 
     * 功能描述: <br>
     * 加1操作
     *
     * @param key
     * @return
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public static Long incr(final String key) {
        return jedisClient.execute(new JedisAction<Long>() {
            @Override
            public Long doAction(Jedis jedis) {
                return jedis.incr(key);
            }
        });
    }

    
    
    /**
     * 将值存入redis的list中
     * @param key 键值；
     * @return 序列化对象的二进制数据；
     */
    public static long push(final String key,final byte[] value) {
        return jedisClient.execute(new JedisAction<Long>() {
            @Override
            public Long doAction(Jedis jedis) {
                return jedis.lpush(key.getBytes(), value);
            }
        });
    }
    
    /**
     * 从list中获取值并删除
     * @param key 键值；
     * @return 序列化对象的二进制数据；
     */
    public static byte[] lpop(final String key) {
        return jedisClient.execute(new JedisAction<byte[]>() {
            @Override
            public byte[] doAction(Jedis jedis) {
                return jedis.lpop(key.getBytes());
            }
        });
    }
    
    /**
     * 获取list的长度
     * @param key 键值；
     * @return 
     */
    public static long llen(final String key) {
        return jedisClient.execute(new JedisAction<Long>() {
            @Override
            public Long doAction(Jedis jedis) {
                return jedis.llen(key.getBytes());
            }
        });
    }
    
    
}
