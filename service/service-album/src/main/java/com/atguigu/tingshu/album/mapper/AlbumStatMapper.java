package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AlbumStatMapper extends BaseMapper<AlbumStat> {


    /**
     * 根据专辑Id查询专辑统计数据
     * @param albumId
     * @return
     */
    @Select("select album_id,\n" +
            "       max(if(stat.stat_type = '0401', stat.stat_num, 0)) as playStatNum,\n" +
            "       max(if(stat.stat_type = '0402', stat.stat_num, 0)) as subscribeStatNum,\n" +
            "       max(if(stat.stat_type = '0403', stat.stat_num, 0)) as buyStatNum,\n" +
            "       max(if(stat.stat_type = '0404', stat.stat_num, 0)) as commentStatNum\n" +
            "from album_stat stat\n" +
            "where album_id = #{albumId}")
    AlbumStatVo selectAlbumStat(@Param("albumId") Long albumId);

}
