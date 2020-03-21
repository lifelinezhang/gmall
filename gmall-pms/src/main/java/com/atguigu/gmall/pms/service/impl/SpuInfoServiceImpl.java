package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.dao.SkuInfoDao;
import com.atguigu.gmall.pms.dao.SpuInfoDescDao;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.BaseAttrVo;
import com.atguigu.gmall.pms.vo.SkuInfoVo;
import com.atguigu.gmall.pms.vo.SpuInfoVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.mysql.cj.util.TimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescDao spuInfoDescDao;

    @Autowired
    private ProductAttrValueService attrValueService;

    @Autowired
    private SkuInfoDao skuInfoDao;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService saleAttrValueService;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo querySpuPage(QueryCondition queryCondition, Long catId) {
        QueryWrapper<SpuInfoEntity> spuInfoEntityQueryWrapper = new QueryWrapper<>();
        if(catId != 0) {
            spuInfoEntityQueryWrapper.eq("catalog_id", catId);
        }
        String key = queryCondition.getKey();
        if(!StringUtils.isEmpty(key)) {
            spuInfoEntityQueryWrapper.and(t -> t.eq("id", key).or().like("spu_name", key));
        }
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(queryCondition),
                spuInfoEntityQueryWrapper
        );
        return new PageVo(page);
    }

    // 大保存方法

    /**
     * 事务隔离级别、传播行为分别对应Transactional注解里面的Isolation和Propagation
     * 回滚策略对应：
     *     noRollbackForClassName/noRollbackFor: 发生什么异常不回滚（运行时异常都会回滚）
     *     rollbackFor:   发生什么异常回滚（编译时异常都不回滚）
     *     timeout:       超时回滚（单位为秒）
     *     readonly:      只读事务(只能查询，不能增删改)
     * @param spuInfoVo
     */
    @Override
    @Transactional(readOnly = true)
    public void bigSave(SpuInfoVo spuInfoVo) throws FileNotFoundException {
        // 1、保存spu相关的3张表
        // 1.1、 保存pms_spu_info信息
        Long spuId = saveSpuInfo(spuInfoVo);
        // 1.2、 保存pms_spu_info_desc信息
        spuInfoDescService.saveSpuInfoDesc(spuInfoVo, spuId);

        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        new FileInputStream((new File("xxxx")));
//        int i = 1 / 0;
        // 1.3、 保存pms_product_attr_value信息
        saveBaseAttrValue(spuInfoVo, spuId);
        // 2、保存sku相关的3张表和营销信息相关的三张表
        saveSkuAndSale(spuInfoVo, spuId);
    }


    private void saveSkuAndSale(SpuInfoVo spuInfoVo, Long spuId) {
        List<SkuInfoVo> skus = spuInfoVo.getSkus();
        if(CollectionUtils.isEmpty(skus)) {
            return;
        }
        skus.forEach(skuInfoVo -> {
            // 2.1、 保存pms_sku_info信息
            skuInfoVo.setSpuId(spuId);
            skuInfoVo.setSkuCode(UUID.randomUUID().toString());
            skuInfoVo.setBrandId(spuInfoVo.getBrandId());
            skuInfoVo.setCatalogId(spuInfoVo.getCatalogId());
            List<String> images = skuInfoVo.getImages();
            // 设置默认图片
            if(!CollectionUtils.isEmpty(images)) {
                skuInfoVo.setSkuDefaultImg(StringUtils.isNotBlank(skuInfoVo.getSkuDefaultImg()) ? skuInfoVo.getSkuDefaultImg() : images.get(0));
            }
            skuInfoDao.insert(skuInfoVo);
            Long skuId = skuInfoVo.getSkuId();
            // 2.2、 保存pms_sku_images信息
            if(!CollectionUtils.isEmpty(images)) {
                List<SkuImagesEntity> skuImagesEntities = images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setImgUrl(image);
                    skuImagesEntity.setSkuId(skuId);
                    // 设置是否是默认图片
                    skuImagesEntity.setDefaultImg(StringUtils.equals(skuInfoVo.getSkuDefaultImg(), image) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(skuImagesEntities);
            }
            // 2.3、 保存pms_sale_attr_value信息
            List<SkuSaleAttrValueEntity> saleAttrs = skuInfoVo.getSaleAttrs();
            if(!CollectionUtils.isEmpty(saleAttrs)) {
                saleAttrs.forEach(skuSaleAttrValueEntity -> {
                    skuSaleAttrValueEntity.setSkuId(skuId);
                });
                saleAttrValueService.saveBatch(saleAttrs);
            }
            // 3、保存营销信息的3张表(feign远程调用sms保存)
            // 3.1、 保存sms_sku_bounds信息
            // 3.1、 保存sms_sku_ladder信息
            // 3.1、 保存sms_sku_full_reduction信息
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuInfoVo, skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            gmallSmsClient.saveSale(skuSaleVo);
        });
    }

    private void saveBaseAttrValue(SpuInfoVo spuInfoVo, Long spuId) {
        List<BaseAttrVo> baseAttrs = spuInfoVo.getBaseAttrs();
        if(!CollectionUtils.isEmpty(baseAttrs)) {
            List<ProductAttrValueEntity> collect = baseAttrs.stream().map(baseAttrVo -> {
                ProductAttrValueEntity attrValueEntity = baseAttrVo;
                attrValueEntity.setSpuId(spuId);
                return attrValueEntity;
            }).collect(Collectors.toList());
            attrValueService.saveBatch(collect);
        }
    }



    private Long saveSpuInfo(SpuInfoVo spuInfoVo) {
        spuInfoVo.setCreateTime(new Date());
        spuInfoVo.setUodateTime(spuInfoVo.getCreateTime());
        this.save(spuInfoVo);
        return spuInfoVo.getId();
    }

}


/**
 注： 要演示事务的传播行为，则子事务需要在不同的sercice中，同时主方法和内部调用方法上都必须加上事务注解；
 在同一个service中也可以使用事务，条件是需要使用代理对象去调用子方法
 大保存方法测试数据：
 {
 "spuName": "一加7 pro",
 "brandId": 6,
 "catalogId": 225,
 "publishStatus": 1,
 "spuDescription": "垃圾手机也很好",
 "spuImages": ["https://ggmall.oss-cn-shanghai.aliyuncs.com/2020-03-21/d5f8d9bd-6a38-4b60-8b95-d9b69de50f18_download.jpg"],
 "baseAttrs": [{
 "attrId": 25,
 "attrName": "大萨达222",
 "valueSelected": ["23231"]
 }, {
 "attrId": 41,
 "attrName": "摄像头数量",
 "valueSelected": ["2"]
 }, {
 "attrId": 33,
 "attrName": "电池",
 "valueSelected": ["4000"]
 }, {
 "attrId": 34,
 "attrName": "屏幕",
 "valueSelected": ["7"]
 }],
 "skus": [{
 "attr_24": "8g",
 "price": "4899",
 "stock": 0,
 "growBounds": "200",
 "buyBounds": "500",
 "work": [0, 1, 1, 0],
 "fullCount": "2",
 "discount": "90",
 "fullPrice": "1000",
 "reducePrice": "10",
 "fullAddOther": 1,
 "images": ["https://ggmall.oss-cn-shanghai.aliyuncs.com/2020-03-21/e66d124c-84db-40c7-891b-efa0340c610b_download.jpg"],
 "skuName": "一加7 pro 8g,黑色,256",
 "skuDesc": "一加7 pro 8g,黑色,256 xxxx",
 "skuTitle": "【现货速发】一加 OnePlus 7 Pro 游戏手机 星雾蓝 8G+256G全网通【明星单品】",
 "skuSubtitle": "【现货速发+京东物流】限量抢购！赠运费险+全国联保一加7Tpro限量抢购！",
 "weight": "200",
 "attr_30": "黑色",
 "attr_35": "256",
 "ladderAddOther": 1,
 "saleAttrs": [{
 "attrId": "24",
 "attrValue": "8g"
 }, {
 "attrId": "30",
 "attrValue": "黑色"
 }, {
 "attrId": "35",
 "attrValue": "256"
 }]
 }, {
 "attr_24": "8g",
 "price": "3899",
 "stock": 0,
 "growBounds": "200",
 "buyBounds": "600",
 "work": [1, 0, 0, 1],
 "fullCount": "3",
 "discount": "80",
 "fullPrice": "2000",
 "reducePrice": "500",
 "fullAddOther": 1,
 "images": ["https://ggmall.oss-cn-shanghai.aliyuncs.com/2020-03-21/19997cb9-596d-4606-883d-a0e394a367d0_download.jpg"],
 "skuName": "一加7 pro 8g,黑色,128g",
 "skuDesc": "阿斯顿发士大夫撒地方",
 "skuTitle": "【现货速发】一加 OnePlus 7 Pro 游戏手机 曜岩灰 8G+256G全网通【明星单品】",
 "skuSubtitle": "【现货速发+京东物流】限量抢购！赠运费险+全国联保一加7Tpro限量抢购！",
 "weight": "300",
 "attr_30": "黑色",
 "attr_35": "128g",
 "ladderAddOther": 1,
 "saleAttrs": [{
 "attrId": "24",
 "attrValue": "8g"
 }, {
 "attrId": "30",
 "attrValue": "黑色"
 }, {
 "attrId": "35",
 "attrValue": "128g"
 }]
 }]
 }

 */