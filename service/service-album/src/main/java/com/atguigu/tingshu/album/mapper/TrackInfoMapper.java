package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {


    /**
     * 查询用户声音列表
     *
     * @param trackListVoPage
     * @param trackInfoQuery
     * @return
     */
    Page<TrackListVo> selectUserTrackPage(Page<TrackListVo> trackListVoPage, @Param("vo") TrackInfoQuery trackInfoQuery);

    /**
     * 查询所有声音数量
     *
     * @return
     */
    @Select("SELECT COUNT(*)\n" +
            "FROM track_info track\n" +
            "WHERE track.user_id = 1\n" +
            "  AND track.is_deleted = 0\n" +
            "  AND EXISTS (\n" +
            "    SELECT 1  -- EXISTS 只关心“是否存在”，返回 1 即可（无需返回具体数据）\n" +
            "    FROM track_stat stat\n" +
            "    WHERE stat.track_id = track.id\n" +
            "      AND stat.is_deleted = 0\n" +
            ")")
    Long selectAll_count();

    /**
     * 更新专辑内声音的排序字段
     *
     * @param orderNum
     * @param albumId
     * @return
     */
    @Update("update track_info track set order_num = order_num - 1 where track.order_num > #{orderNum} and track.album_id = #{albumId} and track.is_deleted = 0")
    int updateOrderNum(@Param("orderNum") Integer orderNum, @Param("albumId") Long albumId);

    /**
     * 查询专辑内声音列表
     *
     * @param albumTrackListVoPage
     * @param albumId
     * @return
     */
    IPage<AlbumTrackListVo> selectAlbumTrackPage(Page<AlbumTrackListVo> albumTrackListVoPage, @Param("albumId") Long albumId);

    /**
     * 更新专辑内声音的统计数据
     *
     * @param trackId
     * @param count
     * @param statType
     * @return
     */
    @Update("update track_stat set stat_num = stat_num+#{count} where track_id = #{trackId} and stat_type = #{statType} and is_deleted = 0")
    int updateTrackStat(@Param("trackId") Long trackId, @Param("count") Integer count, @Param("statType") String statType);

}
