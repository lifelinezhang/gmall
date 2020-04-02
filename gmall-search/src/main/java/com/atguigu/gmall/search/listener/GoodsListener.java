package com.atguigu.gmall.search.listener;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttr;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GoodsRepository goodsRepository;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "gmall-search-queue",
                    durable = "true"),
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE",
                    type = ExchangeTypes.TOPIC,
                    ignoreDeclarationExceptions = "true",
                    durable = "true"),
            key = {"item.insert", "item.update"}
    ))
    public void listener(long spuId) {
        Resp<List<SkuInfoEntity>> skusResp = pmsClient.querySkusBySpuId(spuId);
        List<SkuInfoEntity> skuInfoEntities = skusResp.getData();
        if(!CollectionUtils.isEmpty(skuInfoEntities)) {
            // 把sku转换为goods对象，导入索引库
            List<Goods> goodsList = skuInfoEntities.stream().map(skuInfoEntity -> {
                Goods goods = new Goods();
                // 查询搜索属性及值
                Resp<List<ProductAttrValueEntity>> attrValueResp = pmsClient.querySearchAttrValueBySpuId(spuId);
                List<ProductAttrValueEntity> attrValueEntities = attrValueResp.getData();
                if(!CollectionUtils.isEmpty(attrValueEntities)) {
                    List<SearchAttr> searchAttrs = attrValueEntities.stream().map(productAttrValueEntity -> {
                        SearchAttr searchAttr = new SearchAttr();
                        searchAttr.setAttrId(productAttrValueEntity.getAttrId());
                        searchAttr.setAttrName(productAttrValueEntity.getAttrName());
                        searchAttr.setAttrValue(productAttrValueEntity.getAttrValue());
                        return searchAttr;
                    }).collect(Collectors.toList());
                    goods.setAttrs(searchAttrs);
                }
                // 查询品牌
                Resp<BrandEntity> brandEntityResp = pmsClient.queryBrandsById(skuInfoEntity.getBrandId());
                BrandEntity brandEntity = brandEntityResp.getData();
                if(brandEntity != null) {
                    goods.setBrandId(skuInfoEntity.getBrandId());
                    goods.setBrandName(brandEntity.getName());
                }
                // 查询分类
                Resp<CategoryEntity> categoryEntityResp = pmsClient.queryCategoryById(skuInfoEntity.getCatalogId());
                CategoryEntity categoryEntity = categoryEntityResp.getData();
                if(categoryEntity != null) {
                    goods.setCategoryId(skuInfoEntity.getCatalogId());
                    goods.setCategoryName(categoryEntity.getName());
                }
                Resp<SpuInfoEntity> spuInfoEntityResp = pmsClient.querySpuById(spuId);
                SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
                goods.setCreateTime(spuInfoEntity.getCreateTime());
                goods.setPic(skuInfoEntity.getSkuDefaultImg());
                goods.setPrice(skuInfoEntity.getPrice());
                goods.setSale(0l);
                goods.setSkuId(skuInfoEntity.getSkuId());
                // 查询库存
                Resp<List<WareSkuEntity>> listResp = wmsClient.queryWareSkuBySkuId(skuInfoEntity.getSkuId());
                List<WareSkuEntity> wareSkuEntities = listResp.getData();
                if(!CollectionUtils.isEmpty(wareSkuEntities)) {
                    boolean flag = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                    goods.setStore(flag);
                }

                goods.setTitle(skuInfoEntity.getSkuTitle());
                return goods;
            }).collect(Collectors.toList());
            goodsRepository.saveAll(goodsList);
        }

    }


}
