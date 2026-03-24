package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.cache.TsCache;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    @TsCache(prefix = "album:stat:")
    public AlbumStatVo getAlbumStat(Long albumId) {
        //  调用mapper 层方法;
        return albumStatMapper.selectAlbumStat(albumId);
    }

    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId) {
        //	查询数据 select * from album_info where user_id = 1;
        //	构建分页查询：
        Page<AlbumInfo> albumInfoPage = new Page<>(1, 100);
        //	设置查询条件：
        LambdaQueryWrapper<AlbumInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumInfo::getUserId, userId)
                .select(AlbumInfo::getId, AlbumInfo::getAlbumTitle)
                .orderByDesc(AlbumInfo::getId);
        //	调用分页查询方法
        return albumInfoMapper.selectPage(albumInfoPage, wrapper).getRecords();
//		return albumInfoMapper.selectList(new LambdaQueryWrapper<AlbumInfo>().eq(AlbumInfo::getUserId, userId));
    }

    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo) {
        //	album_info album_attribute_value album_stat
        //	调用mapper 层的修改方法.
        //	第一个参数表示什么? albumInfo:承载修改的数据; 第二个参数：修改条件
        //	AlbumInfo albumInfo = new AlbumInfo();
        //	albumInfoMapper.update(albumInfo, new LambdaQueryWrapper<AlbumInfo>().eq(AlbumInfo::getId, albumId))
        //  albumInfo: 承载的修改数据+Id必须主键！
        //	构建AlbumInfo对象;
        AlbumInfo albumInfo = new AlbumInfo();
        //	赋值主键Id
        albumInfo.setId(albumId);
        //	赋值修改的数据;
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        albumInfoMapper.updateById(albumInfo);
        //	album_attribute_value 用户点击修改内容的时候，没有发送链接请求：只能获取到最新数据;
        //	将原有数据删除，再做保存！
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        albumAttributeValueMapper.delete(wrapper);
        //	新增：
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if (!CollectionUtils.isEmpty(albumAttributeValueVoList)) {
            List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList.stream().map(albumAttributeValueVo -> {
                AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                albumAttributeValue.setAlbumId(albumInfo.getId());
                albumAttributeValue.setAttributeId(albumAttributeValueVo.getAttributeId());
                albumAttributeValue.setValueId(albumAttributeValueVo.getValueId());
                return albumAttributeValue;
            }).collect(Collectors.toList());
            //	批量添加
            albumAttributeValueService.saveBatch(albumAttributeValueList);
        }
        //	album_stat -- 页面没有传递暂时不需要修改！
        //  判断当前是否需要上架：
        //        if("1".equals(albumInfoVo.getIsOpen())){
        //            //  此时需要上架;
        //            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, albumInfo.getId());
        //        } else {
        //            //  此时需要下架;
        //            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_LOWER, albumInfo.getId());
        //        }
        //  判断获取修改的数据规则;
        String routingKey = "1".equals(albumInfoVo.getIsOpen()) ? MqConst.ROUTING_ALBUM_UPPER : MqConst.ROUTING_ALBUM_LOWER;
        //  发送消息实现，下架
        rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, routingKey, albumInfo.getId());
        if ("1".equals(albumInfoVo.getIsOpen())) {
            //  获取布隆过滤器
            RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
            //  添加数据
            bloomFilter.add(albumId);
        }
    }

    @Override
    @TsCache(prefix = RedisConstant.ALBUM_INFO_PREFIX)
    public AlbumInfo getAlbumInfo(Long albumId) {
        //  查询数据库：
        return getAlbumInfoDB(albumId);

        //	查询数据 albumInfoMapper.selectById(albumId);
        //  实现查询时，先查询缓存，再查询数据库的业务逻辑。同时注意缓存击穿的情况-Redisson分布式锁！
        //  考虑缓存以什么数据类型来存储；key是什么！
        //        String albumInfoKey = RedisConstant.ALBUM_INFO_PREFIX + albumId;
        //        //  根据key 查询缓存
        //        AlbumInfo albumInfo = null;
        //        try {
        //            albumInfo = (AlbumInfo) this.redisTemplate.opsForValue().get(albumInfoKey);
        //            //  判断缓存中是否有数据;
        //            if (null == albumInfo) {
        //                log.info("缓存中没有数据！查询数据库并放入缓存");
        //                //  准备定义锁的key;
        //                String locKey = RedisConstant.ALBUM_LOCK_SUFFIX + albumId;
        //                //  调用RedissonClient方法.
        //                RLock lock = redissonClient.getLock(locKey);
        //                //  调用lock()方法
        //                lock.lock();
        ////                lock.lock(10,TimeUnit.MINUTES);
        //                //  业务逻辑;
        //                try {
        //                    albumInfo = (AlbumInfo) this.redisTemplate.opsForValue().get(albumInfoKey);
        //                    if (null != albumInfo){
        //                        log.info("缓存中有数据！直接返回数据");
        //                        return albumInfo;
        //                    }
        //                    //  从数据库中获取数据
        //                    albumInfo = getAlbumInfoDB(albumId);
        //                    ///  判断
        //                    if (null == albumInfo){
        //                        //  数据库中没有这个专辑
        //                        this.redisTemplate.opsForValue().set(albumInfoKey, new AlbumInfo(), RedisConstant.ALBUM_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
        //                        return new AlbumInfo();
        //                    }
        //                    //  放入缓存！
        //                    this.redisTemplate.opsForValue().set(albumInfoKey, albumInfo, RedisConstant.ALBUM_TIMEOUT, TimeUnit.SECONDS);
        //                    return albumInfo;
        //                } finally {
        //                    //  解锁：
        //                    lock.unlock();
        //                }
        //            } else {
        //                log.info("缓存中有数据！直接返回数据");
        //                return albumInfo;
        //            }
        //        } catch (Exception e) {
        //            log.error("查询缓存异常！", e);
        //        }
        //        //	返回数据
        //        return getAlbumInfoDB(albumId);
    }

    @Nullable
    private AlbumInfo getAlbumInfoDB(Long albumId) {
        AlbumInfo albumInfo = this.getById(albumId);
        //	判断
        if (null != albumInfo) {
            //	获取到属性与属性值数据：
            List<AlbumAttributeValue> albumAttributeValueList = this.albumAttributeValueMapper.selectList(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
            //	赋值：
            albumInfo.setAlbumAttributeValueVoList(albumAttributeValueList);
        }
        return albumInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAlbumInfo(Long albumId) {
        //	删除;album_info
        albumInfoMapper.deleteById(albumId);
        //	album_attribute_value
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        albumAttributeValueMapper.delete(wrapper);
        //	album_stat
        albumStatMapper.delete(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, albumId));
    }

    @Override
    public IPage<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> albumListVoPage, AlbumInfoQuery albumInfoQuery) {
        //	查询mapper数据
        return albumInfoMapper.selectUserAlbumPage(albumListVoPage, albumInfoQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {
        //	保存数据到数据库表：
		/*
		album_attribute_value
		album_info
		album_stat
		 */
        //	创建一个对象;
        AlbumInfo albumInfo = new AlbumInfo();
        //	属性拷贝：将相同的属性赋值;
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        //	设置用户Id;
        albumInfo.setUserId(userId);
        //	免费试听集数; 根据专辑的类型: '付费类型: 0101-免费、0102-vip免费、0103-付费
        if (!albumInfoVo.getPayType().equals(SystemConstant.ALBUM_PAY_TYPE_FREE)) {
            //	设置集数;
            albumInfo.setTracksForFree(5);
        }
        //	状态：正常会有一个审核流程：--> mq --> 管理员--> 审核 --> mq: 更新状态;
        albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
        //	保存数据;
        albumInfoMapper.insert(albumInfo);
        //	album_attribute_value
        //	先获取到前端传递的数据：
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if (!CollectionUtils.isEmpty(albumAttributeValueVoList)) {
            //			for (AlbumAttributeValueVo albumAttributeValueVo : albumAttributeValueVoList) {
            //				//	创建一个对象
            //				AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
            //				//	赋值：
            //				albumAttributeValue.setAlbumId(albumInfo.getId());
            //				albumAttributeValue.setAttributeId(albumAttributeValueVo.getAttributeId());
            //				albumAttributeValue.setValueId(albumAttributeValueVo.getValueId());
            //				//	保存; 循环遍历执行 n 次insert 语句; insert into album_attribute_value values (?,?,?);
            //				//	保存; 循环遍历执行 n 次insert 语句; insert into album_attribute_value values (?,?,?);
            //				//	保存; 循环遍历执行 n 次insert 语句; insert into album_attribute_value values (?,?,?);
            //				//	保存; 循环遍历执行 n 次insert 语句; insert into album_attribute_value values (?,?,?);
            //				//	保存; 循环遍历执行 n 次insert 语句; insert into album_attribute_value values (?,?,?) (?,?,?);
            //				albumAttributeValueMapper.insert(albumAttributeValue);
            //			}
            List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList.stream().map(albumAttributeValueVo -> {
                AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                albumAttributeValue.setAlbumId(albumInfo.getId());
                albumAttributeValue.setAttributeId(albumAttributeValueVo.getAttributeId());
                albumAttributeValue.setValueId(albumAttributeValueVo.getValueId());
                return albumAttributeValue;
            }).collect(Collectors.toList());
            //	批量添加
            albumAttributeValueService.saveBatch(albumAttributeValueList);
        }
        //	专辑统计;
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_PLAY);
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_SUBSCRIBE);
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_BROWSE);
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_COMMENT);

        //  判断当前是否需要上架：
        if ("1".equals(albumInfoVo.getIsOpen())) {
            //  将专辑Id保存到布隆过滤器
            RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
            //  调用方法;
            bloomFilter.add(albumInfo.getId());
            //  此时需要上架;
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, albumInfo.getId());
        }
    }

    /**
     * 保存专辑统计数据
     *
     * @param albumId
     * @param statPlay
     */
    public void saveAlbumStat(Long albumId, String statPlay) {
        //	创建对象
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statPlay);
        albumStat.setStatNum(new Random().nextInt(10000));
        //	保存数据;
        albumStatMapper.insert(albumStat);
    }
}
