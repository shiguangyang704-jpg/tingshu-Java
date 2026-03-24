package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order/orderInfo")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;

	/**
	 * 根据状态查看我的订单
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	@Operation(summary = "根据状态查看我的订单")
	@GetMapping("/findUserPage/{pageNo}/{pageSize}")
	@TsLogin
	public Result findUserPage(@PathVariable Long pageNo,
							   @PathVariable Long pageSize,
							   HttpServletRequest request){
		//  获取状态值;
		String orderStatus = request.getParameter("orderStatus");
		//	获取用户Id
		Long userId = AuthContextHolder.getUserId();
		//	封装一个page对象;
		Page<OrderInfo> orderInfoPage = new Page<>(pageNo,pageSize);
		//	调用服务层方法
		IPage<OrderInfo> iPage = orderInfoService.findUserPage(orderInfoPage,userId,orderStatus);
		//	返回数据
		return Result.ok(iPage);
	}

	/**
	 * 根据orderNo查看订单数据
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "根据orderNo查看订单数据")
	@GetMapping("/getOrderInfo/{orderNo}")
	public Result getOrderInfo(@PathVariable String orderNo){
		//	调用服务层方法;
		OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderNo(orderNo);
		//	返回数据
		return Result.ok(orderInfo);
	}
	/**
	 * 提交订单
	 * @param orderInfoVo
	 * @return
	 */
	@TsLogin
	@Operation(summary = "提交订单")
	@PostMapping("/submitOrder")
	public Result submitOrder(@RequestBody OrderInfoVo orderInfoVo){
		//	获取用户Id
		Long userId = AuthContextHolder.getUserId();
		//	调用服务层方法
		String orderNo = orderInfoService.submitOrder(orderInfoVo,userId);
		//	将订单编号存储，传递给前端;
		HashMap<String, Object> map = new HashMap<>();
		//	前端需要的key
		map.put("orderNo",orderNo);
		//	返回数据
		return Result.ok(map);
	}

	/**
	 * 订单结算页
	 * @param tradeVo
	 * @return
	 */
	@TsLogin
	@Operation(summary = "订单结算页")
	@PostMapping("/trade")
	public Result trade(@RequestBody TradeVo tradeVo){
		//	获取用户Id
		Long userId = AuthContextHolder.getUserId();
		//	调用服务层方法;--将页面要显示的数据封装好给前端！
		OrderInfoVo orderInfoVo = orderInfoService.trade(tradeVo,userId);
		//	返回数据
		return Result.ok(orderInfoVo);
	}

}

