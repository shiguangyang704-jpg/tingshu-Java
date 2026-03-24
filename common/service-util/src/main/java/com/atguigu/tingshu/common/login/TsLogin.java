package com.atguigu.tingshu.common.login;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD}) // 这个注解在什么位置生效
@Retention(RetentionPolicy.RUNTIME) // 这个注解的生命周期
public @interface TsLogin {
    //  自定义一个属性：用这个属性来表示是否需要登录！ 默认登录：false:表示不需要登录
    boolean requireLogin() default true;
}
