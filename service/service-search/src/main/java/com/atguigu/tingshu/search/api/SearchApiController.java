package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    /**
     * 获取排行榜数据
     * @param category1Id
     * @param rangking
     * @return
     */
    @Operation(summary = "获取排行榜数据")
    @GetMapping("/findRankingList/{category1Id}/{rangking}")
    public Result findRankingList(@PathVariable Long category1Id, @PathVariable String rangking) {
        //  调用服务层方法
        List<AlbumInfoIndex> list = searchService.findRankingList(category1Id, rangking);
        //  返回数据
        return Result.ok(list);
    }

    /**
     * 更新最近播放专辑排行
     * @return
     */
    @Operation(summary = "更新排行榜")
    @GetMapping("updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking() {
        //  调用服务层方法
        searchService.updateLatelyAlbumRanking();
        //  返回数据
        return Result.ok();
    }

    /**
     * 自动补全
     * @param keyword
     * @return
     */
    @Operation(summary = "自动补全")
    @GetMapping("/completeSuggest/{keyword}")
    public Result completeSuggest(@PathVariable String keyword) {
        //  调用服务层方法
        List<String> list = searchService.completeSuggest(keyword);
        //  返回数据
        return Result.ok(list);
    }

    /**
     * 频道页数据展示
     * @return
     */
    @Operation(summary = "频道页数据展示")
    @GetMapping("/channel/{category1Id}")
    public Result channel(@PathVariable Long category1Id) {
        //  调用服务层方法
        //  返回的时候：需要一个三级分类名称-key=baseCategory3 key=list value=三级分类对象或三级分类名称 分类下对应的专辑列表-value=List<Album>
        //  class Param{ private String baseCategory3; private List<Album> list;}
        //  List<T>
        List<Map<String, Object>> list = searchService.channel(category1Id);
        return Result.ok(list);
    }

    /**
     * 检索
     * @return
     */
    @Operation(summary = "检索")
    @PostMapping
    public Result search(@RequestBody AlbumIndexQuery albumIndexQuery) {
        //  调用服务层方法
        AlbumSearchResponseVo albumSearchResponseVo = searchService.search(albumIndexQuery);
        return Result.ok(albumSearchResponseVo);
    }


    /**
     * 批量上架
     * @return
     */
    @Operation(summary = "批量上架")
    @GetMapping("batchUpperAlbum")
    public Result batchUpperAlbum(){
        //  循环
        for (long i = 1; i <= 1500; i++) {
            searchService.upperAlbum(i);
        }
        //  返回数据
        return Result.ok();
    }

    /**
     * 上架专辑
     * @param albumId
     * @return
     */
    @Operation(summary = "上架专辑")
    @GetMapping("/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId) {
        //  调用服务层方法;
        searchService.upperAlbum(albumId);
        return Result.ok();
    }
    /**
     * 下架专辑
     * @param albumId
     * @return
     */
    @Operation(summary = "下架专辑")
    @GetMapping("/lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId) {
        //  调用服务层方法;
        searchService.lowerAlbum(albumId);
        return Result.ok();
    }

}

