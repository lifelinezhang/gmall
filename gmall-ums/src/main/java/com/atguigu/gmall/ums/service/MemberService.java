package com.atguigu.gmall.ums.service;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * 会员
 *
 * @author lifeline
 * @email 13001196631@163.com
 * @date 2020-04-09 23:33:13
 */
public interface MemberService extends IService<MemberEntity> {

    PageVo queryPage(QueryCondition params);

    Boolean checkData(String data, Integer type);

    void register(MemberEntity memberEntity, String code);

    MemberEntity queryUser(String username, String password);
}

