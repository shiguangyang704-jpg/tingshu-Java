package com.atguigu.tingshu.order.config;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author atguigu-mqx
 * @ClassName CancelOrderConfig
 * @description: TODO
 * @date 2026年03月07日
 * @version: 1.0
 */
@Configuration
@Slf4j
public class CancelOrderConfig {

    //  注入Bean [交换机，队列，绑定关系]
    @Bean
    public CustomExchange cancelOrderExchange(){
        //  创建一个map集合;
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.EXCHANGE_CANCEL_ORDER, "x-delayed-message", true, false, args);
    }

    //  队列
    @Bean
    public Queue cancelOrderQueue(){
        return new Queue(MqConst.QUEUE_CANCEL_ORDER, true, false, false);
    }

    //  绑定关系
    @Bean
    public Binding cancelOrderBinding(){
        //  返回绑定关系;
        return BindingBuilder.bind(cancelOrderQueue()).to(cancelOrderExchange()).with(MqConst.ROUTING_CANCEL_ORDER).noargs();
        //  return new Binding(MqConst.QUEUE_CANCEL_ORDER, Binding.DestinationType.QUEUE, MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER, null);
    }
}
