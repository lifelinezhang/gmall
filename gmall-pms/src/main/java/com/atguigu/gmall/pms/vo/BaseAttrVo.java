package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Data
public class BaseAttrVo extends ProductAttrValueEntity {

    /**
     * 这里要有这个set方法的原因是：从前端传过来的值得名字叫做valueSelected，
     * 而数据库中对应的字段叫做AttrValue，传值是通过set方法传的，所以重写set方法
     * 即可把传进来的值放到别的字段里面
     *
     * valueSelected的值是一个集合，所以还需要把集合转换为字符串
     * @param selected
     */
    public void setValueSelected(List<String> selected) {
        if(CollectionUtils.isEmpty(selected)) {
            return;
        }
        this.setAttrValue(StringUtils.join(selected, ","));
    }

}
