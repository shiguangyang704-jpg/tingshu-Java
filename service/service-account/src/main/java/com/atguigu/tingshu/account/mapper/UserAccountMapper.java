package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 检测账户余额是否充足
     * @param amount
     * @param userId
     * @return
     */
    @Update("update user_account\n" +
            "set total_amount     = total_amount - #{amount},\n" +
            "    available_amount = available_amount - #{amount},\n" +
            "    total_pay_amount=total_pay_amount + #{amount}\n" +
            "where user_id = #{userId} and available_amount >= #{amount} and is_deleted = 0")
    int checkAndDeduct(@Param("amount") BigDecimal amount, @Param("userId") Long userId);

    /**
     * 充值
     * @param userId
     * @param rechargeAmount
     * @return
     */
    @Update("update user_account set total_amount = total_amount + #{amount},available_amount = available_amount + #{amount} where user_id = #{userId} and is_deleted = 0 ")
    int addUserAmount(@Param("userId") Long userId, @Param("amount") BigDecimal rechargeAmount);

}
