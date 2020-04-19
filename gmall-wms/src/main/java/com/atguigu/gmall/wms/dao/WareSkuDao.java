package com.atguigu.gmall.wms.dao;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商品库存
 *
 * @author lifeline
 * @email 13001196631@163.com
 * @date 2020-03-18 22:14:00
 */
@Mapper
@Component
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    List<WareSkuEntity> checkStore(@Param("skuId") Long skuId, @Param("count") Integer count);

    int lockStore(@Param("id") Long id, @Param("count") Integer count);

    int unLockStore(@Param("wareSkuId") Long wareSkuId, @Param("count") Integer count);

    int minusStore(@Param("wareSkuId") Long wareSkuId, @Param("count") Integer count);
}
