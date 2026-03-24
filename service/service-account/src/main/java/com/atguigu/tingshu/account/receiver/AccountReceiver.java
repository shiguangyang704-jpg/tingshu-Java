package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
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
 * @ClassName AccountReceiver
 * @description: TODO
 * @date 2026年02月24日
 * @version: 1.0
 */
@Component
@Slf4j
public class AccountReceiver {

    //  rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_USER_REGISTER, userInfo.getId());

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private RechargeInfoService rechargeInfoService;

    /**
     * 充值成功
     * @param orderNo
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_RECHARGE_PAY_SUCCESS,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER),
            key = {MqConst.ROUTING_RECHARGE_PAY_SUCCESS}
    ))
    public void rechargePaySuccess(String orderNo, Message message , Channel channel){
        System.out.println("充值成功：" + orderNo);
        try {
            //  判断
            if (!StringUtils.isEmpty(orderNo)){
                //  调用服务层方法;
                rechargeInfoService.rechargePaySuccess(orderNo);
            }
        } catch (Exception e) {
            //  异常处理：
            log.error("充值失败：" + e.getMessage());
        }
        //  手动确认
        try {
            //  第一个参数表示唯一值, 第二个参数，是否是批量确认;
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }


    /**
     * 用户注册成功
     * @param userId
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_USER_REGISTER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ACCOUNT),
            key = {MqConst.ROUTING_USER_REGISTER}
    ))
    public void userRegister(Long userId, Message message , Channel channel){
        System.out.println("用户注册成功：" + userId);
        try {
            //  判断
            if (null != userId){
                //  调用服务层方法;
                userAccountService.userAccountRegister(userId);
            }
        } catch (Exception e) {
            //  如果有异常，则记录日志信息.
            log.error("用户注册失败：" + e.getMessage());
            //  消息异常表记录; insert into mq_message_error(message_id, message, create_time, update_time) values(#{messageId}, #{message}, #{createTime}, #{updateTime})
            //  将这个消息放入死信队列;
        }
        //  消息确认;
        try {
            //  第一个参数表示唯一值, 第二个参数，是否是批量确认;
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
