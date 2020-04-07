package com.atguigu.gmall.pms.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.pms.dao.AttrAttrgroupRelationDao;
import com.atguigu.gmall.pms.dao.AttrDao;
import com.atguigu.gmall.pms.dao.AttrGroupDao;
import com.atguigu.gmall.pms.dao.ProductAttrValueDao;
import com.atguigu.gmall.pms.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import com.atguigu.gmall.pms.vo.AttrAndGroupEntity;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
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

    @Autowired
    private ProductAttrValueDao attrValueDao;

    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageVo(page);
    }

    /**
     * Resp是我们自己封装的类，用于返回结果数据，对结果数据进行格式上的统一
     * PageVo也是我们自己封装的类，用于对查询的分页数据进行格式上的统一，传入的参数可以是list，也可以是Ipage
     * Ipage是mybatis-plus提供的分页查询结果类， 穿进去的参数包括一个Page类(IPage的实现类)，和一个QueryWrapper类（可选），
     * Query也是我们自己封装的类，用于对分页条件进行封装，同时过滤掉sql注入
     * 其中Page类用于进行条数和页数的限制，QueryWrapper类用于限制查询条件
     */
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

    @Override
    public List<GroupVo> queryGroupAndAttrsByCatId(Long catId) {
        List<AttrAndGroupEntity> respList = new ArrayList<>();
        // 先根据catid查询组名
        QueryWrapper<AttrGroupEntity> wrapper = new  QueryWrapper<AttrGroupEntity>();
        wrapper.eq("catelog_id", catId);
        List<AttrGroupEntity> list = this.list(wrapper);
//        for (int i = 0; i < list.size() ; i++) {
//            AttrAndGroupEntity attrAndGroupEntity = new AttrAndGroupEntity();
//            BeanUtils.copyProperties(list.get(i), attrAndGroupEntity);
//            respList.add(attrAndGroupEntity);
//        }
//        // 根据组名查询每个组对应的属性id有哪些
//        for(int i = 0; i < respList.size() ; i++) {
//            AttrAndGroupEntity attrAndGroupEntity = respList.get(i);
//            QueryWrapper<AttrAttrgroupRelationEntity> wrapper2 = new  QueryWrapper<AttrAttrgroupRelationEntity>();
//            wrapper2.eq("attr_group_id", attrAndGroupEntity.getAttrGroupId());
//            List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(wrapper2);
//            // 根据拿到的属性id查询属性
//            for (int j = 0; j < relationEntities.size(); j++) {
//                AttrEntity attrEntity = new AttrEntity();
//                QueryWrapper<AttrEntity> wrapper3 = new  QueryWrapper<AttrEntity>();
//                wrapper3.eq("attr_id", relationEntities.get(j).getAttrId());
//                List<AttrEntity> attrEntities = attrDao.selectList(wrapper3);
//                attrAndGroupEntity.setAttrEntities(attrEntities);
//            }
//        }
//        return respList;
        return list.stream().map(attrGroupEntity -> this.queryGroupWithAttrsByGid(attrGroupEntity.getAttrGroupId())).collect(Collectors.toList());
    }

    @Override
    public List<ItemGroupVo> queryItemGroupVoByCidAndSpuId(Long cid, Long spuId) {
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", cid));
        return attrGroupEntities.stream().map(group -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            itemGroupVo.setName(group.getAttrGroupName());
            // 查询规格参数及值
            List<AttrAttrgroupRelationEntity> relationEntities = this.relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", group.getAttrGroupId()));
            List<Long> attrIds = relationEntities.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());
            List<ProductAttrValueEntity> productAttrValueEntities = this.attrValueDao.selectList(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
            itemGroupVo.setBaseAttrs(productAttrValueEntities);
            return itemGroupVo;
        }).collect(Collectors.toList());

    }


}