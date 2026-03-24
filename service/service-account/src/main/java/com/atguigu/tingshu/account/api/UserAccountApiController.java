package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account/userAccount")
@SuppressWarnings({"all"})
public class UserAccountApiController {

    @Autowired
    private UserAccountService userAccountService;

    /**
     * 查看消费记录
     *
     * @param pageNo
     * @param pageSize
     * @return
     */
    @TsLogin
    @Operation(summary = "查看消费记录")
    @GetMapping("/findUserConsumePage/{pageNo}/{pageSize}")
    public Result findUserConsumePage(@PathVariable Long pageNo,
                                      @PathVariable Long pageSize) {
        //获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //  构建分页查询条件;
        Page<UserAccountDetail> userAccountDetailPage = new Page<>(pageNo, pageSize);
        //	调用服务层方法;
        IPage<UserAccountDetail> iPage = userAccountService.findUserConsumePage(userAccountDetailPage, userId);
        return Result.ok(iPage);
    }

    /**
     * 查看充值记录
     *
     * @param pageNo
     * @param pageSize
     * @return
     */
    @TsLogin
    @Operation(summary = "查看消费记录")
    @GetMapping("/findUserRechargePage/{pageNo}/{pageSize}")
    public Result findUserRechargePage(@PathVariable Long pageNo,
                                      @PathVariable Long pageSize) {
        //获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //  构建分页查询条件;
        Page<UserAccountDetail> userAccountDetailPage = new Page<>(pageNo, pageSize);
        //	调用服务层方法;
        IPage<UserAccountDetail> iPage = userAccountService.findUserRechargePage(userAccountDetailPage, userId);
        return Result.ok(iPage);
    }


    /**
     * 检查及扣减账户余额
     *
     * @param accountDeductVo
     * @return
     */
    @Operation(summary = "检查及扣减账户余额")
    @PostMapping("/checkAndDeduct")
    public Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo) {
        //	调用服务层方法;'
        Boolean flag = userAccountService.checkAndDeduct(accountDeductVo);
        //	返回值;
        return flag ? Result.ok() : Result.fail();
    }


    /**
     * 获取账户可用余额
     *
     * @return
     */
    @TsLogin
    @Operation(summary = "获取账户可用余额")
    @GetMapping("/getAvailableAmount")
    public Result getAvailableAmount() {
        //获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //	调用服务层方法;
        BigDecimal availableAmount = userAccountService.getAvailableAmount(userId);
        return Result.ok(availableAmount);
    }

}

