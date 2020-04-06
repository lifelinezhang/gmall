package com.atguigu.gmall.index.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    @AliasFor("prefix")
    String value() default "";

    /**
     * 缓存的前缀
     *
     * @return
     */
    @AliasFor("value")
    String prefix() default "";

    /**
     * 缓存过期时间： 以分为单位
     *
     * @return
     */
    int timeout() default 5;

    /**
     * 为了防止缓存雪崩指定的随机值范围
     *
     * @return
     */
    int random() default 5;
}
