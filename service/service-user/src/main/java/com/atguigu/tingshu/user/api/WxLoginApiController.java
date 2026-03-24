package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 更新用户信息
     *
     * @param userInfoVo
     * @return
     */
    @TsLogin
    @Operation(summary = "更新用户信息")
    @PostMapping("/updateUser")
    public Result updateUser(@RequestBody UserInfoVo userInfoVo) {
        log.info("更新用户信息开始...");
        //  获取用户id
        Long userId = AuthContextHolder.getUserId();
        //  修改的思想：1. 修改的内容； 2. 修改的条件；
        //  更新用户信息
        userInfoService.updateUser(userInfoVo,userId);
        //  返回数据
        return Result.ok();
    }

    /**
     * 获取登录信息
     *
     * @return
     */
    @TsLogin
    @Operation(summary = "获取登录信息")
    @GetMapping("/getUserInfo")
    public Result getUserInfo() {
        //  登录的时候，会返回一个token 给前端放入header中.同时会将用户信息存储到缓存!
        log.info("获取登录信息开始...");
        //  根据本地线程的用户id，获取数据;
        Long userId = AuthContextHolder.getUserId();
        //  如果要查询缓存，需要获取token。如果根据用户Id需要查询数据库;
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        //  返回数据
        return Result.ok(userInfoVo);
    }

    /**
     * 微信登录
     *
     * @return
     */
    @Operation(summary = "微信登录")
    @GetMapping("/wxLogin/{code}")
    public Result wxLogin(@PathVariable String code) {
        log.info("微信登录开始...");

        //  调用微信登录方法
        Map<String, Object> map = userInfoService.wxLogin(code);
        //  返回数据
        return Result.ok(map);
    }


}
