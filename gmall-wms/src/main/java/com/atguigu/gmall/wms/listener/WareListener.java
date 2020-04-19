package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WareListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WareSkuDao wareSkuDao;

    private static final String KEY_PREFIX = "stock:lock:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "WMS-UNLOCK-QUEUE", durable = "true"),
            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unLockListener(String orderToken) {
        String lockJson = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if (StringUtils.isEmpty(lockJson)) {
            return;
        }
        List<SkuLockVo> skuLockVos = JSON.parseArray(lockJson, SkuLockVo.class);
        skuLockVos.forEach(skuLockVo -> {
            this.wareSkuDao.unLockStore(skuLockVo.getWareSkuId(), skuLockVo.getCount());
        });
        // 因为oms和wms中都进行了解锁库存的操作，所以当一个地方解锁之后，需要删除redis中的数据，防止重复解锁
        this.redisTemplate.delete(KEY_PREFIX + orderToken);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "WMS-MINUS-QUEUE", durable = "true"),
            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void minusStoreListener(String orderToken) {
        String lockJson = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        List<SkuLockVo> skuLockVos = JSON.parseArray(lockJson, SkuLockVo.class);
        skuLockVos.forEach(skuLockVo -> {
            this.wareSkuDao.minusStore(skuLockVo.getWareSkuId(), skuLockVo.getCount());
        });
    }


    @Scheduled(fixedDelay = 1000)
    public void test() {
        System.out.println("这是一个周期定时任务");
    }

}
