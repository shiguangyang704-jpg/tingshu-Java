package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface RechargeInfoService extends IService<RechargeInfo> {

    /**
     * 根据订单号查询充值记录
     * @param orderNo
     * @return
     */
    RechargeInfo getRechargeInfo(String orderNo);

    /**
     * 提交充值
     * @param userId
     * @param rechargeInfoVo
     * @return
     */
    String submitRecharge(Long userId, RechargeInfoVo rechargeInfoVo);

    /**
     * 充值成功
     * @param orderNo
     */
    void rechargePaySuccess(String orderNo);
}
