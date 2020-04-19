package com.atguigu.gmall.oms.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    // 声明死信队列，并且将死信队列与、死信队列的消息接收交换机、进行绑定
    @Bean("ORDER-TTL-QUEUE")
    public Queue queue() {
        Map<String, Object> map = new HashMap<>();
        // 这个交换机是死信队列将要转发的那个交换机
        map.put("x-dead-letter-exchange", "GMALL-ORDER-EXCHANGE");
        // 这个key是死信队列将要转发的那个交换机，往下面的队列进行转发的topic的key
        // 跟convertAndSend方法中的key是一个意思
        map.put("x-dead-letter-routing-key", "order.dead");
        map.put("x-message-ttl", 1200000);
        return new Queue("ORDER-TTL-QUEUE", true, false, false, map);
    }

    // 绑定死信队列与   死信队列的消息来源交换机
    @Bean("ORDER-TTL-BINDING")
    public Binding ttlBinding() {
        return new Binding("ORDER-TTL-QUEUE", Binding.DestinationType.QUEUE, "GMALL-ORDER-EXCHANGE", "order.ttl", null);
    }


    @Bean("ORDER-DEAD-QUEUE")
    public Queue dlQueue() {
        return new Queue("ORDER-DEAD-QUEUE", true, false, false, null);
    }

    @Bean("ORDER-DEAD-BINDING")
    public Binding deadBinding() {
        return new Binding("ORDER-DEAD-QUEUE", Binding.DestinationType.QUEUE, "GMALL-ORDER-EXCHANGE", "order.dead", null);
    }
}
