package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.wms.api.GmallWmsAPi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsAPi {

}
