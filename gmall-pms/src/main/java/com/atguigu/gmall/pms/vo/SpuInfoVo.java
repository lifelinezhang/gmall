package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import lombok.Data;

import java.util.List;

@Data
public class SpuInfoVo extends SpuInfoEntity {

    private List<String> spuImages;
    private List<BaseAttrVo> baseAttrs;
    private List<SkuInfoVo> skus;
}


/***
 这个请求是一个跨微服务的请求，且看他如何处理
 {
 "spuName": "锤子S100",
 "brandId": 4,
 "catalogId": 225,
 "publishStatus": 1,
 "spuDescription": "阿斯蒂芬",
 "spuImages": ["https://ggmall.oss-cn-shanghai.aliyuncs.com/2020-03-18/129f18cb-75b6-441f-8edf-c2d9276f60c0_download.jpg", "https://ggmall.oss-cn-shanghai.aliyuncs.com/2020-03-18/2f7832b9-17c3-41ac-830e-b0b2cbb68435_d49eb0f94e78a6f4069ff60d29373fc.jpg"],
 "baseAttrs": [{
 "attrId": 25,
 "attrName": "大萨达222",
 "valueSelected": ["13213"]
 }, {
 "attrId": 41,
 "attrName": "摄像头数量",
 "valueSelected": ["3"]
 }, {
 "attrId": 33,
 "attrName": "电池",
 "valueSelected": ["4000"]
 }, {
 "attrId": 34,
 "attrName": "屏幕",
 "valueSelected": ["5"]
 }],
 "skus": [{
 "attr_24": "8g",
 "price": "100",
 "stock": 0,
 "growBounds": "100",
 "buyBounds": "2000",
 "work": [1, 0, 0, 0],
 "fullCount": "3",
 "discount": "5",
 "fullPrice": "200",
 "reducePrice": "100",
 "fullAddOther": 1,
 "images": ["https://ggmall.oss-cn-shanghai.aliyuncs.com/2020-03-18/5350f22d-7346-471d-85d2-cb1bc42490e9_download.jpg"],
 "skuName": "锤子S100 8g,粉色,128g",
 "skuDesc": "傻逼才买",
 "skuTitle": "锤子S100 8g,粉色,128g",
 "skuSubtitle": "锤子S100 8g,粉色,128g",
 "weight": "10",
 "attr_30": "粉色",
 "attr_35": "128g",
 "saleAttrs": [{
 "attrId": "24",
 "attrValue": "8g"
 }, {
 "attrId": "30",
 "attrValue": "粉色"
 }, {
 "attrId": "35",
 "attrValue": "128g"
 }],
 "ladderAddOther": 1
 }, {
 "attr_24": "8g",
 "price": "200",
 "stock": 0,
 "growBounds": "20",
 "buyBounds": "3000",
 "work": [0, 1, 0, 0],
 "fullCount": "2",
 "discount": "6",
 "fullPrice": "300",
 "reducePrice": "200",
 "fullAddOther": 1,
 "images": ["https://ggmall.oss-cn-shanghai.aliyuncs.com/2020-03-18/1578e839-d5dc-4170-8aed-7924b6c22bcc_download.jpg"],
 "skuName": "锤子S100 8g,粉色,256",
 "skuDesc": "锤子S100 8g,粉色,256",
 "skuTitle": "锤子S100 8g,粉色,256",
 "skuSubtitle": "锤子S100 8g,粉色,256",
 "weight": "200",
 "attr_30": "粉色",
 "attr_35": "256",
 "saleAttrs": [{
 "attrId": "24",
 "attrValue": "8g"
 }, {
 "attrId": "30",
 "attrValue": "粉色"
 }, {
 "attrId": "35",
 "attrValue": "256"
 }],
 "ladderAddOther": 1
 }]
 }

 */