package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    /**
     * 上传音频文件
     * @param file
     *
     *  @return
     */
    Map<String, Object> uploadTrack(MultipartFile file);

    /**
     * 保存音频信息
     * @param trackInfoVo
     */
    void saveTrackInfo(TrackInfoVo trackInfoVo,Long userId);

    /**
     * 获取用户音频列表
     * @param trackListVoPage
     * @param trackInfoQuery
     * @return
     */
    Page<TrackListVo> findUserTrackPage(Page<TrackListVo> trackListVoPage, TrackInfoQuery trackInfoQuery);


    /**
     * 删除音频信息
     * @param trackId
     * @return
     */
    boolean removeTrackInfo(Long trackId);

    /**
     * 修改音频信息
     * @param trackId
     * @param trackInfoVo
     */
    void updateTrackInfo(Long trackId, TrackInfoVo trackInfoVo);

    /**
     * 根据专辑Id获取声音分页列表
     * @param albumId
     * @param pageNo
     * @param pageSize
     * @return
     */
    IPage<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> albumTrackListVoPage, Long albumId, Long userId);

    /**
     * 更新声音播放量
     * @param trackStatMqVo
     */
    void updateTrackStat(TrackStatMqVo trackStatMqVo);

    /**
     * 获取用户付费的专辑声音列表
     * @param trackId
     * @return
     */
    List<Map<String, Object>> findUserTrackPaidList(Long trackId);

    /**
     * 获取付费声音列表
     * @param trackId
     * @param trackCount
     * @return
     */
    List<TrackInfo> findPaidTrackInfoList(Long trackId, Integer trackCount);
}
