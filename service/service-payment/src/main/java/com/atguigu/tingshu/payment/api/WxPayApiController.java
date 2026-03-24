package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment/wxPay")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    /**
     * 微信支付回调
     *
     * @return
     */
    @Operation(summary = "微信异步回调")
    @PostMapping("/notify")
    public Map<String,Object> wxNotify(HttpServletRequest request) {
        log.info("微信支付回调开始...");
        //  创建一个map集合；
        Map<String, Object> result = new HashMap<>();
        //  调用微信支付回调方法;
        Transaction transaction = wxPayService.wxNotify(request);
        log.info("微信支付回调结束...");
        //  判断
        if (null != transaction && transaction.getTradeState().equals(Transaction.TradeStateEnum.SUCCESS)){
            //  调用更新交易记录与修改订单或充值状态;
            paymentInfoService.updatePaymentStatus(transaction);
            result.put("code", "SUCCESS");
            result.put("message", "成功");
            //  返回数据
            return result;
        }
        //  设置失败信息;
        result.put("code", "FAIL");
        result.put("message", "失败");
        //  返回结果;
        return result;
    }

    /**
     * 微信支付状态查询
     *
     * @param orderNo
     * @return
     */
    @TsLogin
    @Operation(summary = "微信支付状态查询")
    @GetMapping("/queryPayStatus/{orderNo}")
    public Result queryPayStatus(@PathVariable String orderNo) {
        log.info("微信支付状态查询开始...");
        //  调用微信支付状态查询方法;
        Transaction result = wxPayService.queryPayStatus(orderNo);
        log.info("微信支付状态查询结束...");
        //  判断结果：
        if (null != result && result.getTradeState().equals(Transaction.TradeStateEnum.SUCCESS)) {
            //  支付成功; 1.修改交易记录状态 payment_info; order_info; recharge_info
            paymentInfoService.updatePaymentStatus(result);
            //  返回结果;
            return Result.ok(true);
        }
        return Result.ok(false);
    }


    /**
     * 微信支付
     *
     * @param paymentType 1301:使用微信支付订单；1302:表示使用微信充值
     * @param orderNo
     * @return
     */
    @TsLogin
    @Operation(summary = "微信支付")
    @PostMapping("/createJsapi/{paymentType}/{orderNo}")
    public Result createJsapi(@PathVariable String paymentType,
                              @PathVariable String orderNo) {
        log.info("微信支付开始...");
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //  调用宏服务层方法;
        Map<String, Object> map = wxPayService.createJsapi(paymentType, orderNo, userId);
        log.info("微信支付结束...");
        return Result.ok(map);
    }
}
