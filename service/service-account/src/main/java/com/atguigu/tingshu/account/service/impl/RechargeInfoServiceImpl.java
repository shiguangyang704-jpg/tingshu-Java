package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;

    @Autowired
    private UserAccountService userAccountService;

    @Override
    public void rechargePaySuccess(String orderNo) {
        //  查询对象
        RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(new LambdaQueryWrapper<RechargeInfo>().eq(RechargeInfo::getOrderNo, orderNo));
        //  判断
        if (null != rechargeInfo){
            //  修改数据;
            rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_PAID);
            rechargeInfoMapper.updateById(rechargeInfo);
            //  添加用户账户信息;
            userAccountService.addUserAmount(rechargeInfo.getUserId(),rechargeInfo.getRechargeAmount(),orderNo);
        }
    }

    @Override
    public String submitRecharge(Long userId, RechargeInfoVo rechargeInfoVo) {
        //	给谁充多少钱!
        RechargeInfo rechargeInfo = new RechargeInfo();
        rechargeInfo.setUserId(userId);
        //	赋值订单编号：
        String ourderNo = UUID.randomUUID().toString().replaceAll("-", "");
        rechargeInfo.setOrderNo(ourderNo);
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
        rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());
        rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
        int result = rechargeInfoMapper.insert(rechargeInfo);
        //  返回数据
        return ourderNo;
    }

    @Override
    public RechargeInfo getRechargeInfo(String orderNo) {
        return rechargeInfoMapper.selectOne(new LambdaQueryWrapper<RechargeInfo>().eq(RechargeInfo::getOrderNo, orderNo));
    }
}
