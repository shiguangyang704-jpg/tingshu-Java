package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

    @Override
    public IPage<UserAccountDetail> findUserRechargePage(Page<UserAccountDetail> userAccountDetailPage, Long userId) {
        //  构建查询条件：
        LambdaQueryWrapper<UserAccountDetail> wrapper = new LambdaQueryWrapper<UserAccountDetail>()
                .eq(UserAccountDetail::getUserId, userId)
                .eq(UserAccountDetail::getTradeType, SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT)
                .orderByDesc(UserAccountDetail::getId);
        //  返回数据
        return userAccountDetailMapper.selectPage(userAccountDetailPage,wrapper);
    }

    @Override
    public IPage<UserAccountDetail> findUserConsumePage(Page<UserAccountDetail> userAccountDetailPage, Long userId) {
        //  构建查询条件：
        LambdaQueryWrapper<UserAccountDetail> wrapper = new LambdaQueryWrapper<UserAccountDetail>()
                .eq(UserAccountDetail::getUserId, userId)
                .eq(UserAccountDetail::getTradeType, SystemConstant.ACCOUNT_TRADE_TYPE_MINUS)
                .orderByDesc(UserAccountDetail::getId);
        //  返回数据
        return userAccountDetailMapper.selectPage(userAccountDetailPage,wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addUserAmount(Long userId, BigDecimal rechargeAmount, String orderNo) {
        //  记录当前资金变化
        this.addUserAccountDetail(userId, "用户充值", SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, rechargeAmount, orderNo);
        //  充值;
        int result = userAccountMapper.addUserAmount(userId, rechargeAmount);
        //  返回数据
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean checkAndDeduct(AccountDeductVo accountDeductVo) {
        //	核心工作：扣减余额;
        //	update user_account
        //set total_amount     = total_amount - ?,
        //    available_amount = available_amount - ?,
        //    total_pay_amount=total_pay_amount + ?
        //where user_id = ? and available_amount >= ? and is_deleted = 0
        //	返回受影响的行数
        try {
            //  记录账户资金变化;
            this.addUserAccountDetail(accountDeductVo.getUserId(), accountDeductVo.getContent(), SystemConstant.ACCOUNT_TRADE_TYPE_MINUS,
                    accountDeductVo.getAmount(), accountDeductVo.getOrderNo());
            return userAccountMapper.checkAndDeduct(accountDeductVo.getAmount(), accountDeductVo.getUserId()) > 0;
        } catch (Exception e) {
            log.error("用户账户扣减失败");
        }
        return false;
    }

    public void addUserAccountDetail(Long userId, String title, String typeMinus, BigDecimal amount, String orderNo) {
        //  创建对象
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        //  赋值;
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTitle(title);
        userAccountDetail.setTradeType(typeMinus);
        userAccountDetail.setAmount(amount);
        userAccountDetail.setOrderNo(orderNo);
        //  保存数据；
        userAccountDetailMapper.insert(userAccountDetail);
    }

    @Override
    public BigDecimal getAvailableAmount(Long userId) {
        //	select available_amount from user_account where user_id = ?
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId).select(UserAccount::getAvailableAmount)).getAvailableAmount();
    }

    @Override
    public void userAccountRegister(Long userId) {
        //	创建对象
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        userAccount.setTotalAmount(new BigDecimal("100"));
        userAccount.setAvailableAmount(new BigDecimal("100"));
        userAccountMapper.insert(userAccount);
    }
}
