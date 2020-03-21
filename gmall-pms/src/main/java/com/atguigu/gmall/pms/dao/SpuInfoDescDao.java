package com.atguigu.gmall.pms.dao;

import com.atguigu.gmall.pms.entity.SpuInfoDescEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

/**
 * spu信息介绍
 * 
 * @author lifeline
 * @email 13001196631@163.com
 * @date 2020-03-14 12:27:10
 */
@Mapper
@Component
public interface SpuInfoDescDao extends BaseMapper<SpuInfoDescEntity> {
	
}
