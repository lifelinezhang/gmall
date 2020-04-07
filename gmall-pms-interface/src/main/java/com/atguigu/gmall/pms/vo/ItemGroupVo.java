package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import lombok.Data;

import java.util.List;

@Data
public class ItemGroupVo {
    private String name;
    private List<ProductAttrValueEntity> baseAttrs;
}
