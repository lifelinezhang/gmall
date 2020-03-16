package com.atguigu.gmall.pms.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.pms.dao.AttrAttrgroupRelationDao;
import com.atguigu.gmall.pms.dao.AttrDao;
import com.atguigu.gmall.pms.dao.AttrGroupDao;
import com.atguigu.gmall.pms.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrAttrgroupRelationDao relationDao;

    @Autowired
    private AttrDao attrDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo queryGroupByPage(QueryCondition params, Integer catId) {
        QueryWrapper<AttrGroupEntity> queryWrapper = new QueryWrapper<AttrGroupEntity>();
        queryWrapper.eq("catelog_id", catId);
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                queryWrapper
        );
        return new PageVo(page);
    }

    @Override
    public GroupVo queryGroupWithAttrsByGid(Long gid) {
        GroupVo groupVo = new GroupVo();
        // 根据attr_group_id查询group
        AttrGroupEntity attrGroupEntity = this.getById(gid);
        // 使用工具类赋值
        BeanUtils.copyProperties(attrGroupEntity, groupVo);
        // 根据attr_group_id查询attr_id
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("attr_group_id", gid);
        List<AttrAttrgroupRelationEntity> relationList = relationDao.selectList(wrapper);
        if(!CollectionUtils.isNotEmpty(relationList)) {
            return groupVo;
        }
        groupVo.setRelations(relationList);
        // 根据 attr_id 查询 attr
        List<Long> attrIds = relationList.stream().map(e -> e.getAttrId()).collect(Collectors.toList());
        List<AttrEntity> attrEntities = attrDao.selectBatchIds(attrIds);
        groupVo.setAttrEntities(attrEntities);
        return groupVo;
    }


}