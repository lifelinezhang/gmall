package com.atguigu.gmall.ums.dao;

import com.atguigu.gmall.ums.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author lifeline
 * @email 13001196631@163.com
 * @date 2020-04-09 23:33:13
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
