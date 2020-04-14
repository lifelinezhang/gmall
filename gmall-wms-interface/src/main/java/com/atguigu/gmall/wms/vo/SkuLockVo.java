package com.atguigu.gmall.wms.vo;

import lombok.Data;

@Data
public class SkuLockVo {
    private Long skuId;
    private Integer count;
    private Long wareSkuId; // 库存表中该商品对应的id（非skuid）
    private Boolean lock; // 商品锁定状态
}
