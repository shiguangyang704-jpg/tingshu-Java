package com.atguigu.tingshu.handler;

import com.atguigu.tingshu.model.CDCEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

/**
 * @author atguigu-mqx
 * @ClassName AlbumInfoCdcHandler
 * @description: TODO
 * @date 2026年03月04日
 * @version: 1.0
 */
@Slf4j
@Component
@CanalTable("album_info")
public class AlbumInfoCdcHandler implements EntryHandler<CDCEntity> {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void insert(CDCEntity cdcEntity) {
        EntryHandler.super.insert(cdcEntity);
    }

    @Override
    public void update(CDCEntity before, CDCEntity after) {
//        EntryHandler.super.update(before, after);
        log.info("专辑信息:{}", before.getId());
        log.info("更新专辑信息:{}", after.getId());
        //  album:info:[albumId]
        try {
            String dataKey = "album:info:[" + after.getId() + "]";
            redisTemplate.delete(dataKey);
        } catch (Exception e) {
            //  记录日志.... 发邮件或警告！
            //  mq--兜底！ 消费者： 记录日志!
        }
    }

    @Override
    public void delete(CDCEntity cdcEntity) {
        EntryHandler.super.delete(cdcEntity);
    }
}
