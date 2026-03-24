package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    /**
     * 查询用户专辑列表
     * @param albumListVoPage
     * @param albumInfoQuery
     * @return
     */
    IPage<AlbumListVo> selectUserAlbumPage(Page<AlbumListVo> albumListVoPage, @Param("vo") AlbumInfoQuery albumInfoQuery);

    /**
     * 更新专辑播放量
     * @param albumId
     * @param count
     * @param statPlay
     * @return
     */
    @Update("update album_stat set stat_num = stat_num+#{count} where album_id = #{albumId} and stat_type = #{statPlay} and is_deleted = 0")
    int updateAlbumStat(@Param("albumId") Long albumId, @Param("count") Integer count, @Param("statPlay") String statPlay);
}
