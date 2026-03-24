package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.config.redis.RedisConfig;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private CategoryFeignClient categoryFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisConfig redisConfig;


    @Override
    public Map<String, Object> getItemInfo(Long albumId) {
        //  创建map;
        Map<String, Object> map = new HashMap<>();
        //  判断布隆过滤器中是否存在！
        //        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        //        if (!bloomFilter.contains(albumId)){
        //            //  返回空数据
        //            return map;
        //        }
        //  优化：
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Result<AlbumInfo> infoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            //  判断：
            Assert.notNull(infoResult, "专辑远程调用数据不存在！");
            AlbumInfo albumInfo = infoResult.getData();
            Assert.notNull(albumInfo, "专辑数据不存在！");
            map.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolExecutor);


        //  获取数据：
        CompletableFuture<Void> statCompletableFuture = CompletableFuture.runAsync(() -> {
            Result<AlbumStatVo> statVoResult = albumInfoFeignClient.getAlbumStat(albumId);
            Assert.notNull(statVoResult, "专辑统计数据不存在！");
            AlbumStatVo albumStatVo = statVoResult.getData();
            Assert.notNull(albumStatVo, "统计数据不存在！");
            map.put("albumStatVo", albumStatVo);
        }, threadPoolExecutor);

        //  获取分类数据;
        CompletableFuture<Void> categoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<BaseCategoryView> categoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
            Assert.notNull(categoryViewResult, "分类远程调用数据不存在！");
            BaseCategoryView baseCategoryView = categoryViewResult.getData();
            Assert.notNull(baseCategoryView, "分类数据不存在！");
            map.put("baseCategoryView", baseCategoryView);
        }, threadPoolExecutor);

        //  作者：
        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(albumInfo.getUserId());
            Assert.notNull(userInfoVoResult, "作者远程调用数据不存在！");
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            Assert.notNull(userInfoVo, "作者数据不存在！");
            map.put("announcer", userInfoVo);
        },threadPoolExecutor);

        //  组合：
        CompletableFuture.allOf(albumInfoCompletableFuture, statCompletableFuture, categoryViewCompletableFuture, userCompletableFuture).join();
        //  返回数据;
        return map;
    }
}
