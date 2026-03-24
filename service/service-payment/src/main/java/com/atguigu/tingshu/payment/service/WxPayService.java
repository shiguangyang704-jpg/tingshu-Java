package com.atguigu.tingshu.payment.service;

import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WxPayService {

    /**
     * 创建微信支付二维码
     * @param paymentType
     * @param orderNo
     * @param userId
     * @return
     */
    Map<String, Object> createJsapi(String paymentType, String orderNo, Long userId);

    /**
     * 查询微信支付订单状态
     * @param orderNo
     * @return
     */
    Transaction queryPayStatus(String orderNo);

    /**
     * 微信支付回调
     * @param request
     * @return
     */
    Transaction wxNotify(HttpServletRequest request);
}
