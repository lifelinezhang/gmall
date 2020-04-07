package com.atguigu.gmall.item.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.sms.vo.SaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ItemService {

    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallPmsClient pmsClient;

    public ItemVo queryItemVo(Long skuId) {
        ItemVo itemVo = new ItemVo();
        // 设置skuid
        itemVo.setSkuId(skuId);
        // 1、根据id查询sku
        Resp<SkuInfoEntity> skuResp = this.pmsClient.querySkuById(skuId);
        SkuInfoEntity skuInfoEntity = skuResp.getData();
        if (skuInfoEntity == null) {
            return itemVo;
        }
        itemVo.setSkuTitle(skuInfoEntity.getSkuTitle());
        itemVo.setSubTitle(skuInfoEntity.getSkuSubtitle());
        itemVo.setPrice(skuInfoEntity.getPrice());
        itemVo.setWeight(skuInfoEntity.getWeight());
        // 获取spuid
        Long spuId = skuInfoEntity.getSpuId();
        // 2、根据sku中的spuid查询spu
        itemVo.setSpuId(spuId);
        Resp<SpuInfoEntity> spuResp = this.pmsClient.querySpuById(spuId);
        SpuInfoEntity spuInfoEntity = spuResp.getData();
        if (spuInfoEntity != null) {
            itemVo.setSpuName(spuInfoEntity.getSpuName());
        }
        // 3、根据skuid查询图片列表
        Resp<List<SkuImagesEntity>> skuImagesResp = this.pmsClient.querySkuImagesBySkuId(skuId);
        List<SkuImagesEntity> skuImagesEntities = skuImagesResp.getData();
        itemVo.setPics(skuImagesEntities);
        // 4、根据sku中的brandid和categoryid查询品牌和分类
        Resp<BrandEntity> brandEntityResp = this.pmsClient.queryBrandsById(skuInfoEntity.getBrandId());
        BrandEntity brandEntity = brandEntityResp.getData();
        itemVo.setBrandEntity(brandEntity);
        Resp<CategoryEntity> categoryEntityResp = this.pmsClient.queryCategoryById(skuInfoEntity.getCatalogId());
        CategoryEntity categoryEntity = categoryEntityResp.getData();
        itemVo.setCategoryEntity(categoryEntity);
        // 5、根据skuid查询营销信息
        Resp<List<SaleVo>> salesResp = this.smsClient.querySalesBySkuId(skuId);
        List<SaleVo> saleVoList = salesResp.getData();
        itemVo.setSales(saleVoList);
        // 6、根据skuid查询库存
        Resp<List<WareSkuEntity>> wareResp = this.wmsClient.queryWareSkuBySkuId(skuId);
        List<WareSkuEntity> wareSkuEntities = wareResp.getData();
        itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
        // 7、根据spuid查询所有skuid，然后再去查询所有的销售属性
        Resp<List<SkuSaleAttrValueEntity>> saleAttrValueResp = this.pmsClient.querySkuSalesAttrValuesBySpuId(spuId);
        List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = saleAttrValueResp.getData();
        itemVo.setSalAttrs(skuSaleAttrValueEntities);
        // 8、根据spuid查询商品描述（海报）
        Resp<SpuInfoDescEntity> spuInfoDescEntityResp = this.pmsClient.querySpuDescBySpuId(spuId);
        SpuInfoDescEntity spuInfoDescEntity = spuInfoDescEntityResp.getData();
        if (spuInfoDescEntity != null) {
            String decript = spuInfoDescEntity.getDecript();
            String[] split = StringUtils.split(decript, ",");
            itemVo.setImages(Arrays.asList(split));
        }
        // 9、根据cateid和spuid查询组及组下的规格参数（带值的）
        Resp<List<ItemGroupVo>> itemGroupResp = this.pmsClient.queryItemGroupVoByCidAndSpuId(skuInfoEntity.getCatalogId(), spuId);
        List<ItemGroupVo> itemGroupVos = itemGroupResp.getData();
        itemVo.setGroups(itemGroupVos);
        return itemVo;
    }
}
