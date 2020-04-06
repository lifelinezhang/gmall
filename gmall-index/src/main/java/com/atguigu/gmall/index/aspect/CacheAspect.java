package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class CacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.index.annotation.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;
        // 获取目标方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache annotation = method.getAnnotation(GmallCache.class);
        // 获取目标方法的返回值
        Class<?> returnType = method.getReturnType();
        // 获取注解中的缓存前缀
        String prefix = annotation.prefix();
        // 获取目标方法的参数列表
        Object[] args = joinPoint.getArgs();
        // 从缓存中查询数据
        String key = prefix + Arrays.asList(args).toString();
        result = this.cacheHit(key, returnType);
        if (result != null) {
            return result;
        }
        // 2、没有命中，加分布式锁
        RLock lock = this.redissonClient.getLock("lock" + Arrays.asList(args).toString());
        lock.lock();
        // 3、再次查询缓存，如果缓存中没有， 则执行目标方法
        result = this.cacheHit(key, returnType);
        if (result != null) {
            lock.unlock();  // 释放分布式锁
            return result;
        }
        // 执行目标方法
        result = joinPoint.proceed(joinPoint.getArgs());
        // 4、放入缓存，释放分布式锁
        int timeout = annotation.timeout();
        int random = annotation.random();
        this.redisTemplate.opsForValue().set(key, JSON.toJSONString(result), (int) (timeout + Math.random() * random), TimeUnit.MINUTES);
        lock.unlock();
        return result;
    }

    private Object cacheHit(String key, Class<?> returnType) {
        // 从缓存中查询数据
        String json = this.redisTemplate.opsForValue().get(key);
        // 1、命中，直接返回
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseObject(json, returnType);
        }
        return null;
    }
}
