package com.atguigu.gmall.cart.listener;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CartListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String price_prefix = "gmall:sku:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART-ITEM-QUEUE", durable = "true"),
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId) {
        Resp<List<SkuInfoEntity>> skuResp = pmsClient.querySkusBySpuId(spuId);
        List<SkuInfoEntity> skuInfoEntities = skuResp.getData();
        skuInfoEntities.forEach(skuInfoEntity -> {
            redisTemplate.opsForValue().set(price_prefix + skuInfoEntity.getSkuId().toString(), skuInfoEntity.getPrice().toString());
        });
    }

}
