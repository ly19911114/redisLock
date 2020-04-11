package com.ly.redislock.service;

/**
 * @author LiuYang
 * @date 2020/4/11
 */
public interface RedisService {
    Boolean lock(String key,String uuid);
    Boolean unlock(String key,String uuid);
}
