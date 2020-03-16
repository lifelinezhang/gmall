package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.GroupVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 属性分组
 *
 * @author lifeline
 * @email 13001196631@163.com
 * @date 2020-03-14 12:27:10
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageVo queryPage(QueryCondition params);

    PageVo queryGroupByPage(QueryCondition params, Integer catId);

    GroupVo queryGroupWithAttrsByGid(Long gid);
}

