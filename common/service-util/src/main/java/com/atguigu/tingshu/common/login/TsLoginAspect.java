package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author atguigu-mqx
 * @ClassName TsLoginAspect
 * @description: TODO
 * @date 2026年02月09日
 * @version: 1.0
 */
@Component
@Aspect
public class TsLoginAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * @param point
     * @param tsLogin
     * @return
     * @throws Throwable
     */
    @Around("@annotation(tsLogin)")
    public Object loginAspect(ProceedingJoinPoint point, TsLogin tsLogin) throws Throwable {
        // start stopwatch
        // Object retVal = point.proceed(); // 表示执行带有注解的方法体！
        // stop stopwatch
        // 如何用户登录了，1. 则会在请求头中有一个token！ 2. 登录成功之后，会将用户信息存储到缓存！
        //  从请求头中获取token！在切面类中如何获取请求头对象！
        //  String token = request.getHeader("token");
        //  RequestContextHolder 需要知道springmvc的请求流程！
        RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
        //  需要获取到请求，还可以获取响应;
        //  HttpServletRequest servletRequest = (HttpServletRequest) previousAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
        ServletRequestAttributes attributes = (ServletRequestAttributes) previousAttributes;
        //  请求对象;
        HttpServletRequest request = attributes.getRequest();
        //  响应：
        HttpServletResponse response = attributes.getResponse();
        String token = request.getHeader("token");
        //  还有一个判断条件：需要获取到当前注解的属性；
        if (tsLogin.requireLogin()) {
            //  判断token 是否存在！
            if (StringUtils.isEmpty(token)){
                //  不存在，则抛出异常，需要页面展示登录信息.
                throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
            }
            //  token 不为空！ 缓存中没有数据一样需要抛出异常；
            //  从缓存中获取数据; 考虑缓存的数据类型，以及缓存的key，value;
            //  key=userId;唯一，但是不合适！
            String userLoginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
            UserInfo userInfo = (UserInfo) this.redisTemplate.opsForValue().get(userLoginKey);
            //  判断
            if (null == userInfo){
                //  说明缓存中不存在！
                throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
            }
        }
        Object obj = null;

        try {
            //  判断token 不为空！
            if (!StringUtils.isEmpty(token)) {
                //  判断缓存中是否有数据！
                String userLoginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
                UserInfo userInfo = (UserInfo) this.redisTemplate.opsForValue().get(userLoginKey);
                //  判断
                if (null != userInfo) {
                    //  存储本地线程：
                    AuthContextHolder.setUserId(userInfo.getId());
                    AuthContextHolder.setUsername(userInfo.getNickname());
                }
            }
            //  执行带有注解的方法体！
            obj = point.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            //  移除本地线程数据
            AuthContextHolder.removeUserId();
            AuthContextHolder.removeUsername();
        }
        //  返回数据
        return obj;
    }

}
