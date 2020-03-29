package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.pojo.SearchParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public void search(SearchParam searchParam) throws IOException {
        // 构建DSL语句
        SearchRequest searchRequest = this.buildQueryDsl(searchParam);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(response);
    }

    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        // 获取关键字
        String keyword = searchParam.getKeyword();
        if(StringUtils.isEmpty(keyword)) {
            return null;
        }
        // 查询条件构建器
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 1、构建查询条件和过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 1.1、构建查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
        // 1.2、构建过滤条件
        // 1.2.1、构建品牌过滤
        String[] brand = searchParam.getBrand();
        if(brand != null && brand.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brand));
        }
        // 1.2.2、 构建分类过滤
        String[] catelog3 = searchParam.getCatelog3();
        if(catelog3 != null && catelog3.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", catelog3));
        }
        // 1.2.3、 构建规格属性嵌套过滤
        String[] props = searchParam.getProps();
        if(props != null && props.length != 0) {
            for (String prop : props) {
                // 以：进行分割，分割后应该是两个元素，1-attrId， 2-attrValue（以 - 分割的字符串）
                String[] split = StringUtils.split(prop, ":");
                // 判断切分之后的字符串是否合法
                if(split == null || split.length != 2) {
                    continue;
                }
                // 以 - 分割处理attrValue
                String[] attrValues = StringUtils.split(split[1], "-");
                // 构建嵌套查询
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                // 构建嵌套查询中的子查询
                BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                // 构建子查询中的过滤条件
                subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                subBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                // 把嵌套查询放入过滤器中
                boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));
                boolQueryBuilder.filter(boolQuery);
            }
        }
        // 1.2.4、 价格区间过滤
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
        Integer priceFrom = searchParam.getPriceFrom();
        Integer priceTo = searchParam.getPriceTo();
        if(priceFrom != null) {
            rangeQueryBuilder.gte(priceFrom);
        }
        if(priceTo != null) {
            rangeQueryBuilder.lte(priceTo);
        }
        boolQueryBuilder.filter(rangeQueryBuilder);
        // 查询条件构建器最后一步
        sourceBuilder.query(boolQueryBuilder);
        // 2、构建分页
        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);
        // 3、构建排序
        String order = searchParam.getOrder();
        if(!StringUtils.isEmpty(order)) {
            String[] split = StringUtils.split(order, ":");
            if(split != null && split.length == 2) {
                String field = null;
                switch (split[0]) {
                    case "1" : field = "sale"; break;
                    case "2" : field = "price"; break;
                }
                sourceBuilder.sort(field, StringUtils.equals("asc", split[1]) ? SortOrder.ASC : SortOrder.DESC);
            }
        }
        // 4、构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<em>").postTags("</em>"));
        // 5、构建聚合
        // 5.1、 品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName")));
        // 5.2、 分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        // 5.3、 搜索规格属性聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "attrs")
            .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
            .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
            .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));
        System.out.println(sourceBuilder.toString());
        // 查询参数
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }
    /**
     * GET /goods/_search
     * {
     *   "query": {
     *     "bool": {
     *       "must": [
     *         {
     *           "match": {
     *             "title": {
     *               "query": "手机",
     *               "operator": "and"
     *             }
     *           }
     *         }
     *       ],
     *       "filter": [
     *         {
     *           "terms": {
     *             "brandId": [
     *               "6"
     *             ]
     *           }
     *         },
     *         {
     *           "range": {
     *             "price": {
     *               "gte": 2000,
     *               "lte": 5000
     *             }
     *           }
     *         },
     *         {
     *           "terms": {
     *             "categoryId": [
     *               "225"
     *             ]
     *           }
     *         },
     *         {
     *           "bool": {
     *             "must": [
     *               {
     *                 "nested": {
     *                   "path": "attrs",
     *                   "query": {
     *                     "bool": {
     *                       "must": [
     *                         {
     *                           "term": {
     *                             "attrs.attrName": "电池"
     *                           }
     *                         },
     *                         {
     *                           "terms": {
     *                             "attrs.attrValue": ["4000"]
     *                           }
     *                         }
     *                       ]
     *                     }
     *                   }
     *                 }
     *               }
     *             ]
     *           }
     *         }
     *       ]
     *     }
     *   },
     *   "from": 1,
     *   "size": 1,
     *   "sort": [
     *     {
     *       "price": {
     *         "order": "asc"
     *       }
     *     }
     *   ],
     *   "highlight": {
     *     "fields": {"title": {}},
     *     "pre_tags": "<em>",
     *     "post_tags": "</em>"
     *   },
     *   "aggs": {
     *     "brandIdAgg": {
     *       "terms": {
     *         "field": "brandId"
     *       },
     *       "aggs": {
     *         "brandNameAgg": {
     *           "terms": {
     *             "field": "brandName"
     *           }
     *         }
     *       }
     *     },
     *     "categoryIdAgg": {
     *       "terms": {
     *         "field": "categoryId"
     *       },
     *       "aggs": {
     *         "categoryNameAgg": {
     *           "terms": {
     *             "field": "categoryName"
     *           }
     *         }
     *       }
     *     },
     *     "attrAgg": {
     *       "nested": {
     *         "path": "attrs"
     *       },
     *       "aggs": {
     *         "attrIdAgg": {
     *           "terms": {
     *             "field": "attrs.attrId"
     *           },
     *           "aggs": {
     *             "attrNameAgg": {
     *               "terms": {
     *                 "field": "attrs.attrName"
     *               }
     *             },
     *             "attrValueAgg": {
     *               "terms": {
     *                 "field": "attrs.attrValue"
     *               }
     *             }
     *           }
     *         }
     *       }
     *     }
     *   }
     * }
     */


}
