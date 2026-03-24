package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserVipService;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserVipServiceMapper;
import com.atguigu.tingshu.user.mapper.VipServiceConfigMapper;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author atguigu-mqx
 * @ClassName VipStrategy
 * @description: TODO
 * @date 2026年03月09日
 * @version: 1.0
 */
@Service("1003")
@Slf4j
public class VipStrategy implements ItemTypeStrategy {

    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserVipServiceMapper userVipServiceMapper;
    @Override
    public Boolean savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //  定义变量;
        boolean isSuccess = false;
        try {
            Long vipServiceConfigId = userPaidRecordVo.getItemIdList().get(0);
            //  获取数据对象
            VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(vipServiceConfigId);
            //  获取用户购买的月份数;
            Integer serviceMonth = vipServiceConfig.getServiceMonth();
            //  考虑这次购买是否属于续期！
            //  查看现在的用户身份，是否属于过期!
            UserInfo userInfo = this.userInfoMapper.selectById(userPaidRecordVo.getUserId());
            //  创建对象
            UserVipService userVipservice = new UserVipService();
            userVipservice.setOrderNo(userPaidRecordVo.getOrderNo());
            userVipservice.setUserId(userPaidRecordVo.getUserId());
            userVipservice.setStartTime(new Date());
            //  创建时间对象；
            LocalDateTime localDateTime = new LocalDateTime(new Date());
            //  判断当前用户的身份.
            if (userInfo.getIsVip() == 1 && userInfo.getVipExpireTime().after(new Date())) {
                //  属于续期 过期时间基础上+购买的时间;
                localDateTime = new LocalDateTime(userInfo.getVipExpireTime());
            }
            //  添加购买时间;
            LocalDateTime expireTime = localDateTime.plusMonths(serviceMonth);
            userVipservice.setExpireTime(expireTime.toDate());
            //  记录用户购买vip;
            userVipServiceMapper.insert(userVipservice);
            //  user_info
            //  不管你是续期，还是头一次统一更新一次;
            userInfo.setIsVip(1);
            userInfo.setVipExpireTime(userVipservice.getExpireTime());
            //  this.updateById(userInfo);
            userInfoMapper.updateById(userInfo);
            //  显示写一行错误代码！
//            int i = 1/0;
            //  成功
            isSuccess = true;
        } catch (Exception e) {
            log.error("保存用户购买记录失败！");
        }
        return isSuccess;
    }
}
