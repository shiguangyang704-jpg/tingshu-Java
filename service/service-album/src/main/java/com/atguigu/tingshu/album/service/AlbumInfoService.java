package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {


    /**
     * 保存专辑信息
     * @param albumInfoVo
     * @param userId
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo,Long userId);

    /**
     * 获取用户专辑分页列表
     * @param page
     * @param albumInfoQuery
     * @return
     */
    IPage<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> objectPage, AlbumInfoQuery albumInfoQuery);

    /**
     * 删除专辑信息
     * @param albumId
     */
    void removeAlbumInfo(Long albumId);

    /**
     * 根据专辑id获取专辑信息
     * @param albumId
     * @return
     */
    AlbumInfo getAlbumInfo(Long albumId);

    /**
     * 修改专辑信息
     * @param albumId
     * @param albumInfoVo
     */
    void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo);

    /**
     * 获取用户所有专辑列表
     * @param userId
     * @return
     */
    List<AlbumInfo> findUserAllAlbumList(Long userId);

    /**
     * 获取专辑统计数据
     * @param albumId
     * @return
     */
    AlbumStatVo getAlbumStat(Long albumId);

}
