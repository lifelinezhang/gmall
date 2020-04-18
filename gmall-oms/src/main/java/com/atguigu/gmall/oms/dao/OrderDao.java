package com.atguigu.gmall.oms.dao;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

/**
 * 订单
 *
 * @author lifeline
 * @email 13001196631@163.com
 * @date 2020-04-13 22:31:09
 */
@Mapper
@Component
public interface OrderDao extends BaseMapper<OrderEntity> {

    public int closeOrder(String orderToken);

}
