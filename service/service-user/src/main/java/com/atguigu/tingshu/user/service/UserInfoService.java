package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    /**
     * 微信登录
     * @param code
     * @return
     */
    Map<String, Object> wxLogin(String code);

    /**
     * 根据用户id查询用户信息
     * @param userId
     * @return
     */
    UserInfoVo getUserInfo(Long userId);

    /**
     * 修改用户信息
     * @param userInfoVo
     * @param userId
     */
    void updateUser(UserInfoVo userInfoVo,Long userId);

    /**
     * 查询用户是否付费
     * @param userId
     * @param albumId
     * @param trackIdList
     * @return
     */
    Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> trackIdList);

    /**
     * 查询用户付费的音频列表
     * @param albumId
     * @param userId
     * @return
     */
    List<Long> findUserPaidTrackList(Long albumId, Long userId);

    /**
     * 保存用户付费记录
     * @param userPaidRecordVo
     * @return
     */
    Boolean savePaidRecord(UserPaidRecordVo userPaidRecordVo);

}
