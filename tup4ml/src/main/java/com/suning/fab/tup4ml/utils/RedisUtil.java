package com.suning.fab.tup4ml.utils;

import java.util.List;

import com.suning.framework.sedis.JedisAction;
import com.suning.framework.sedis.JedisClient;
import com.suning.framework.sedis.impl.JedisClientImpl;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

public abstract class RedisUtil {
	protected static JedisClient jedisClient = new JedisClientImpl("redis.conf");

	private RedisUtil() {
		throw new IllegalStateException("RedisUtil class");
	}

	/**
	 * 获取redis数据库的所有key值，下次调用时需要传入cursor，第一次调用为0；
	 * @param cursor redis的scan游标；
	 * @return 返回ScanResult<String>对象，包含这次遍历的key值；
	 */
	public static ScanResult<String> scan(final String cursor, final String match) {
		return jedisClient.execute(new JedisAction<ScanResult<String>>() {
			@Override
			public ScanResult<String> doAction(Jedis jedis) {
				if(null != match) {
					ScanParams scanParams = new ScanParams();  
					scanParams.match(match);  
					return jedis.scan(cursor, scanParams);
				}else {
					return jedis.scan(cursor);
				}
			}
		});
	}


	/**
	 * 获取redis存储的序列化对象；
	 * @param key 键值；
	 * @return 序列化对象的二进制数据；
	 */
	public static List<byte[]> hvals(final String key) {
		return jedisClient.execute(new JedisAction<List<byte[]>>() {
			@Override
			public List<byte[]> doAction(Jedis jedis) {
				return jedis.hvals(key.getBytes());
			}
		});
	}


	/**
	 * 获取redis存储的序列化对象；
	 * @param key 键值；
	 * @return 序列化对象的二进制数据；
	 */
	public static byte[] get(final String key) {
		return jedisClient.execute(new JedisAction<byte[]>() {
			@Override
			public byte[] doAction(Jedis jedis) {
				return jedis.get(key.getBytes());
			}
		});
	}
	
	/**
	 * 获取redis存储的序列化对象；
	 * @param key 键值；
	 * @return 序列化对象的二进制数据；
	 */
	public static String getStr(final String key) {
	    return jedisClient.execute(new JedisAction<String>() {
	        @Override
	        public String doAction(Jedis jedis) {
	            return jedis.get(key);
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
	
	/**
	 * 删除key值
	 * @param key 键值；
	 * @return 
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
	 * 存储序列化对象到redis；
	 * @param key 键值；
	 * @param value 序列化对象的二进制数据；
	 * @return redis的状态码；
	 */
	public static String set(final String key, final byte[] value) {
		return jedisClient.execute(new JedisAction<String>() {
			@Override
			public String doAction(Jedis jedis) {
				return jedis.set(key.getBytes(), value);
			}
		});
	}
	
	/**
	 * 存储序列化对象到redis；
	 * @param key 键值；
	 * @param value 序列化对象的二进制数据；
	 * @return redis的状态码；
	 */
	public static String setex(final String key, final int seconds, final String value) {
	    return jedisClient.execute(new JedisAction<String>() {
	        @Override
	        public String doAction(Jedis jedis) {
	            return jedis.setex(key, seconds, value);
	        }
	    });
	}
	
	/**
     * 失效时间
     * @param key 键值；
     * @return 
     */
    public static long exprie(final String key,int seconds) {
        return jedisClient.execute(new JedisAction<Long>() {
            @Override
            public Long doAction(Jedis jedis) {
                return jedis.expire(key, seconds);
            }
        });
    }
    
    /**
     * 失效时间 单元毫秒
     * @param key 键值；
     * @return 
     */
    public static long pexprie(final String key,int milliseconds) {
        return jedisClient.execute(new JedisAction<Long>() {
            @Override
            public Long doAction(Jedis jedis) {
                return jedis.pexpire(key, milliseconds);
            }
        });
    }
    
    /**
     * 
     * @param key 键值；
     * @return 
     */
    public static long setnx(final String key,String value) {
        return jedisClient.execute(new JedisAction<Long>() {
            @Override
            public Long doAction(Jedis jedis) {
                return jedis.setnx(key, value);
            }
        });
    }
    
    /**
     * 
     * 功能描述: <br>
     * 获取当前redis的时间，转化当前毫秒数
     * 
     * @param key
     * @return
     * @since 1.0
     */
    public static Long getTime() {
        List<String> timeList = jedisClient.execute(new JedisAction<List<String>>() {
            @Override
            public List<String> doAction(Jedis jedis) {
                return jedis.time();
            }
        });
        
        return Long.parseLong(timeList.get(0)) * 1000 + Long.parseLong(timeList.get(1))/1000;
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
    
    public static String set(final String key, final String value, final String nxxx,
            final String expx, final long time)  {
        return jedisClient.execute(new JedisAction<String>() {
            @Override
            public String doAction(Jedis jedis) {
                return jedis.set(key,value, nxxx,expx,time);
            }
        });
    }
}
