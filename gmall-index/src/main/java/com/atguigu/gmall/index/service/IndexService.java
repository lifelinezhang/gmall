package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String key_prefix = "index:cates:";

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private GmallPmsClient gmallPmsClient;

    public List<CategoryEntity> queryLvl1Categories() {
        Resp<List<CategoryEntity>> listResp = gmallPmsClient.getCatagoriesByPidOrLevel(1, null);
        return listResp.getData();
    }

    @GmallCache(prefix = "index:cates:", timeout = 7200, random = 100)
    public List<CategoryVo> querySubCategories(Long pid) {
        // 查询数据库
        Resp<List<CategoryVo>> listResp = gmallPmsClient.querySubCategories(pid);
        List<CategoryVo> CategoryVos = listResp.getData();
        return CategoryVos;
    }

    // 这个是不使用注解情况下的写法
    public List<CategoryVo> querySubCategories2(Long pid) {
        // 1、判断缓存中有没有
        String cateJson = redisTemplate.opsForValue().get(key_prefix + pid);
        // 2、 有的话，直接返回
        if (!StringUtils.isEmpty(cateJson)) {
            return JSON.parseArray(cateJson, CategoryVo.class);
        }
        // 这个地方设置分布式锁之后需要再进行一次缓存校验，判断其他请求是否已经把数据取出来了
        RLock lock = this.redissonClient.getLock("lock" + pid);
        lock.lock();
        // 1、判断缓存中有没有
        String cateJson2 = redisTemplate.opsForValue().get(key_prefix + pid);
        // 2、 有的话，直接返回
        if (!StringUtils.isEmpty(cateJson2)) {
            lock.unlock();
            return JSON.parseArray(cateJson2, CategoryVo.class);
        }
        // 3、 查询完成后放入缓存
        Resp<List<CategoryVo>> listResp = gmallPmsClient.querySubCategories(pid);
        List<CategoryVo> CategoryVos = listResp.getData();
        redisTemplate.opsForValue().set(key_prefix + pid, JSON.toJSONString(CategoryVos), 7 + new Random().nextInt(5), TimeUnit.DAYS);
        // 释放锁
        lock.unlock();
        return CategoryVos;
    }

    /**
     * 锁
     * 1、互斥性
     * 2、获取锁并设置过期时间防止死锁，这两个操作需要具备原子性;释放锁也要具备原子性
     * 3、但是释放时间如果小于业务逻辑执行时间的话，则会释放下一个请求的锁，需要做到解铃还须系铃人
     */
    public void testLock2() {
        // 给自己的锁生成一个唯一标志
        String uuid = UUID.randomUUID().toString();
        // 执行redis的setnx命令(在设置的时候同时设置时间是为了防止死锁)
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 5, TimeUnit.SECONDS);
        // 判断是否拿到锁
        if (lock) {
            String numStr = redisTemplate.opsForValue().get("num");
            if (StringUtils.isEmpty(numStr)) {
                return;
            }
            int num = Integer.parseInt(numStr);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));
            // 释放锁资源，其他请求才能执行（LUA脚本）
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script), Arrays.asList("lock"), uuid);
//            if(StringUtils.equals(this.redisTemplate.opsForValue().get("lock"), uuid)) {
//                this.redisTemplate.delete("lock");
//            }
        } else {
            // 重试获取锁
            this.testLock2();
        }
    }


    public void testLock() {
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();
        String numStr = redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(numStr)) {
            return;
        }
        int num = Integer.parseInt(numStr);
        this.redisTemplate.opsForValue().set("num", String.valueOf(++num));
        lock.unlock();
    }

    public String testRead() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10l, TimeUnit.SECONDS);
        String test = this.redisTemplate.opsForValue().get("test");
//        rwLock.readLock().unlock();
        return test;
    }

    public String testWrite() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10l, TimeUnit.SECONDS);
        this.redisTemplate.opsForValue().set("test", UUID.randomUUID().toString());
//        rwLock.readLock().unlock();
        return "写入了数据";
    }


    public String testLatch() throws InterruptedException {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.trySetCount(5);
        latch.await();
        return "主业务开始执行";
    }

    public String testCountdown() {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.countDown();
        return "分支业务执行了一次";
    }
}
