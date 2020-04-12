package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptors.LoginIntercepter;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private static final String key_prefix = "gmall:cart:";
    private static final String price_prefix = "gmall:sku:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    public void addCart(Cart cart) {
        String key = getLoginStatus();
        // 获取购物车，获取的是用户的hash操作对象
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        // 判断购物车中是否有该记录
        String skuId = cart.getSkuId().toString();
        Integer count = cart.getCount();
        if (hashOps.hasKey(skuId)) {
            // 有则更新数量
            // 获取购物车中的sku记录
            String cartJson = hashOps.get(skuId).toString();
            // 反序列化，更新数量
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount() + count);
        } else {
            // 没有则新增购物车记录
            cart.setCheck(true);
            // 查询sku相关的信息
            Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(cart.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity == null) {
                return;
            }
            cart.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
            cart.setPrice(skuInfoEntity.getPrice());
            cart.setTitle(skuInfoEntity.getSkuTitle());
            // 查询营销属性
            Resp<List<SkuSaleAttrValueEntity>> listResp = this.pmsClient.querySkuSalesAttrValuesBySkuId(cart.getSkuId());
            List<SkuSaleAttrValueEntity> saleAttrValueEntities = listResp.getData();
            cart.setSaleAttrValues(saleAttrValueEntities);
            // 查询营销信息
            Resp<List<SaleVo>> saleResp = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<SaleVo> saleVos = saleResp.getData();
            cart.setSales(saleVos);
            // 查询库存信息
            Resp<List<WareSkuEntity>> wareResp = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResp.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
            }
            // 额外保存商品的当前价格到redis数据库
            this.redisTemplate.opsForValue().set(price_prefix + skuId, skuInfoEntity.getPrice().toString());
        }
        // 重新写入redis
        hashOps.put(skuId, JSON.toJSONString(cart));
    }

    private String getLoginStatus() {
        String key = key_prefix;
        // 获取登录状态
        UserInfo userInfo = LoginIntercepter.getUserInfo();
        if (userInfo.getId() != null) {
            key += userInfo.getId();
        } else {
            key += userInfo.getUserKey();
        }
        return key;
    }

    public List<Cart> queryCarts() {
        // 获取登录状态
        UserInfo userInfo = LoginIntercepter.getUserInfo();
        // 查询未登录的购物车
        String unLoginKey = key_prefix + userInfo.getUserKey();
        BoundHashOperations<String, Object, Object> unLoginHashOps = this.redisTemplate.boundHashOps(unLoginKey);
        List<Object> cartJsonList = unLoginHashOps.values();
        List<Cart> unLoginCarts = null;
        if (!CollectionUtils.isEmpty(cartJsonList)) {
            unLoginCarts = cartJsonList.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                // 查询当前价格
                String priceString = this.redisTemplate.opsForValue().get(price_prefix + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(priceString));
                return cart;
            }).collect(Collectors.toList());
        }
        // 判断是否登录， 未登录直接返回
        if (userInfo.getId() == null) {
            return unLoginCarts;
        }
        // 登录，购物车同步
        String loginKey = key_prefix + userInfo.getId();
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unLoginCarts)) {
            unLoginCarts.forEach(cart -> {
                Integer count = cart.getCount();
                if (loginHashOps.hasKey(cart.getSkuId().toString())) {
                    String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                    cart = JSON.parseObject(cartJson, Cart.class);
                    cart.setCount(cart.getCount() + count);
                }
                loginHashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
            });
            // 同步完成之后要删除未登录状态的购物车
            this.redisTemplate.delete(unLoginKey);
        }
        // 查询登录状态的购物车
        List<Object> loginCartJsonList = loginHashOps.values();
        return loginCartJsonList.stream().map(cartJson -> {
            Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
            // 查询当前价格
            String priceString = this.redisTemplate.opsForValue().get(price_prefix + cart.getSkuId());
            cart.setCurrentPrice(new BigDecimal(priceString));
            return cart;
        }).collect(Collectors.toList());
    }

    public void updateCart(Cart cart) {
        // 获取登录状态
        String key = getLoginStatus();
        // 获取购物车
        BoundHashOperations<String, Object, Object> boundHashOps = this.redisTemplate.boundHashOps(key);
        Integer count = cart.getCount();
        // 判断更新的这条记录，在购物车中是否存在
        if (boundHashOps.hasKey(cart.getSkuId().toString())) {
            String cartJson = boundHashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            boundHashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
        }
    }

    public void deleteCart(Long skuId) {
        // 获取登录状态
        String key = getLoginStatus();
        // 获取购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId.toString())) {
            hashOps.delete(skuId.toString());
        }
    }
}
