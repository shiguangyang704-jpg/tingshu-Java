package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    /**
     * 获取用户某首歌曲的播放进度
     *
     * @param trackId 歌曲id
     * @param userId  用户id
     * @return
     */
    BigDecimal getTrackBreakSecond(Long trackId, Long userId);

    /**
     * 更新用户播放进度
     *
     * @param userListenProcessVo
     * @param userId
     */
    void updateListenProcess(UserListenProcessVo userListenProcessVo, Long userId);

    /**
     * 获取用户最近播放歌曲
     *
     * @param userId
     * @return
     */
    Map<String, Object> getLatelyTrack(Long userId);

}
