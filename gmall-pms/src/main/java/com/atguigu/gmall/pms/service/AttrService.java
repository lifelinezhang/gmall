package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.AttrIncloudGroupEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * 商品属性
 *
 * @author lifeline
 * @email 13001196631@163.com
 * @date 2020-03-14 12:27:10
 */
public interface AttrService extends IService<AttrEntity> {

    PageVo queryPage(QueryCondition params);

    PageVo queryByType(QueryCondition queryCondition, Long cateId, Long type);

    boolean saveAttr(AttrIncloudGroupEntity attr);
}

