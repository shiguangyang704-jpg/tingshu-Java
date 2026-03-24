package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void updatePaymentStatus(Transaction result) {
        //  更新交易记录; payment_info; 根据订单编号更新;
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        //  transaction_id = out_trade_no;
        //  out_trade_no = order_no;
        wrapper.eq(PaymentInfo::getOrderNo, result.getOutTradeNo());
        PaymentInfo paymentInfo = this.getOne(wrapper);
        //  判断：
        if (null != paymentInfo && paymentInfo.getPaymentStatus().equals(SystemConstant.PAYMENT_STATUS_UNPAID)) {
            //  修改当前状态为已支付;
            paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_PAID);
            //  修改其他字典;
            paymentInfo.setCallbackTime(new Date());
//        paymentInfo.setContent(result.toString());
            paymentInfo.setCallbackContent(result.toString());
            paymentInfo.setOutTradeNo(result.getTransactionId());
            //  调用更新方法;
            this.updateById(paymentInfo);
            //  更新订单状态;
            String routingKey = paymentInfo.getPaymentType().equals(SystemConstant.PAYMENT_TYPE_ORDER) ? MqConst.ROUTING_ORDER_PAY_SUCCESS : MqConst.ROUTING_RECHARGE_PAY_SUCCESS;
            //  发送消息;
            rabbitService.sendMessage(MqConst.EXCHANGE_ORDER, routingKey, result.getOutTradeNo());
        }

    }

    @Override
    public void savePaymentInfo(Long userId, String paymentType, String orderNo, BigDecimal orderAmount) {
        //  创建对象：
        PaymentInfo paymentInfo = this.getOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
        //  判断
        if (null != paymentInfo) {
            return;
        }
        paymentInfo = new PaymentInfo();
        //  赋值;
        paymentInfo.setUserId(userId);
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        paymentInfo.setAmount(orderAmount);
        paymentInfo.setContent(paymentType.equals(SystemConstant.PAYMENT_TYPE_ORDER) ? "订单支付" : "充值");
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);
        //  保存数据;
        this.save(paymentInfo);
    }
}
