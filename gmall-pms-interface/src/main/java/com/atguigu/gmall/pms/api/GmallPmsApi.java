package com.atguigu.gmall.pms.api;

import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.CategoryVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


public interface GmallPmsApi {

    // 分页查询spu（createTIme)
    @GetMapping("pms/spuinfo/page")
    public Resp<List<SpuInfoEntity>> querySpusByPage(QueryCondition queryCondition);

    @GetMapping("pms/spuinfo/info/{id}")
    public Resp<SpuInfoEntity> querySpuById(@PathVariable("id") Long id);

    // 根据spuId查询spu下的sku
    @GetMapping("pms/skuinfo/{spuId}")
    public Resp<List<SkuInfoEntity>> querySkusBySpuId(@PathVariable("spuId")Long spuId);

    // 根据品牌id查询品牌
    @GetMapping("pms/brand/info/{brandId}")
    public Resp<BrandEntity> queryBrandsById(@PathVariable("brandId") Long brandId);

    // 根据分类id查询分类
    @GetMapping("pms/category/info/{catId}")
    public Resp<CategoryEntity> queryCategoryById(@PathVariable("catId") Long catId);

    // 根据spuid查询该商品对应的搜索属性及值
    @GetMapping("pms/productattrvalue/{spuId}")
    public Resp<List<ProductAttrValueEntity>> querySearchAttrValueBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/category")
    public Resp<List<CategoryEntity>> getCatagoriesByPidOrLevel(@RequestParam(value = "level", defaultValue = "0") Integer level,
                                                                @RequestParam(value = "parentCid", required = false) Long parentCid);

    @GetMapping("pms/category/{pid}")
    public Resp<List<CategoryVo>> querySubCategories(@PathVariable("pid") Long pid);

    @GetMapping("pms/skuinfo/info/{skuId}")
    public Resp<SkuInfoEntity> querySkuById(@PathVariable("skuId") Long skuId);

    @GetMapping("pms/skuimages/{skuId}")
    public Resp<List<SkuImagesEntity>> querySkuImagesBySkuId(@PathVariable("skuId") Long skuId);

    @GetMapping("pms/skusaleattrvalue/{spuId}")
    public Resp<List<SkuSaleAttrValueEntity>> querySkuSalesAttrValuesBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/spuinfodesc/info/{spuId}")
    public Resp<SpuInfoDescEntity> querySpuDescBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/attrgroup/item/group/{cid}/{spuId}")
    public Resp<List<ItemGroupVo>> queryItemGroupVoByCidAndSpuId(@PathVariable("cid") Long cid, @PathVariable("spuId") Long spuId);

    @GetMapping("pms/skusaleattrvalue/sku/{skuId}")
    public Resp<List<SkuSaleAttrValueEntity>> querySkuSalesAttrValuesBySkuId(@PathVariable("skuId") Long skuId);
}
