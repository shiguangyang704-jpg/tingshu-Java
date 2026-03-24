package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author atguigu-mqx
 * @ClassName VipStrategy
 * @description: TODO
 * @date 2026年03月09日
 * @version: 1.0
 */
@Service("1001")
@Slf4j
public class AlbumStrategy implements ItemTypeStrategy {

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;
    @Override
    public Boolean savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        boolean isSuccess = false;
        //  购买专辑;
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>()
                .eq(UserPaidAlbum::getUserId, userPaidRecordVo.getUserId())
                .eq(UserPaidAlbum::getAlbumId, userPaidRecordVo.getItemIdList().get(0)));
        //  判断对象是否存在!
        if (null != userPaidAlbum) {
            throw new RuntimeException("用户已购买过专辑！");
        }
        //  保存购买记录;
        userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
        userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
        userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
        //  保存数据;
        try {
            isSuccess = userPaidAlbumMapper.insert(userPaidAlbum) > 0;
        } catch (Exception e) {
            log.error("保存用户购买记录失败！");
        }
        //  返回数据
        return isSuccess;
    }
}
