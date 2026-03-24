package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author atguigu-mqx
 * @ClassName VipStrategy
 * @description: TODO
 * @date 2026年03月09日
 * @version: 1.0
 */
@Service("1002")
@Slf4j
public class TrackStrategy implements ItemTypeStrategy {

    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;
    @Override
    public Boolean savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //  定义变量
        boolean isSuccess = false;

        //  购买声音;
        //  根据声音Id获取声音对象;
        try {
            Result<TrackInfo> trackInfoResult = trackInfoFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0));
            Assert.notNull(trackInfoResult, "查询声音信息失败！");
            TrackInfo trackInfo = trackInfoResult.getData();
            Assert.notNull(trackInfo, "查询声音信息失败！");
            Long albumId = trackInfo.getAlbumId();
            //  --- 多端能够同时登录： 则需要先查询列表；在循环对比; trackId 是否存在！ 如果存在！则continue；钱--退款！
            //  userPaidRecordVo.getItemIdList():记录的所有声音id集合;
            for (Long trackId : userPaidRecordVo.getItemIdList()) {
                //  创建对象
                UserPaidTrack userPaidTrack = new UserPaidTrack();
                //  赋值：
                userPaidTrack.setUserId(userPaidRecordVo.getUserId());
                //  获取专辑Id;
                userPaidTrack.setAlbumId(albumId);
                userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
                userPaidTrack.setTrackId(trackId);
                userPaidTrackMapper.insert(userPaidTrack);
            }
            //  成功！
            isSuccess = true;
        } catch (Exception e) {
            log.error("保存用户购买记录失败！");
        }
        return isSuccess;
    }
}
