package com.atguigu.gmall.order.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptors.LoginIntercepter;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private AmqpTemplate amqpTemplate;

    private static final String TOKEN_PREFIX = "order:token:";

    public OrderConfirmVo confirm() {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        UserInfo userInfo = LoginIntercepter.getUserInfo();
        Long userId = userInfo.getId();
        if (userId == null) {
            return null;
        }

        CompletableFuture<Void> addressCompletableFuture = CompletableFuture.runAsync(() -> {
            // 获取用户的收货地址列表，根据用户id查询收货地址列表
            Resp<List<MemberReceiveAddressEntity>> addressesResp = this.umsClient.queryAddressesByUserId(userId);
            List<MemberReceiveAddressEntity> memberReceiveAddressEntities = addressesResp.getData();
            orderConfirmVo.setAddresses(memberReceiveAddressEntities);
        }, threadPoolExecutor);


        // 获取购物车中选中的商品的信息 skuId count
        CompletableFuture<Void> bigSkuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Resp<List<Cart>> cartsResp = this.cartClient.queryCheckCartsByUserId(userId);
            List<Cart> cartList = cartsResp.getData();
            if (CollectionUtils.isEmpty(cartList)) {
                throw new OrderException("请勾选购物车商品");
            }
            return cartList;
        }, threadPoolExecutor).thenAcceptAsync(cartList -> {
            List<OrderItemVo> itemVos = cartList.stream().map(cart -> {
                OrderItemVo orderItemVo = new OrderItemVo();
                Long skuId = cart.getSkuId();

                CompletableFuture<Void> skuCompletableFuture = CompletableFuture.runAsync(() -> {
                    Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(skuId);
                    SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                    if (skuInfoEntity != null) {
                        orderItemVo.setWeight(skuInfoEntity.getWeight());
                        orderItemVo.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
                        orderItemVo.setPrice(skuInfoEntity.getPrice());
                        orderItemVo.setTitle(skuInfoEntity.getSkuTitle());
                        orderItemVo.setSkuId(skuId);
                        orderItemVo.setCount(cart.getCount());
                    }
                }, threadPoolExecutor);

                CompletableFuture<Void> saleAttrCompletableFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<SkuSaleAttrValueEntity>> salesAttrValuesResp = this.pmsClient.querySkuSalesAttrValuesBySkuId(skuId);
                    List<SkuSaleAttrValueEntity> attrValueEntities = salesAttrValuesResp.getData();
                    orderItemVo.setSaleAttrValues(attrValueEntities);
                }, threadPoolExecutor);

                CompletableFuture<Void> wareSkuCompletableFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<WareSkuEntity>> wareSkuResp = this.wmsClient.queryWareSkuBySkuId(skuId);
                    List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                    }
                }, threadPoolExecutor);
                CompletableFuture.allOf(skuCompletableFuture, saleAttrCompletableFuture, wareSkuCompletableFuture).join();
                return orderItemVo;
            }).collect(Collectors.toList());
            orderConfirmVo.setOrderItems(itemVos);
        }, threadPoolExecutor);


        // 查询用户信息，获取积分
        CompletableFuture<Void> memberCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<MemberEntity> memberEntityResp = this.umsClient.queryMemberById(userId);
            MemberEntity memberEntity = memberEntityResp.getData();
            orderConfirmVo.setBounds(memberEntity.getIntegration());
        }, threadPoolExecutor);

        // 生成一个唯一标志，防止重复提交（响应到页面一份，保存到redis一份）
        CompletableFuture<Void> tokenCompletableFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getIdStr();
            orderConfirmVo.setOrderToken(orderToken);
            this.redisTemplate.opsForValue().set(TOKEN_PREFIX + orderToken, orderToken);
        }, threadPoolExecutor);

        CompletableFuture.allOf(
                addressCompletableFuture, bigSkuCompletableFuture, memberCompletableFuture, tokenCompletableFuture
        ).join();
        return orderConfirmVo;
    }

    public void submit(OrderSubmitVo submitVo) {
        UserInfo userInfo = LoginIntercepter.getUserInfo();

        String orderToken = submitVo.getOrderToken();
        // 1、 防重复提交， 查询redis中有没有orderToken的信息，有则是第一次提交，放行并删除redis中的orderToken
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(TOKEN_PREFIX + orderToken), orderToken);
        if (flag == 0) {
            throw new OrderException("订单不可重复提交！");
        }
        // 2、校验总价格，如果总价一致则放行
        List<OrderItemVo> items = submitVo.getItems(); // 送货清单
        BigDecimal totalPrice = submitVo.getTotalPrice(); // 总价
        if (CollectionUtils.isEmpty(items)) {
            throw new OrderException("没有购买的商品，请到购物车中勾选商品");
        }
        // 获取实时总价信息
        BigDecimal currentTotalPrice = items.stream().map(item -> {
            Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(item.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity != null) {
                return skuInfoEntity.getPrice().multiply(new BigDecimal(item.getCount()));
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();
        // 判断实时总价和页面总价格是否一致
        if (currentTotalPrice.compareTo(totalPrice) != 0) {
            throw new OrderException("页面已过期，请刷新页面后重新下单！");
        }
        // 3、校验库存是否充足并锁定库存，一次性提示所有库存不够的商品信息
        List<SkuLockVo> lockVos = items.stream().map(item -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount());
            skuLockVo.setOrderToken(orderToken);
            return skuLockVo;
        }).collect(Collectors.toList());
        Resp<Object> wareResp = this.wmsClient.checkAndLockStore(lockVos);
        if (wareResp.getCode() != 0) {
            throw new OrderException(wareResp.getMsg());
        }
//        int i = 1 / 0;
        // 4、下单（创建订单及订单详情）
        try {
            submitVo.setUserId(userInfo.getId());
            Resp<OrderEntity> orderEntityResp = this.omsClient.saveOrder(submitVo);
            OrderEntity orderEntity = orderEntityResp.getData();
        } catch (Exception e) {
            // 如果下单失败，则需要回滚第三步的锁定库存，这里可以使用seata，但是seata存在性能问题
            // 这里就可以使用另一种解决方案：消息的最终一致
            // 即发送一条消息告诉wms去解锁这个订单锁定的库存
            this.amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "stock.unlock", orderToken);
            throw new OrderException("服务器错误，创建订单失败");
        }
        // 5、删除购物车（发送消息删除购物车）
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userInfo.getId());
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        map.put("skuIds", skuIds);
        this.amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "cart.delete", map);
    }


    public static void main(String[] args) {
        // 定时任务
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(
                () -> {
                    System.out.println("这是一个定时任务");
                }, 5l, 5l, TimeUnit.SECONDS);
    }
}
