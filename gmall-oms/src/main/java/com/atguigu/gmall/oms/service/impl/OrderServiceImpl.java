package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.oms.dao.OrderDao;
import com.atguigu.gmall.oms.dao.OrderItemDao;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.service.OrderService;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private OrderItemDao itemDao;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageVo(page);
    }

    @Override
    @Transactional
    public OrderEntity saveOrder(OrderSubmitVo submitVo) {
        // 1、保存订单orderentity
        // 1.1、 地址相关信息
        MemberReceiveAddressEntity address = submitVo.getAddress();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverCity(address.getCity());
        // 1.2、 用户相关信息
        Resp<MemberEntity> memberEntityResp = this.umsClient.queryMemberById(submitVo.getUserId());
        MemberEntity memberEntity = memberEntityResp.getData();
        orderEntity.setMemberUsername(memberEntity.getUsername());
        orderEntity.setMemberId(memberEntity.getId());
        // 1.3、 清算每个商品赠送积分
        orderEntity.setIntegration(0);
        orderEntity.setGrowth(0);
        // 1.4、删除状态
        orderEntity.setDeleteStatus(0);
        // 1.5、订单状态
        orderEntity.setStatus(0);
        // 1.6、创建及修改时间
        orderEntity.setCreateTime(new Date());
        orderEntity.setModifyTime(orderEntity.getCreateTime());
        // 1.7、快递公司
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());
        // 1.8、数据来源
        orderEntity.setSourceType(1);
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setOrderSn(submitVo.getOrderToken());
        this.save(orderEntity);
        Long orderId = orderEntity.getId();
        // 2、保存订单详情orderitementity
        List<OrderItemVo> items = submitVo.getItems();
        items.forEach(item -> {
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setSkuId(item.getSkuId());
            Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(item.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            Resp<SpuInfoEntity> spuInfoEntityResp = this.pmsClient.querySpuById(skuInfoEntity.getSpuId());
            SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
            itemEntity.setSkuPrice(skuInfoEntity.getPrice());
            itemEntity.setSkuAttrsVals(JSON.toJSONString(item.getSaleAttrValues()));
            itemEntity.setCategoryId(skuInfoEntity.getCatalogId());
            itemEntity.setOrderId(orderId);
            itemEntity.setOrderSn(submitVo.getOrderToken());
            itemEntity.setSpuId(spuInfoEntity.getId());
            itemEntity.setSkuName(skuInfoEntity.getSkuName());
            itemEntity.setSkuPic(skuInfoEntity.getSkuDefaultImg());
            itemEntity.setSkuQuantity(item.getCount());
            itemEntity.setSpuName(spuInfoEntity.getSpuName());
            this.itemDao.insert(itemEntity);
        });

//        int i = 1 / 0;
        // 订单创建之后，响应之前，发送延时消息，达到定时关单的效果
        this.amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "order.ttl", submitVo.getOrderToken());
        return orderEntity;
    }

}