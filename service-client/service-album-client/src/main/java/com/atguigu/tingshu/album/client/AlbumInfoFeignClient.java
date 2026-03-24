package com.atguigu.tingshu.album.client;

import com.atguigu.tingshu.album.client.impl.AlbumInfoDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 */
@FeignClient(value = "service-album", fallback = AlbumInfoDegradeFeignClient.class)
public interface AlbumInfoFeignClient {

    /**
     * 根据id查询
     *
     * @param albumId
     * @return
     */
    @GetMapping("api/album/albumInfo/getAlbumInfo/{albumId}")
    Result<AlbumInfo> getAlbumInfo(@PathVariable Long albumId);

    /**
     * 获取专辑统计数据
     *
     * @param albumId
     * @return
     */
    @GetMapping("api/album/albumInfo/getAlbumStatVo/{albumId}")
    Result<AlbumStatVo> getAlbumStat(@PathVariable Long albumId);
}