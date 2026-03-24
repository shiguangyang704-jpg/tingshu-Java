package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.*;
import com.atguigu.tingshu.user.factory.StrategyFactory;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    //	注入一个对象到spring 容器中.
    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;
    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;

    @Autowired
    private StrategyFactory strategyFactory;

    @Override
    public List<Long> findUserPaidTrackList(Long albumId, Long userId) {
        //  user_paid_track
        List<Long> trackIdList = userPaidTrackMapper.selectList(new LambdaQueryWrapper<UserPaidTrack>().eq(UserPaidTrack::getUserId, userId)
                        .eq(UserPaidTrack::getAlbumId, albumId))
                .stream()
                .map(UserPaidTrack::getTrackId)
                .collect(Collectors.toList());
        //  返回数据
        return trackIdList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //  通过工厂模式获取到策略对象
        ItemTypeStrategy typeStrategy = strategyFactory.getTypeStrategy(userPaidRecordVo.getItemType());
        //  通过策略对象调用方法;
        return typeStrategy.savePaidRecord(userPaidRecordVo);
        //        //  声明变量
        //        boolean isSuccess = false;
        //        //  判断用户购买类型：
        //        if (userPaidRecordVo.getItemType().equals(SystemConstant.ORDER_ITEM_TYPE_ALBUM)) {
        //            //  购买专辑;
        //            UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>()
        //                    .eq(UserPaidAlbum::getUserId, userPaidRecordVo.getUserId())
        //                    .eq(UserPaidAlbum::getAlbumId, userPaidRecordVo.getItemIdList().get(0)));
        //            //  判断对象是否存在!
        //            if (null != userPaidAlbum) {
        //                throw new RuntimeException("用户已购买过专辑！");
        //            }
        //            //  保存购买记录;
        //            userPaidAlbum = new UserPaidAlbum();
        //            userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
        //            userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
        //            userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
        //            //  保存数据;
        //            try {
        //                isSuccess = userPaidAlbumMapper.insert(userPaidAlbum) > 0;
        //            } catch (Exception e) {
        //                log.error("保存用户购买记录失败！");
        //            }
        //
        //        } else if (userPaidRecordVo.getItemType().equals(SystemConstant.ORDER_ITEM_TYPE_TRACK)) {
        //            //  购买声音;
        //            //  根据声音Id获取声音对象;
        //            try {
        //                Result<TrackInfo> trackInfoResult = trackInfoFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0));
        //                Assert.notNull(trackInfoResult, "查询声音信息失败！");
        //                TrackInfo trackInfo = trackInfoResult.getData();
        //                Assert.notNull(trackInfo, "查询声音信息失败！");
        //                Long albumId = trackInfo.getAlbumId();
        //                //  --- 多端能够同时登录： 则需要先查询列表；在循环对比; trackId 是否存在！ 如果存在！则continue；钱--退款！
        //                //  userPaidRecordVo.getItemIdList():记录的所有声音id集合;
        //                for (Long trackId : userPaidRecordVo.getItemIdList()) {
        //                    //  创建对象
        //                    UserPaidTrack userPaidTrack = new UserPaidTrack();
        //                    //  赋值：
        //                    userPaidTrack.setUserId(userPaidRecordVo.getUserId());
        //                    //  获取专辑Id;
        //                    userPaidTrack.setAlbumId(albumId);
        //                    userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
        //                    userPaidTrack.setTrackId(trackId);
        //                    userPaidTrackMapper.insert(userPaidTrack);
        //                }
        //                //  成功！
        //                isSuccess = true;
        //            } catch (Exception e) {
        //                log.error("保存用户购买记录失败！");
        //            }
        //        } else {
        //            //  第一件事：记录当前用户购买信息; user_vip_service 第二件事；user_info 更新用户信息;
        //            //  购买vip; 需要做要给续期！
        //            //  先获取购买vip 服务的配置Id
        //            try {
        //                Long vipServiceConfigId = userPaidRecordVo.getItemIdList().get(0);
        //                //  获取数据对象
        //                VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(vipServiceConfigId);
        //                //  获取用户购买的月份数;
        //                Integer serviceMonth = vipServiceConfig.getServiceMonth();
        //                //  考虑这次购买是否属于续期！
        //                //  查看现在的用户身份，是否属于过期!
        //                UserInfo userInfo = this.userInfoMapper.selectById(userPaidRecordVo.getUserId());
        //                //  创建对象
        //                UserVipService userVipservice = new UserVipService();
        //                userVipservice.setOrderNo(userPaidRecordVo.getOrderNo());
        //                userVipservice.setUserId(userPaidRecordVo.getUserId());
        //                userVipservice.setStartTime(new Date());
        //                //  创建时间对象；
        //                LocalDateTime localDateTime = new LocalDateTime(new Date());
        //                //  判断当前用户的身份.
        //                if (userInfo.getIsVip() == 1 && userInfo.getVipExpireTime().after(new Date())) {
        //                    //  属于续期 过期时间基础上+购买的时间;
        //                    localDateTime = new LocalDateTime(userInfo.getVipExpireTime());
        //                }
        //                //  添加购买时间;
        //                LocalDateTime expireTime = localDateTime.plusMonths(serviceMonth);
        //                userVipservice.setExpireTime(expireTime.toDate());
        //                //  记录用户购买vip;
        //                userVipServiceMapper.insert(userVipservice);
        //                //  user_info
        //                //  不管你是续期，还是头一次统一更新一次;
        //                userInfo.setIsVip(1);
        //                userInfo.setVipExpireTime(userVipservice.getExpireTime());
        //                this.updateById(userInfo);
        //                //  成功
        //                isSuccess = true;
        //            } catch (Exception e) {
        //                log.error("保存用户购买记录失败！");
        //            }
        //        }
        //        //  返回数据；
        //        return isSuccess;
    }

    @Override
    public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> trackIdList) {
        //  user_paid_album 判断当前这个用户是否购买了专辑;
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getUserId, userId)
                .eq(UserPaidAlbum::getAlbumId, albumId));
        //  判断对象是否为空;
        if (null != userPaidAlbum) {
            //  说明这个用户购买了专辑；当前专辑下对应的所有声音都需要免费;
            //  key=trackId: value=0/1 0：未购买过 1:用户已购买过
            Map<Long, Integer> map = trackIdList.stream().collect(Collectors.toMap(trackId -> trackId
                    , trackId -> 1));
            //  返回数据
            return map;
        }
        //  判断当前用户是否购买了付费的声音; user_paid_track
        //  select * from user_paid_track where user_id = ? and album_id = ? and track_id in (?,?,?)
        //  构建查询条件：
        LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidTrack::getUserId, userId)
                .eq(UserPaidTrack::getAlbumId, albumId)
                .in(UserPaidTrack::getTrackId, trackIdList);
        //  查看用户已购买的声音Id集合：
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(wrapper);
        //  默认值都是0表示未购买过！
        Map<Long, Integer> map = trackIdList.stream().collect(Collectors.toMap(trackId -> trackId, trackId -> 0));
        //  判断userPaidTrackList
        if (!CollectionUtils.isEmpty(userPaidTrackList)) {
            List<Long> userpaidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
            // 给购买过的声音Id 设置为1
            for (Long trackId : trackIdList) {
                if (userpaidTrackIdList.contains(trackId)) {
                    map.put(trackId, 1);
                }
            }
        }
        return map;
        //  判断userPaidTrackList这个集合 为空，则说明没有购买过任何声音!
        //        if (CollectionUtils.isEmpty(userPaidTrackList)) {
        //            //  判断集合是否为空;
        //            Map<Long, Integer> map = trackIdList.stream().collect(Collectors.toMap(trackId -> trackId
        //                    , trackId -> 0));
        //            //  返回数据;
        //            return map;
        //        } else {
        //            //  创建一个map 集合：
        //            HashMap<Long, Integer> map = new HashMap<>();
        //            List<Long> userpaidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
        //            // 给购买过的声音Id 设置为1
        //            for (Long trackId : trackIdList) {
        //                if (userpaidTrackIdList.contains(trackId)) {
        //                    map.put(trackId, 1);
        //                } else {
        //                    map.put(trackId, 0);
        //                }
        //            }
        //            //  返回数据；
        //            return map;
        //        }

    }

    @Override
    public void updateUser(UserInfoVo userInfoVo, Long userId) {
        //  创建用户对象;
        UserInfo userInfo = new UserInfo();
        //  赋值
        userInfo.setId(userId);
        userInfo.setNickname(userInfoVo.getNickname());
        userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
        //  更新数据
        userInfoMapper.updateById(userInfo);
    }

    @Override
    public UserInfoVo getUserInfo(Long userId) {
        //  获取数据
        UserInfo userInfo = userInfoMapper.selectById(userId);
        //  属性拷贝：
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo, userInfoVo);
        //  返回数据
        return userInfoVo;
    }

    @Override
    public Map<String, Object> wxLogin(String code) {
        //	调用服务器接口：
        WxMaJscode2SessionResult wxMaJscode2SessionResult = null;
        try {
            wxMaJscode2SessionResult = wxMaService.jsCode2SessionInfo(code);
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
        //	微信openid 是唯一值;
        String openid = wxMaJscode2SessionResult.getOpenid();
        //	利用这个唯一值与数据库表user_info 进行比较;
        UserInfo userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getWxOpenId, openid));
        //	判断用户对象;
        if (null == userInfo) {
            //	未注册过，需要给你注册! 保存数据到user_info;
            userInfo = new UserInfo();
            userInfo.setWxOpenId(openid);
            userInfo.setNickname("听友：" + System.currentTimeMillis());
            userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            //	保存
            userInfoMapper.insert(userInfo);
            //	充值;100;service-account; mq;更好！
            rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_USER_REGISTER, userInfo.getId());
        }
        //	不为空; ---存储到缓存！
        //	存储数据
        String token = UUID.randomUUID().toString();
        //  定义缓存的key;
        String userLoginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
        //  存储用户信息数据;
        this.redisTemplate.opsForValue().set(userLoginKey, userInfo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
        //	声明给map 集合
        Map<String, Object> map = new HashMap<>();
        //  存储数据;
        map.put("token", token);
        //	返回数据
        return map;
    }
}
