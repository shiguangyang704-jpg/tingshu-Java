package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kotlin.jvm.Volatile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album/albumInfo")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;




    /**
     * 获取专辑统计数据
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "获取专辑统计数据")
    @GetMapping("/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStat(@PathVariable Long albumId){
        //  调用服务层方法;
        AlbumStatVo albumStatVo = albumInfoService.getAlbumStat(albumId);
        //  返回数据
        return Result.ok(albumStatVo);
    }

    /**
     * 查询用户对应的专辑列表
     *
     * @return
     */
    @Operation(summary = "查询用户对应的专辑列表")
    @GetMapping("/findUserAllAlbumList")
    public Result findUserAllAlbumList() {
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId() == null ? 1L : AuthContextHolder.getUserId();
        //  调用服务层方法
        List<AlbumInfo> albumInfoList = albumInfoService.findUserAllAlbumList(userId);
        //	返回数据
        return Result.ok(albumInfoList);
    }

    /**
     * 更新专辑
     * 以Json 格式传递的!
     *
     * @return
     */
    @Operation(summary = "更新专辑")
    @PutMapping("/updateAlbumInfo/{albumId}")
    public Result updateAlbumInfo(@PathVariable Long albumId,
                                  @RequestBody @Validated AlbumInfoVo albumInfoVo) {
        //	调用服务层方法
        albumInfoService.updateAlbumInfo(albumId,albumInfoVo);
        //	返回数据
        return Result.ok();
    }

    /**
     * 根据专辑Id获取到专辑数据
     *
     * @return
     */
    @Operation(summary = "根据专辑Id获取到专辑数据")
    @GetMapping("/getAlbumInfo/{albumId}")
    public Result getAlbumInfo(@PathVariable Long albumId) {
        //	调用服务层方法
        AlbumInfo albumInfo = albumInfoService.getAlbumInfo(albumId);
        //	返回数据
        return Result.ok(albumInfo);
    }


    /**
     * 删除专辑
     *
     * @return
     */
    @Operation(summary = "根据专辑Id删除专辑对应数据")
    @DeleteMapping("/removeAlbumInfo/{albumId}")
    public Result removeAlbumInfo(@PathVariable Long albumId) {
        //	调用服务层方法
        albumInfoService.removeAlbumInfo(albumId);
        //	返回数据
        return Result.ok();
    }

    /**
     * 分页查看专辑列表
     *
     * @return
     */
    @TsLogin
    @Operation(summary = "分页查看专辑列表")
    @PostMapping("/findUserAlbumPage/{pageNo}/{pageSize}")
    public Result findUserAlbumPage(@PathVariable Long pageNo,
                                    @PathVariable Long pageSize,
                                    @RequestBody AlbumInfoQuery albumInfoQuery) {
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId() == null ? 1L : AuthContextHolder.getUserId();
        //  赋值：
        albumInfoQuery.setUserId(userId);
        //  需要将第几页与每页显示的条数进行封装给page 对象
        Page<AlbumListVo> objectPage = new Page<>(pageNo, pageSize);
        //  调用服务层方法;
        IPage<AlbumListVo> iPage = albumInfoService.findUserAlbumPage(objectPage, albumInfoQuery);
        //	调用服务层方法
        return Result.ok(iPage);
    }


    /**
     * 保存专辑
     * 应该要求必须登录才能使用！
     *
     * @return
     */
    @TsLogin
    @Operation(summary = "保存专辑")
    @PostMapping("/saveAlbumInfo")
    public Result saveAlbumInfo(@RequestBody @Validated AlbumInfoVo albumInfoVo) {
        //	登录模块写完之后，会将用户信息存储到本地线程中！
        Long userId = AuthContextHolder.getUserId() == null ? 1L : AuthContextHolder.getUserId();
        //	调用服务层方法
        albumInfoService.saveAlbumInfo(albumInfoVo, userId);
        //	返回数据
        return Result.ok();
    }


}

