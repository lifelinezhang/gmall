package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.CategoryVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 商品三级分类
 *
 * @author lifeline
 * @email 13001196631@163.com
 * @date 2020-03-14 12:27:10
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageVo queryPage(QueryCondition params);

    List<CategoryVo> querySubCategories(Long pid);
}

