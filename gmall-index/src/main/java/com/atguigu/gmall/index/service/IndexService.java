package com.atguigu.gmall.index.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient gmallPmsClient;

    public List<CategoryEntity> queryLvl1Categories() {
        Resp<List<CategoryEntity>> listResp = gmallPmsClient.getCatagoriesByPidOrLevel(1, null);
        return listResp.getData();
    }

    public List<CategoryVo> querySubCategories(Long pid) {
        Resp<List<CategoryVo>> listResp = gmallPmsClient.querySubCategories(pid);
        return listResp.getData();
    }
}
