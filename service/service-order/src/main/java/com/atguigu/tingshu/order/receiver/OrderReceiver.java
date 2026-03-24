package com.atguigu.tingshu.order.receiver;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author atguigu-mqx
 * @ClassName OrderReceiver
 * @description: TODO
 * @date 2026年03月07日
 * @version: 1.0
 */
@Component
@Slf4j
public class OrderReceiver {

    @Autowired
    private OrderInfoService orderInfoService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ORDER_PAY_SUCCESS,durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER),
            key = {MqConst.ROUTING_ORDER_PAY_SUCCESS}
    ))
    public void orderPaySuccess(String orderNo, Message message , Channel channel){
        //  判断接收消息;
        try {
            if (!StringUtils.isEmpty(orderNo)){
                //  调用服务层方法;order_info 表 的状态修改为已支付;
                orderInfoService.orderPaySuccess(orderNo);
            }
        } catch (Exception e) {
            log.error("订单支付成功失败！");
        }
        //  手动确认
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 订单取消
     * @param orderNo
     * @param message
     * @param channel
     */
    @RabbitListener(queues = MqConst.QUEUE_CANCEL_ORDER)
    public void cancelOrder(String orderNo, Message message , Channel channel){
        try {
            //  获取消息;
            if (!StringUtils.isEmpty(orderNo)){
                //  取消订单：
                orderInfoService.cancelOrder(orderNo);
            }
        } catch (Exception e) {
            log.error("订单取消失败！");
        }
        //  手动确认
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
