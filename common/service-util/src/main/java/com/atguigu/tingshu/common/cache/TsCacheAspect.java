package com.atguigu.tingshu.common.cache;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.login.TsLogin;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author atguigu-mqx
 * @ClassName TsCacheAspect
 * @description: TODO
 * @date 2026年03月03日
 * @version: 1.0
 */
@Component
@Aspect
@Slf4j
public class TsCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 缓存切面
     * @param point
     * @param tsCache
     * @return
     * @throws Throwable
     */
    @Around("@annotation(tsCache)")
    public Object cacheAspect(ProceedingJoinPoint point, TsCache tsCache) throws Throwable {
        // 做分布式锁业务处理;
        //  组成缓存的key;
        String prefix = tsCache.prefix();
        String dataKey = prefix + Arrays.asList(point.getArgs()).toString();
        try {
            //  从缓存中获取数据
            Object object = this.redisTemplate.opsForValue().get(dataKey);
            //  判断缓存中是否有数据
            if (null == object) {
                //  缓存中没有数据：
                log.info("缓存中没有数据，开始查询数据库");
                //  定义一个锁的key;
                String dataLocKey = "lock:" + dataKey;
                //  调用上锁方法;
                RLock lock = redissonClient.getLock(dataLocKey);
                lock.lock();
                try {
                    //  再次查询缓存!
                    object = this.redisTemplate.opsForValue().get(dataKey);
                    if (null != object) {
                        log.info("缓存中有数据，开始返回数据");
                        return object;
                    }
                    //  业务逻辑; 查询数据库：
                    object = point.proceed();
                    //  判断是否存在数据：
                    if (null == object){
                        log.info("数据库中没有数据，返回null");
                        this.redisTemplate.opsForValue().set(dataKey, new Object(), RedisConstant.ALBUM_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        return new Object();
                    }
                    log.info("数据库中有数据，开始返回数据");
                    this.redisTemplate.opsForValue().set(dataKey, object, RedisConstant.ALBUM_TIMEOUT, TimeUnit.SECONDS);
                    return object;
                } finally {
                    //  解锁
                    lock.unlock();
                }
            }else {
                log.info("缓存中有数据，开始返回数据");
                //  返回数据
                return object;
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        return point.proceed();
    }
}
