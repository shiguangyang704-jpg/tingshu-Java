package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account/rechargeInfo")
@SuppressWarnings({"all"})
public class RechargeInfoApiController {

	@Autowired
	private RechargeInfoService rechargeInfoService;

	/**
	 * 充值业务
	 * {"amount":10000,"payWay":"1101"}
	 * @return
	 */
	@TsLogin
	@Operation(summary = "充值业务")
	@PostMapping("/submitRecharge")
	public Result submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo) {
		//	给谁充值;
		Long userId = AuthContextHolder.getUserId();
		//	调用服务层方法;
		String orderNo = rechargeInfoService.submitRecharge(userId, rechargeInfoVo);
		//	创建map 集合
		Map<String, Object> map = new HashMap<>();
		map.put("orderNo", orderNo);
		//	返回值;
		return Result.ok(map);
	}

	/**
	 * 根据订单号获取充值信息
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "根据订单号获取充值信息")
	@GetMapping("/getRechargeInfo/{orderNo}")
	public Result<RechargeInfo> getRechargeInfo(@PathVariable("orderNo") String orderNo){
		//	调用服务层方法;
		RechargeInfo rechargeInfo = rechargeInfoService.getRechargeInfo(orderNo);
		//	返回数据
		return Result.ok(rechargeInfo);
	}

}

