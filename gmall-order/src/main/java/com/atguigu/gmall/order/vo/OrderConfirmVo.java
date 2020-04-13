package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {

    private List<MemberReceiveAddressEntity> addresses;
    private List<OrderItemVo> orderItems;
    private Integer bounds;
    private String orderToken; // 防止订单重复提交

}
