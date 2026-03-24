package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album/trackInfo")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

    @Autowired
    private TrackInfoService trackInfoService;

    /**
     * 批量获取下单付费声音列表
     * @param trackId
     * @param trackCount
     * @return
     */
    @Operation(summary = "批量获取下单付费声音列表")
    @GetMapping("/findPaidTrackInfoList/{trackId}/{trackCount}")
    public Result<List<TrackInfo>> findPaidTrackInfoList(@PathVariable("trackId") Long trackId, @PathVariable("trackCount") Integer trackCount){
        //  调用服务层方法;
        List<TrackInfo>  list = trackInfoService.findPaidTrackInfoList(trackId, trackCount);
        //  返回数据;
        return Result.ok(list);
    }


    /**
     * 获取用户付费的专辑声音列表
     *
     * @return
     */
    @Operation(summary = "获取专辑对应的声音列表")
    @GetMapping("/findUserTrackPaidList/{trackId}")
    public Result  findUserTrackPaidList(@PathVariable Long trackId) {
        //  获取用户Id
        //        Long userId = AuthContextHolder.getUserId();
        //  调用服务层方法
        List<Map<String,Object>> list = trackInfoService.findUserTrackPaidList(trackId);
        //  返回数据
        return Result.ok(list);
    }


    /**
     * 根据专辑Id获取声音分页列表
     *
     * @return
     */
    @TsLogin(requireLogin = false)
    @Operation(summary = "根据专辑Id获取声音分页列表")
    @GetMapping("/findAlbumTrackPage/{albumId}/{pageNo}/{pageSize}")
    public Result findAlbumTrackPage(@PathVariable Long albumId,
                                     @PathVariable Long pageNo,
                                     @PathVariable Long pageSize) {
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //  创建分页对象
        Page<AlbumTrackListVo> albumTrackListVoPage = new Page<>(pageNo, pageSize);
        //  调用服务层方法
        IPage<AlbumTrackListVo> pageModel = trackInfoService.findAlbumTrackPage(albumTrackListVoPage, albumId,userId);
        //  返回数据
        return Result.ok(pageModel);
    }

    /**
     * 修改声音数据
     *
     * @return
     */
    @Operation(summary = "修改声音数据")
    @PutMapping("/updateTrackInfo/{trackId}")
    public Result updateTrackInfo(@PathVariable Long trackId,
                                  @RequestBody TrackInfoVo trackInfoVo) {
        //  调用服务层方法
        trackInfoService.updateTrackInfo(trackId, trackInfoVo);
        //  返回数据
        return Result.ok();
    }

    /**
     * 根据声音Id获取到声音数据
     *
     * @return
     */
    @Operation(summary = "根据声音Id获取到声音数据")
    @GetMapping("/getTrackInfo/{trackId}")
    public Result getTrackInfo(@PathVariable Long trackId) {
        //  调用服务层方法. 专辑分类： albumId-->获取专辑数据!
        TrackInfo trackInfo = trackInfoService.getById(trackId);
        //  返回数据
        return Result.ok(trackInfo);
    }

    /**
     * 删除声音
     *
     * @return
     */
    @Operation(summary = "删除声音")
    @DeleteMapping("/removeTrackInfo/{trackId}")
    public Result removeTrackInfo(@PathVariable Long trackId) {
        //  调用service方法
        boolean result = trackInfoService.removeTrackInfo(trackId);
        //  判断结果
        if (result) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    /**
     * 分页查询声音列表
     *
     * @return
     */
    @TsLogin
    @Operation(summary = "分页查询声音列表")
    @PostMapping("/findUserTrackPage/{pageNo}/{pageSize}")
    public Result findUserTrackPage(@PathVariable Long pageNo,
                                    @PathVariable Long pageSize,
                                    @RequestBody TrackInfoQuery trackInfoQuery,
                                    HttpServletRequest request) {
        request.getHeader("token");
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId() == null ? 1L : AuthContextHolder.getUserId();
        trackInfoQuery.setUserId(userId);
        Page<TrackListVo> trackListVoPage = new Page<>(pageNo, pageSize);
        //  关闭查询总数据：
        trackListVoPage.setSearchCount(false);
        //	分页查询数据
        Page<TrackListVo> pageModel = trackInfoService.findUserTrackPage(trackListVoPage, trackInfoQuery);
        //	返回数据
        return Result.ok(pageModel);
    }


    /**
     * 保存声音
     *
     * @return
     */
    @TsLogin
    @Operation(summary = "保存声音")
    @PostMapping("/saveTrackInfo")
    public Result saveTrackInfo(@RequestBody TrackInfoVo trackInfoVo) {
        //	先登录，从本地线程中获取数据;
        Long userId = AuthContextHolder.getUserId() == null ? 1L : AuthContextHolder.getUserId();
        //	保存声音数据
        trackInfoService.saveTrackInfo(trackInfoVo, userId);
        //	返回数据
        return Result.ok();
    }


    /**
     * 声音上传
     *
     * @return
     */
    @Operation(summary = "声音上传")
    @PostMapping("/uploadTrack")
    public Result uploadTrack(MultipartFile file) {
        //	获取到声音文件数据;
        Map<String, Object> map = trackInfoService.uploadTrack(file);
        //	返回数据;
        return Result.ok(map);
    }

}

