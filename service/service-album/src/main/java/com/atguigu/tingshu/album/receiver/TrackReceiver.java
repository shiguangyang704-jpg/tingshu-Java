package com.atguigu.tingshu.album.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author atguigu-mqx
 * @ClassName TrackReceiver
 * @description: TODO
 * @date 2026年03月02日
 * @version: 1.0
 */
@Component
@Slf4j
public class TrackReceiver {

    @Autowired
    private TrackInfoService trackInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    //  监听消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TRACK_STAT_UPDATE, durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_TRACK),
            key = {MqConst.ROUTING_TRACK_STAT_UPDATE}
    ))
    public void updateTrackStat(String data, Message message, Channel channel) {
        //  判断一下数据是否为空
        if (!StringUtils.isEmpty(data)){
            log.info("更新歌曲播放量：{}", data);
            //  将这个字符串再转换为Java 对象
            TrackStatMqVo trackStatMqVo = JSON.parseObject(data, TrackStatMqVo.class);
            //  防止消息重复消费; trackStatMqVo.getBusinessNo();
            Boolean result = redisTemplate.opsForValue().setIfAbsent(trackStatMqVo.getBusinessNo(), 1, 3, TimeUnit.MINUTES);
            if (result){
                try {
                    //  调用服务层方法;
                    trackInfoService.updateTrackStat(trackStatMqVo);
                } catch (Exception e) {
                    this.redisTemplate.delete(trackStatMqVo.getBusinessNo());
                    throw new RuntimeException(e);
                }
            }
        }
        //  手动ack
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
