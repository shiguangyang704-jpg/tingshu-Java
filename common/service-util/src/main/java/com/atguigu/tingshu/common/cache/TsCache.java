package com.atguigu.tingshu.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author atguigu-mqx
 * @ClassName TsCache
 * @description: TODO
 * @date 2026年03月03日
 * @version: 1.0
 */
@Target({ElementType.METHOD}) // 这个注解在什么位置生效
@Retention(RetentionPolicy.RUNTIME) // 这个注解的生命周期
public @interface TsCache {

    //  核心点：分布式锁的业务逻辑;
    //  只要注解的前缀不一样，即使方法的参数意义，也能保证key不重复！ALBUM_INFO_PREFIX
    String prefix() default "cache:";

}
