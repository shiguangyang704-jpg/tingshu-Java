package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 根据订单编号获取订单信息
     *
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfoByOrderNo(String orderNo);


    /**
     * 订单交易
     *
     * @param tradeVo
     * @param userId
     * @return
     */
    OrderInfoVo trade(TradeVo tradeVo, Long userId);

    /**
     * 提交订单
     *
     * @param orderInfoVo
     * @param userId
     * @return
     */
    String submitOrder(OrderInfoVo orderInfoVo, Long userId);

    /**
     * 取消订单
     *
     * @param orderNo
     */
    void cancelOrder(String orderNo);

    /**
     * 获取用户订单列表
     *
     * @param orderInfoPage
     * @param userId
     * @param orderStatus
     * @return
     */
    IPage<OrderInfo> findUserPage(Page<OrderInfo> orderInfoPage, Long userId, String orderStatus);

    /**
     * 订单支付成功
     *
     * @param orderNo
     */
    void orderPaySuccess(String orderNo);
}
