package com.atguigu.gmall.index.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "pms-service")
public interface GmallPmsClient extends GmallPmsApi {

}
