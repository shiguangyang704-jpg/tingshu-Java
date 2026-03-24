package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {


    /**
     * 用户注册
     * @param userId
     */
    void userAccountRegister(Long userId);

    /**
     * 获取用户可用余额
     * @param userId
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);

    /**
     * 检测并扣减
     * @param accountDeductVo
     * @return
     */
    Boolean checkAndDeduct(AccountDeductVo accountDeductVo);


    /**
     * 添加用户金额
     * @param userId
     * @param rechargeAmount
     */
    int addUserAmount(Long userId, BigDecimal rechargeAmount, String orderNo);

    /**
     * 查询用户充值记录
     * @param userAccountDetailPage
     * @param userId
     * @return
     */
    IPage<UserAccountDetail> findUserRechargePage(Page<UserAccountDetail> userAccountDetailPage, Long userId);

    /**
     * 查询用户消费记录
     * @param userAccountDetailPage
     * @param userId
     * @return
     */
    IPage<UserAccountDetail> findUserConsumePage(Page<UserAccountDetail> userAccountDetailPage, Long userId);
}
