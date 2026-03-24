package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.*;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private TrackStatMapper trackStatMapper;

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Autowired
    private VodService vodService;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Override
    public IPage<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> albumTrackListVoPage, Long albumId, Long userId) {
        //  1.  先知道当前专辑Id对应有多少声音列表！
        IPage<AlbumTrackListVo> iPage = trackInfoMapper.selectAlbumTrackPage(albumTrackListVoPage, albumId);
        //  专辑的类型：免费，vip免费，付费;
        //  是否登录： 未登录：除免费之外都需要付费！ 登录：普通，vip用户
        //  获取到当前专辑对象;
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        if (null == userId) {
            //  说明当前用户未登录;
            if (!SystemConstant.ALBUM_PAY_TYPE_FREE.equals(albumInfo.getPayType())) {
                //  vip免费或付费情况;
                //  让专辑对应声音列表中除去试听集数外；都应该付费！isShowPaidMark = true;
                //  albumInfo.getTracksForFree(); 获取免费试听集数;
                iPage.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() > albumInfo.getTracksForFree())
                        .forEach(albumTrackListVo -> albumTrackListVo.setIsShowPaidMark(true));
            }
        } else {
            //  在外层声明一个变量：
            boolean isPaid = false;
            //    用户登录状态; 判断专辑类型;
            if (SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(albumInfo.getPayType())) {
                //  vip免费的条件
                //  判断当前用户身份：通过用户Id查找用户信息;
                Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
                Assert.notNull(userInfoVoResult, "用户信息不存在！");
                UserInfoVo userInfoVo = userInfoVoResult.getData();
                Assert.notNull(userInfoVo, "用户信息不存在！");
                //  不是vip的情况需要付费；
                if (userInfoVo.getIsVip() == 0 || (userInfoVo.getIsVip() == 1 && userInfoVo.getVipExpireTime().before(new Date()))) {
                    isPaid = true;
                }
            } else if (SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(albumInfo.getPayType())) {
                //  付费
                isPaid = true;
            }
            //  统一处理付费情况;
            if (isPaid) {
                //  找出哪些是需要付费的声音数据;
                List<AlbumTrackListVo> albumTrackListVoList = iPage.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() > albumInfo.getTracksForFree()).collect(Collectors.toList());
                //  存储了哪些需要付费的声音集合：albumTrackListVoList
                //  这个用户是否曾经购买过专辑或购买过当前某些声音！user_paid_album user_paid_track
                //  获取需要付费的声音Id列表：
                List<Long> trackIdPaidList = albumTrackListVoList.stream().map(AlbumTrackListVo::getTrackId).collect(Collectors.toList());
                //  返回一个map集合：key=trackId: value=0/1  1:用户已购买过 0：未购买过
                Result<Map<Long, Integer>> mapResult = userInfoFeignClient.userIsPaidTrack(albumId, trackIdPaidList);
                Assert.notNull(mapResult, "付费情况获取失败！");
                Map<Long, Integer> map = mapResult.getData();
                Assert.notNull(map, "付费情况获取失败！");
                //  设置哪些需要付费：
                for (AlbumTrackListVo albumTrackListVo : albumTrackListVoList) {
                    //  setIsShowPaidMark:默认值false;什么时候付钱;key=trackId value=0  true;
                    albumTrackListVo.setIsShowPaidMark(map.get(albumTrackListVo.getTrackId()) == 0);
                }
            }
        }
        //  返回数据;
        return iPage;
    }

    @Override
    public List<TrackInfo> findPaidTrackInfoList(Long trackId, Integer trackCount) {
        //  创建集合列表;
        List<TrackInfo> paidTrackInfoList = new ArrayList<>();
        //  根据声音Id查询声音对象; trackCount=0; select * from track_info where id = 49166;
        TrackInfo trackInfo = this.getById(trackId);
        //  trackCount>0； select * from track_info where order_num > 6 and album_id = 1466 limit 10;
        if (trackCount > 0){
            //  构建查询条件.;
            LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId())
                    .gt(TrackInfo::getOrderNum, trackInfo.getOrderNum())
                    .last(" limit " + trackCount);
            //  查询所有专辑列表;
            paidTrackInfoList = trackInfoMapper.selectList(wrapper);
        } else {
            //  trackCount=0;说明购买的是本集;
            paidTrackInfoList.add(trackInfo);
        }
        //  返回数据;
        return paidTrackInfoList;
    }

    @Override
    public List<Map<String, Object>> findUserTrackPaidList(Long trackId) {
        //  构建数据;
        List<Map<String, Object>> list = new ArrayList<>();
        //  根据声音Id获取到声音对象;
        TrackInfo trackInfo = this.getById(trackId);
        //  获取需要付费的声音集合列表;
        //  gt:> ge:>=
        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId())
                .gt(TrackInfo::getOrderNum, trackInfo.getOrderNum());
        //  获取到收费的声音列表； 1,2,3,4,5,6;
        List<TrackInfo> trackInfoList = trackInfoMapper.selectList(wrapper);
        //  所有需要付费的声音Id集合；
        List<Long> allTrackIdList = new ArrayList<>();
        //  业务实现：
        //  获取到当前用户已购买的声音Id集合; 2,3;
        Result<List<Long>> trackIdListResult = userInfoFeignClient.findUserPaidTrackList(trackInfo.getAlbumId());
        Assert.notNull(trackIdListResult, "用户已购买的声音Id集合获取失败！");
        //  判断
        List<Long> trackIdList = trackIdListResult.getData();
        //  过滤数据：没有购买过声音;
        if (CollectionUtils.isEmpty(trackIdList)) {
            //  说明没有购买过声音;
            allTrackIdList = trackInfoList.stream().map(TrackInfo::getId).collect(Collectors.toList());
        } else {
            //  购买过;
            //  List<TrackInfo> infoList = trackInfoL1ist.stream().filter(tInfo -> !trackIdList.contains(tInfo.getId())).collect(Collectors.toList());
            allTrackIdList = allTrackIdList.stream().filter(tId -> !trackIdList.contains(tId)).collect(Collectors.toList());
        }
        //  获取声音单价；
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
        //  allTrackIdList 这个集合记录所有需要付费的声音Id;
        if (allTrackIdList.size() >= 0) {
            //  声明一个Map集合；
            HashMap<String, Object> map = new HashMap<>();
            //  赋值：
            map.put("name", "本集");
            //  单条声音价格等同于专辑的单价;
            map.put("price", albumInfo.getPrice());
            //  购买集数0表示当前用户点击的这条声音！
            map.put("trackCount", 0);
            //  添加到集合中;
            list.add(map);
        }
        //  什么时候回出现后10集;  从用户点击的哪条声音开始：只剩下10集需要付费; 则；显示本集，后10集; 如果剩余9集; 显示本集，后9集；
        if (allTrackIdList.size() > 0 && allTrackIdList.size() <= 10) {
            //  声明一个Map集合；
            HashMap<String, Object> map = new HashMap<>();
            //  赋值：
            map.put("name", "后" + allTrackIdList.size() + "集");
            //  单条声音价格等同于专辑的单价;
            map.put("price", albumInfo.getPrice().multiply(BigDecimal.valueOf(allTrackIdList.size())));
            //  购买集数0表示当前用户点击的这条声音！
            map.put("trackCount", allTrackIdList.size());
            //  添加到集合中;
            list.add(map);
        }
        //  从用户点击的哪条声音开始：只剩下18集需要付费; 则；显示本集，后10集; 后18集；
        if (allTrackIdList.size() > 10) {
            //  声明一个Map集合；
            HashMap<String, Object> map = new HashMap<>();
            //  赋值：
            map.put("name", "后10集");
            //  单条声音价格等同于专辑的单价;
            map.put("price", albumInfo.getPrice().multiply(new BigDecimal("10")));
            //  购买集数0表示当前用户点击的这条声音！
            map.put("trackCount", 10);
            //  添加到集合中;
            list.add(map);
        }

        //  从用户点击的哪条声音开始：只剩下18集需要付费;
        if (allTrackIdList.size() > 10 && allTrackIdList.size() <= 20) {
            //  声明一个Map集合；
            HashMap<String, Object> map = new HashMap<>();
            //  赋值：
            map.put("name", "后" + allTrackIdList.size() + "集");
            //  单条声音价格等同于专辑的单价;
            map.put("price", albumInfo.getPrice().multiply(BigDecimal.valueOf(allTrackIdList.size())));
            //  购买集数0表示当前用户点击的这条声音！
            map.put("trackCount", allTrackIdList.size());
            //  添加到集合中;
            list.add(map);
        }
        //  从用户点击的哪条声音开始：只剩下18集需要付费; 则；显示本集，后10集; 后18集；
        if (allTrackIdList.size() > 20) {
            //  声明一个Map集合；
            HashMap<String, Object> map = new HashMap<>();
            //  赋值：
            map.put("name", "后20集");
            //  单条声音价格等同于专辑的单价;
            map.put("price", albumInfo.getPrice().multiply(new BigDecimal("20")));
            //  购买集数0表示当前用户点击的这条声音！
            map.put("trackCount", 20);
            //  添加到集合中;
            list.add(map);
        }

        //  从用户点击的哪条声音开始：只剩下18集需要付费;
        if (allTrackIdList.size() > 20 && allTrackIdList.size() <= 30) {
            //  声明一个Map集合；
            HashMap<String, Object> map = new HashMap<>();
            //  赋值：
            map.put("name", "后" + allTrackIdList.size() + "集");
            //  单条声音价格等同于专辑的单价;
            map.put("price", albumInfo.getPrice().multiply(BigDecimal.valueOf(allTrackIdList.size())));
            //  购买集数0表示当前用户点击的这条声音！
            map.put("trackCount", allTrackIdList.size());
            //  添加到集合中;
            list.add(map);
        }
        //  从用户点击的哪条声音开始：只剩下18集需要付费; 则；显示本集，后10集; 后18集；
        if (allTrackIdList.size() > 30) {
            //  声明一个Map集合；
            HashMap<String, Object> map = new HashMap<>();
            //  赋值：
            map.put("name", "后30集");
            //  单条声音价格等同于专辑的单价;
            map.put("price", albumInfo.getPrice().multiply(new BigDecimal("30")));
            //  购买集数0表示当前用户点击的这条声音！
            map.put("trackCount", 30);
            //  添加到集合中;
            list.add(map);
        }
        //  返回数据;
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrackStat(TrackStatMqVo trackStatMqVo) {
        //  更新声音播放量;
        log.info("更新声音播放量：{}", trackStatMqVo.getTrackId());
        //  update track_stat set stat_num = stat_num+1 where track_id = 52003 and stat_type = '0701' and is_deleted = 0
        trackInfoMapper.updateTrackStat(trackStatMqVo.getTrackId(), trackStatMqVo.getCount(), trackStatMqVo.getStatType());
        //  判断要更新专辑的类型;
        if (SystemConstant.TRACK_STAT_PLAY.equals(trackStatMqVo.getStatType())) {
            //  更新专辑播放量;
            log.info("更新专辑播放量：{}", trackStatMqVo.getAlbumId());
            albumInfoMapper.updateAlbumStat(trackStatMqVo.getAlbumId(), trackStatMqVo.getCount(), SystemConstant.ALBUM_STAT_PLAY);
        } else if (SystemConstant.TRACK_STAT_COMMENT.equals(trackStatMqVo.getStatType())) {
            log.info("更新专辑评论量：{}", trackStatMqVo.getAlbumId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrackInfo(Long trackId, TrackInfoVo trackInfoVo) {
        String originMediaFileId = null;
        try {
            //  track_info; 修改;
            //  创建一个trackInfo 对象;
            TrackInfo trackInfo = this.getById(trackId);
            //  获取到原始声音Id
            originMediaFileId = trackInfo.getMediaFileId();
            //  进行属性拷贝; 主要修改简介，封面等数据;
            BeanUtils.copyProperties(trackInfoVo, trackInfo);
            //  是否更新了声音：
            boolean flag = false;
            //  判断用户是否修改了声音;
            if (!originMediaFileId.equals(trackInfoVo.getMediaFileId())) {
                //  修改了声音; 获取最新的相关数据;
                TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
                if (null == mediaInfo) {
                    throw new GuiguException(808, "声音不存在！");
                }
                //  赋值最新数据;
                trackInfo.setMediaDuration(mediaInfo.getDuration());
                trackInfo.setMediaSize(mediaInfo.getSize());
                trackInfo.setMediaType(mediaInfo.getType());
                trackInfo.setMediaUrl(mediaInfo.getMediaUrl());
                //  更新了声音
                flag = true;
            }
            //  更新声音数据; 数据库更新，插入，删除的时候返回受影响的行数;
            trackInfoMapper.updateById(trackInfo);
            //  修改了声音！
            if (flag) {
                //  删除云点播;
                vodService.deleteVod(originMediaFileId);
            }
        } catch (BeansException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (GuiguException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTrackInfo(Long trackId) {
        //  创建一个对象;
        Boolean flag = false;
        TrackInfo trackInfo = null;
        try {
            //  先获取到声音对象
            trackInfo = this.getById(trackId);
            //  删除数据：track_info track_stat album_info vod--删除;
            trackInfoMapper.deleteById(trackId);
            //  track_stat
            trackStatMapper.delete(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, trackId));
            //  album_info
            AlbumInfo albumInfo = this.albumInfoMapper.selectById(trackInfo.getAlbumId());
            if (null != albumInfo && albumInfo.getIncludeTrackCount() > 0) {
                albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
                albumInfoMapper.updateById(albumInfo);
            }
            //  维护order_num 字段;
            //  47:对应的最后一条数据;
            //  update track_info track set order_num = order_num - 1 where order_num > 47 and album_id = 1
            //  如果这个专辑只有一条声音!
            int result = trackInfoMapper.updateOrderNum(trackInfo.getOrderNum(), trackInfo.getAlbumId());
            // select order_num from track_info where album_id = 2 and is_deleted = 0 order by order_num desc;
            LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId())
                    .orderByDesc(TrackInfo::getOrderNum);
            //  54;
            TrackInfo trackInfoOrderNum = trackInfoMapper.selectOne(wrapper);
            //  删除成功：除去最后一行数据;
            if (null != trackInfoOrderNum && trackInfoOrderNum.getOrderNum() == trackInfo.getOrderNum() || result > 0) {
                //  说明是最后一行;
                flag = true;
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new GuiguException(809, "删除失败！");
        }
        //  删除云点播;
        Boolean result = vodService.deleteVod(trackInfo.getMediaFileId());
        if (result && flag) {
            return true;
        }
        return false;
    }

    @Override
    public Page<TrackListVo> findUserTrackPage(Page<TrackListVo> trackListVoPage, TrackInfoQuery trackInfoQuery) {
        //  调用mapper 层
        Page<TrackListVo> trackListVoPage1 = trackInfoMapper.selectUserTrackPage(trackListVoPage, trackInfoQuery);
        //  获取总共数据;
        trackListVoPage.setTotal(trackInfoMapper.selectAll_count());
        //  返回数据
        return trackListVoPage1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {
        //	track_info--声音表; track_stat--统计表; album_info--include_track_count
        TrackInfo trackInfo = new TrackInfo();
        //	赋值：
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        //	赋值：前端没有传递的数据：
        trackInfo.setUserId(userId);
        //  order_num = album_info.include_track_count+1
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
        trackInfo.setOrderNum(albumInfo.getIncludeTrackCount() + 1);
        //  根据流媒体文件Id获取到流媒体对象数据;
        TrackMediaInfoVo trackMediaInfoVo = vodService.getMediaInfo(trackInfo.getMediaFileId());
        //  赋值：
        if (null != trackMediaInfoVo) {
            trackInfo.setMediaDuration(trackMediaInfoVo.getDuration());
            trackInfo.setMediaSize(trackMediaInfoVo.getSize());
            trackInfo.setMediaType(trackMediaInfoVo.getType());
            trackInfo.setMediaUrl(trackMediaInfoVo.getMediaUrl());
        }
        //	保存数据：
        trackInfoMapper.insert(trackInfo);
        //  保存声音统计信息;
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PLAY);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COLLECT);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PRAISE);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COMMENT);
        //  更新专辑信息表;
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
        this.albumInfoMapper.updateById(albumInfo);
    }

    private void saveTrackStat(Long trackId, String statPlay) {
        //  创建对象
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statPlay);
        trackStat.setStatNum(new Random().nextInt(10000));
        trackStatMapper.insert(trackStat);
    }

    @Override
    public Map<String, Object> uploadTrack(MultipartFile file) {
        //	创建一个map 集合
        Map<String, Object> map = new HashMap<>();
        //	设置对象存储路径：
        String tempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
        //	调用api接口：
        VodUploadClient client = new VodUploadClient(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
        //	构建上传请求对象;
        VodUploadRequest request = new VodUploadRequest();
        request.setMediaFilePath(tempPath);
        //	上传声音：
        try {
            VodUploadResponse response = client.upload(vodConstantProperties.getRegion(), request);
            log.info("Upload FileId = {}", response.getFileId());
            //	存储流媒体文件Id与url地址;
            map.put("mediaFileId", response.getFileId());
            map.put("mediaUrl", response.getMediaUrl());
            //	返回数据;
            return map;
        } catch (Exception e) {
            // 业务方进行异常处理
            log.error("Upload Err", e);
        }
        return map;
    }
}
