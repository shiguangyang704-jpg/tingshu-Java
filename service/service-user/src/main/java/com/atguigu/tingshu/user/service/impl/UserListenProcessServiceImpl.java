package com.atguigu.tingshu.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public Map<String, Object> getLatelyTrack(Long userId) {
        //  创建7一个map集合：
        Map<String, Object> map = new HashMap<>();
        //  获取最近播放的声音;
        //  获取到集合名称;
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        //  查找这个用户播放过的所有声音列表;按照更新时间进行排序；降序; selectOne()
        //  构建查询条件;
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        //  判断
        if (null != userListenProcess) {
            map.put("albumId", userListenProcess.getAlbumId());
            map.put("trackId", userListenProcess.getTrackId());
        }
        //  返回数据;
        return map;
    }

    @Override
    public void updateListenProcess(UserListenProcessVo userListenProcessVo, Long userId) {
        //  先定位到将数据保存到哪个家集合?
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        //  UserListenProcess 修改记录：
        //  创建UserListenProcess 这对象;
        Query query = Query.query(Criteria.where("trackId").is(userListenProcessVo.getTrackId()));
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        //  判断
        if (null != userListenProcess) {
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            userListenProcess.setUpdateTime(new Date());
        } else {
            //  创建对象
            userListenProcess = new UserListenProcess();
            //  赋值：
            userListenProcess.setUserId(userId);
            userListenProcess.setTrackId(userListenProcessVo.getTrackId());
            userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setUpdateTime(new Date());
        }
        //  实现修改功能;
        mongoTemplate.save(userListenProcess, collectionName);
        //  增加播放量的时候；肯定是在第一次！ setnx:key不存在的时候生效;
        //  同一个人在规定的时间内，播放同一个声音的时候：第一次播放数量+1，否则不加;
        String key = userId + ":" + userListenProcessVo.getTrackId();
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", 1, TimeUnit.DAYS);
        //  判断这个结果
        if (result) {
            //  说明key不存在，可以增加播放量; 指定的专辑对应的声音+1 与 专辑+1;
            //  创建一个对象来处理播放量的问题：
            TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
            //  赋值;
            trackStatMqVo.setCount(1);
            trackStatMqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
            trackStatMqVo.setTrackId(userListenProcessVo.getTrackId());
            trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
            trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-", ""));
            //  使用异步实现更新播放量;
            rabbitService.sendMessage(MqConst.EXCHANGE_TRACK, MqConst.ROUTING_TRACK_STAT_UPDATE, JSON.toJSONString(trackStatMqVo));
        }

    }

    @Override
    public BigDecimal getTrackBreakSecond(Long trackId, Long userId) {
        //	从哪里获取数据? mongodb; userId-集合; data：class UserListenProcess{}
        Query query = Query.query(Criteria.where("trackId").is(trackId));
        //	获取集合的名称;
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        //	返回数据;
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        //	判断
        if (null != userListenProcess) {
            return userListenProcess.getBreakSecond();
        }
        return new BigDecimal("0");
    }
}
