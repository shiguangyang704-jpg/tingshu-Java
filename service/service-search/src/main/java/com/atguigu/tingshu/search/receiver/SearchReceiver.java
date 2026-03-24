package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.search.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author atguigu-mqx
 * @ClassName SearchReceiver
 * @description: TODO
 * @date 2026年02月25日
 * @version: 1.0
 */
@Component
@Slf4j
public class SearchReceiver {


    @Autowired
    private SearchService searchService;

    /**
     * 专辑上架
     * @param albumId
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ALBUM_UPPER, durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM),
            key = {MqConst.ROUTING_ALBUM_UPPER}
    ))
    public void albumUpper(Long albumId, Message message , Channel channel) {
        try {
            //  判断：
            if (null != albumId){
                //  调用服务层方法
                searchService.upperAlbum(albumId);
            }
        } catch (Exception e) {
            log.error("上架异常：" + e.getMessage());
        }
        //  消息确认;
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 专辑下架
     * @param albumId
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ALBUM_LOWER, durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM),
            key = {MqConst.ROUTING_ALBUM_LOWER}
    ))
    public void albumLower(Long albumId, Message message , Channel channel) {
        try {
            //  判断：
            if (null != albumId){
                //  调用服务层方法
                searchService.lowerAlbum(albumId);
            }
        } catch (Exception e) {
            log.error("下架异常：" + e.getMessage());
        }
        //  消息确认;
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
