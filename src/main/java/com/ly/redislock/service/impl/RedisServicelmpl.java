package com.ly.redislock.service.impl;



import com.ly.redislock.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


import java.util.Collections;

/**
 * @author LiuYang
 * @date 2020/4/11
 */
@Service("redisService")
public class RedisServicelmpl implements RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisServicelmpl.class);

    @Autowired
    private JedisPool jedisPool;

    private static final String LOCK_SUCCESS = "OK";
    private static final Long RELEASE_SUCCESS = 1L;

    //锁过期时间
    private static final long expireTime = 30000;

    //获取锁的超时时间
    private long timeout = 900000;

    @Override
    public Boolean lock(String key, String uuid) {
        Jedis jedis = null;
        long start = System.currentTimeMillis();
        try {
            jedis=jedisPool.getResource();
            while (true) {
                //使用setnx是为了保持原子性
                String result = jedis.set(key, uuid, "NX", "PX", expireTime);

                if (LOCK_SUCCESS.equals(result)) {
                    return true;
                }
                //在timeout时间内仍未获取到锁，则获取失败
                long time = System.currentTimeMillis() - start;
                if (time > start) {
                    return false;
                }


            }
        } catch (Exception e) {
            log.error("redis竞争锁失败！");
            throw e;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Boolean unlock(String key, String uuid) {
        Jedis jedis = null;
        try {
            jedis=jedisPool.getResource();
            //lua脚本
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(key), Collections.singletonList(uuid));
            if (RELEASE_SUCCESS.equals(result)){
                return true;
            }
            return false;
        }catch (Exception w ){
            log.error("解锁失败");
            throw w;
        }finally {
            if (jedis!=null){
                jedis.close();
            }
        }
    }
}
