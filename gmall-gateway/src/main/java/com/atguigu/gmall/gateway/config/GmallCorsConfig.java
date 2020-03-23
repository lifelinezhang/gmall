package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class GmallCorsConfig {

    // 配置可以进行跨域请求的路径以及跨域规则
    @Bean
    public CorsWebFilter corsWebFilter() {
        // cors跨域配置对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 配置允许跨域的域名
        corsConfiguration.addAllowedOrigin("http://localhost:1000");
        // 是否允许携带cookie
        corsConfiguration.setAllowCredentials(true);
        // 允许跨域请求的方法
        corsConfiguration.addAllowedMethod("*");
        // 允许跨域请求的头信息
        corsConfiguration.addAllowedHeader("*");
        // 配置源对象
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        // cors过滤器对象
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }


}
