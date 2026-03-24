package com.atguigu.tingshu.search.service;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SearchService {


    /**
     * 上架专辑
     * @param albumId
     */
    void upperAlbum(Long albumId);

    /**
     * 下架专辑
     * @param albumId
     */
    void lowerAlbum(Long albumId);

    /**
     * 专辑检索
     * @param albumIndexQuery
     * @return
     */
    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    /**
     * 频道
     * @param category1Id
     * @return
     */
    List<Map<String, Object>> channel(Long category1Id);

    /**
     * 专辑自动补全
     * @param keyword
     * @return
     */
    List<String> completeSuggest(String keyword);

    /**
     * 更新最近专辑排行
     */
    void updateLatelyAlbumRanking();


    /**
     * 获取专辑排行
     * @param category1Id
     * @param rangking
     * @return
     */
    List<AlbumInfoIndex> findRankingList(Long category1Id, String rangking);
}
