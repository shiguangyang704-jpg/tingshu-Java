package com.atguigu.tingshu.album.client.impl;


import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlbumInfoDegradeFeignClient implements AlbumInfoFeignClient {


    @Override
    public Result<AlbumStatVo> getAlbumStat(Long albumId) {
        return null;
    }

    @Override
    public Result<AlbumInfo> getAlbumInfo(Long albumId) {
        log.error("AlbumInfoDegradeFeignClient.getAlbumInfo() 熔断...");
        return null;
    }
}
