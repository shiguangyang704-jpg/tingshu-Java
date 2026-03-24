package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user/userInfo")
@SuppressWarnings({"all"})
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 处理用户购买记录
     *
     * @param userPaidRecordVo
     * @return
     */
    @Operation(summary = "处理用户购买记录")
    @PostMapping("/savePaidRecord")
    Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo) {
        //	调用服务层方法;
        Boolean result = userInfoService.savePaidRecord(userPaidRecordVo);
        //	返回数据：
        return result ? Result.ok() : Result.fail();
    }


    /**
     * 获取专辑已支付的声音Id集合列表
     *
     * @param albumId
     * @return
     */
    @TsLogin
    @Operation(summary = "获取专辑已支付的声音Id集合列表")
    @GetMapping("/findUserPaidTrackList/{albumId}")
    public Result<List<Long>> findUserPaidTrackList(@PathVariable Long albumId) {
        //	获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //	调用服务层方法;
        List<Long> trackIdList = userInfoService.findUserPaidTrackList(albumId, userId);
        //	返回数据
        return Result.ok(trackIdList);
    }


    /**
     * 判断用户是否购买过专辑或声音
     *
     * @param albumId
     * @param trackIdList
     * @return
     */
    @TsLogin
    @Operation(summary = "判断用户是否购买过专辑或声音")
    @PostMapping("/userIsPaidTrack/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable("albumId") Long albumId, @RequestBody List<Long> trackIdList) {
        //	获取到用户Id
        Long userId = AuthContextHolder.getUserId();
        //	调用服务层方法;
        Map<Long, Integer> map = userInfoService.userIsPaidTrack(userId, albumId, trackIdList);
        //	返回数据
        return Result.ok(map);
    }

    /**
     * 根据userId 获取到用户信息
     *
     * @param userId
     * @return
     */
    @Operation(summary = "根据userId 获取到用户信息")
    @GetMapping("/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId) {
        //	调用服务层方法;
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        //	返回数据
        return Result.ok(userInfoVo);
    }

}

