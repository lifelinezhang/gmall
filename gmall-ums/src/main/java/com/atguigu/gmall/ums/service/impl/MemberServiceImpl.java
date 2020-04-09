package com.atguigu.gmall.ums.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.exception.MemberException;
import com.atguigu.gmall.ums.dao.MemberDao;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        switch (type) {
            case 1:
                wrapper.eq("username", data);
                break;
            case 2:
                wrapper.eq("mobile", data);
                break;
            case 3:
                wrapper.eq("email", data);
                break;
            default:
                return false;
        }
        return this.count(wrapper) == 0;
    }

    @Override
    public void register(MemberEntity memberEntity, String code) {
        // 校验手机验证码
        // 生成盐
        String salt = UUID.randomUUID().toString().substring(0, 6);
        memberEntity.setSalt(salt);
        // 加盐加密
        memberEntity.setPassword(DigestUtils.md5Hex(memberEntity.getPassword() + salt));
        // 数据初始化
        memberEntity.setGrowth(0);
        memberEntity.setIntegration(0);
        memberEntity.setLevelId(0l);
        memberEntity.setCreateTime(new Date());
        memberEntity.setStatus(1);
        // 新增用户
        this.save(memberEntity);
        // 删除redis中的手机验证码
    }

    @Override
    public MemberEntity queryUser(String username, String password) {
        // 根据用户名查询盐
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        MemberEntity memberEntity = this.getOne(wrapper);
        if (memberEntity == null) {
//            return null;
            throw new MemberException("用户名不存在");
        }
        // 加盐加密查询用户
        if (!StringUtils.equals(memberEntity.getPassword(), DigestUtils.md5Hex(password + memberEntity.getSalt()))) {
            throw new MemberException("密码错误");
        }
        return memberEntity;
    }

}