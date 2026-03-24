package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.account.client.RechargeInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.OrderInfoFeignClient;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.payment.util.PayUtil;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private RSAAutoCertificateConfig config;

    @Autowired
    private WxPayV3Config wxPayV3Config;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private RechargeInfoFeignClient rechargeInfoFeignClient;


    @Override
    public Transaction wxNotify(HttpServletRequest request) {
        //  从请求头中获取数据;
        String wechatPaySerial = request.getHeader("Wechatpay-Serial");
        String wechatpayNonce = request.getHeader("Wechatpay-Nonce");
        String wechatTimestamp = request.getHeader("Wechatpay-Timestamp");
        String wechatSignature = request.getHeader("Wechatpay-Signature");
        //  请求头：
        String requestBody = PayUtil.readData(request);
        // 构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(wechatpayNonce)
                .signature(wechatSignature)
                .timestamp(wechatTimestamp)
                .body(requestBody)
                .build();
        // 初始化 NotificationParser
        NotificationParser parser = new NotificationParser(config);
        try {
            // 以支付通知回调为例，验签、解密并转换成 Transaction
            Transaction transaction = parser.parse(requestParam, Transaction.class);
            //  返回数据：
            return transaction;
        } catch (Exception e) {
            // 签名验证失败，返回 401 UNAUTHORIZED 状态码
            log.error("sign verification failed", e);
        }
        //  默认返回
        return null;
    }

    @Override
    public Transaction queryPayStatus(String orderNo) {
        //  商户订单号查询订单
        QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
        queryRequest.setMchid(wxPayV3Config.getMerchantId());
        queryRequest.setOutTradeNo(orderNo);
        //  transaction_id = out_trade_no;
        //  out_trade_no = order_no;
        try {
            //  生成支付的功能;
            JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(config).build();
            Transaction result = service.queryOrderByOutTradeNo(queryRequest);
            System.out.println(result.getTradeState());
            //  返回数据
            return result;
        } catch (ServiceException e) {
            // API返回失败, 例如ORDER_NOT_EXISTS
            System.out.printf("code=[%s], message=[%s]\n", e.getErrorCode(), e.getErrorMessage());
            System.out.printf("reponse body=[%s]\n", e.getResponseBody());
        }
        //  默认返回：null
        return null;
    }

    @Override
    public Map<String, Object> createJsapi(String paymentType, String orderNo, Long userId) {
        //	创建map集合
        Map<String, Object> result = new HashMap<>();
        //  定义支付金额;
        BigDecimal orderAmount = new BigDecimal("0.00");
        //	判断支付类型：
        if (paymentType.equals(SystemConstant.PAYMENT_TYPE_ORDER)) {
            //	订单;
            //	远程调用：
            Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderNo);
            //	判断
            Assert.notNull(orderInfoResult, "订单不存在！");

            OrderInfo orderInfo = orderInfoResult.getData();
            if (null != orderInfo && orderInfo.getOrderStatus().equals(SystemConstant.ORDER_STATUS_UNPAID)) {
                //	获取当前订单金额：
                orderAmount = orderInfo.getOrderAmount();
            }
        } else {
            //	充值
            Result<RechargeInfo> rechargeInfoResult = rechargeInfoFeignClient.getRechargeInfo(orderNo);
            Assert.notNull(rechargeInfoResult, "充值信息不存在！");
            RechargeInfo rechargeInfo = rechargeInfoResult.getData();
            Assert.notNull(rechargeInfo, "充值信息不存在！");
            orderAmount = rechargeInfo.getRechargeAmount();
        }
        //  在支付的时候，需要保存支付交易记录; 目的：跟微信对账;payment_info;
        paymentInfoService.savePaymentInfo(userId, paymentType, orderNo, orderAmount);
        //  生成支付的功能;
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(config).build();
        // 跟之前下单示例一样，填充预下单参数
        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal(1);
        request.setAmount(amount);
        request.setAppid(wxPayV3Config.getAppid());
        request.setMchid(wxPayV3Config.getMerchantId());
        request.setDescription("测试商品标题");
        request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
        //	微信内部out_trade_no = order_no;
        request.setOutTradeNo(orderNo);
        //	创建一个Payer对象
        Payer payer = new Payer();
        //	通过用户Id调用用户信息;
        Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
        Assert.notNull(userInfoVoResult, "用户信息不存在！");
        UserInfoVo userInfoVo = userInfoVoResult.getData();
        Assert.notNull(userInfoVo, "用户信息不存在！");
        payer.setOpenid(userInfoVo.getWxOpenId());
        request.setPayer(payer);
        // response包含了调起支付所需的所有参数，可直接用于前端调起支付
        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
        //	赋值数据并返回
        result.put("timeStamp", response.getTimeStamp()); // 时间戳
        result.put("nonceStr", response.getNonceStr());   // 随机字符串
        result.put("package", response.getPackageVal());  // 订单详情扩展字符串
        result.put("signType", response.getSignType());   // 签名方式
        result.put("paySign", response.getPaySign());     // 签名
        return result;
    }
}
