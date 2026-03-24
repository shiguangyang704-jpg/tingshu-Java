package com.atguigu.tingshu.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.mapper.OrderDetailMapper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.user.client.VipServiceConfigFeignClient;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private VipServiceConfigFeignClient vipServiceConfigFeignClient;

    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderDerateMapper orderDerateMapper;

    @Autowired
    private UserAccountFeignClient userAccountFeignClient;


    // ... existing code ...

    @Override
    public void orderPaySuccess(String orderNo) {
        //  修改状态;
        OrderInfo orderInfo = this.getOrderInfoByOrderNo(orderNo);
        //  判断
        if (null != orderInfo && orderInfo.getOrderStatus().equals(SystemConstant.ORDER_STATUS_UNPAID)) {
            //  修改为已支付;
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
            this.updateById(orderInfo);
            //  保存用户交易记录：
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            //  赋值：
            userPaidRecordVo.setOrderNo(orderNo);
            userPaidRecordVo.setUserId(orderInfo.getUserId());
            userPaidRecordVo.setItemType(orderInfo.getItemType());
            //  订单明细表总的.itemId;
            List<Long> itemIdList = orderInfo.getOrderDetailList().stream().map(OrderDetail::getItemId).collect(Collectors.toList());
            userPaidRecordVo.setItemIdList(itemIdList);
            Result result = userInfoFeignClient.savePaidRecord(userPaidRecordVo);
            //  判断
            if (200 != result.getCode()) {
                log.error("保存用户交易记录失败！");
            }
        }
    }

    @Override
    public IPage<OrderInfo> findUserPage(Page<OrderInfo> orderInfoPage, Long userId, String orderStatus) {
        //  根据状态查看我的订单.
        IPage<OrderInfo> iPage = orderInfoMapper.selectUserPage(orderInfoPage, userId, orderStatus);
        //  处理订单列表中的支付状态。
        iPage.getRecords().forEach(orderInfo -> {
            //  设置支付方式;
            orderInfo.setPayWayName(orderInfo.getPayWay().equals(SystemConstant.ORDER_PAY_ACCOUNT) ? "余额支付" : "在线支付");
            //  设置支付状态; 0901-未支付 0902-已支付；0903-已取消
            orderInfo.setOrderStatusName(orderInfo.getOrderStatus().equals(SystemConstant.ORDER_STATUS_UNPAID) ? "未支付" :
                    orderInfo.getOrderStatus().equals(SystemConstant.ORDER_STATUS_PAID) ? "已支付" : "已取消");
        });
        //  返回数据;
        return iPage;
    }

    @Override
    public void cancelOrder(String orderNo) {
        //  取消订单；订单的状态：0901-未支付 0902-已支付 0903-已取消
        OrderInfo orderInfo = this.getOrderInfoByOrderNo(orderNo);
        //  利用业务字段保证幂等性;
        if (null != orderInfo && orderInfo.getOrderStatus().equals(SystemConstant.ORDER_STATUS_UNPAID)) {
            //  取消订单：
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_CANCEL);
            //  更新数据库：
            this.updateById(orderInfo);
        }
    }

    @Override
    public OrderInfo getOrderInfoByOrderNo(String orderNo) {
        //  查询订单对象
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        //  付款方式： 支付方式：1101-微信 1102-支付宝 1103-账户余额
        String payWayName = orderInfo.getPayWay().equals(SystemConstant.ORDER_PAY_ACCOUNT) ? "余额支付" : "在线支付";
        orderInfo.setPayWayName(payWayName);
        //  判断
        if (null != orderInfo) {
            orderInfo.setOrderDetailList(orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderInfo.getId())));
        }
        return orderInfo;
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public String submitOrder(OrderInfoVo orderInfoVo, Long userId) {

        //  1.  防止用户重复提交订单！
        //  缓存获取：流水号：
        //  页面传递的流水号;
        String tradeNo = orderInfoVo.getTradeNo();
        //  缓存的key 是这样的格式;
        String tradeNoKey = "tradeNo:" + tradeNo;
        //  从缓存中获取数据;
        //  String tradeNoRedis = (String) this.redisTemplate.opsForValue().get(tradeNoKey);
        //  正常情况
        //        if (!tradeNoRedis.equals(tradeNo)) {
        //            throw new RuntimeException("用户重复提交订单！");
        //        }
        //  多个平台：防止误删锁使用lua 脚本;
        String redistScript = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";
        //  当key与键值匹配的时候才会删除！
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(redistScript);
        redisScript.setResultType(Long.class);
        Long resultCount = (Long) redisTemplate.execute(redisScript, Arrays.asList(tradeNoKey), tradeNo);
        //  判断返回值;
        if (resultCount == 0) {
            throw new RuntimeException("用户重复提交订单！");
        }



        //  2.  校验sign{orderInfoVo 演变}
        //  orderInfoVo 对象转换为map集合
        Map map = JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class);
        //  校验过程中页面传递的什么值无所谓，再次都给置为空
        map.put("payWay", "");
        SignHelper.checkSign(map);
        //  声明要给订单编号;
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");



        //  判断支付类型：
        if (!orderInfoVo.getPayWay().equals(SystemConstant.ORDER_PAY_ACCOUNT)) {
            //  在线支付： 1.保存订单信息
            this.saveOrder(orderInfoVo, userId, orderNo);
            //  2.发送延迟消息实现取消订单
            rabbitService.sendDealyMessage(MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER, orderNo, MqConst.CANCEL_ORDER_DELAY_TIME);
        } else {
            //  余额支付; 1.保存订单信息 2.扣减余额 3.判断扣减结果：true: 更新订单专题，保存用户购买记录{user}
            this.saveOrder(orderInfoVo, userId, orderNo);
            //  扣减余额：远程调用:service-acount; user_id, amount;
            //  需要将数据封装到一个实体类中;
            //  创建对象;
            AccountDeductVo accountDeductVo = new AccountDeductVo();
            //  赋值：
            accountDeductVo.setUserId(userId);
            accountDeductVo.setAmount(orderInfoVo.getOrderAmount());
            accountDeductVo.setContent(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
            accountDeductVo.setOrderNo(orderNo);
            Result result = userAccountFeignClient.checkAndDeduct(accountDeductVo);
            //  判断返回值结果;
            if (200 != result.getCode()) {
                throw new GuiguException(result.getCode(), result.getMessage());
            }
            //  更新订单状态与记录购买记录!
            //  将订单状态修改为支付;
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
            //  更新条件;
            LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderInfo::getOrderNo, orderNo);
            this.update(orderInfo, wrapper);
            //  需要远程调用：
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            userPaidRecordVo.setOrderNo(orderNo);
            userPaidRecordVo.setUserId(userId);
            userPaidRecordVo.setItemType(orderInfoVo.getItemType());
            //  这些数据从何而来? 订单明细中的itemId = 购买项目Id{albumId,trackId,vipServiceConfigId}
            List<Long> itemIdList = orderInfoVo.getOrderDetailVoList().stream().map(OrderDetailVo::getItemId).collect(Collectors.toList());
            userPaidRecordVo.setItemIdList(itemIdList);
            //  远程调用：
            Result userResult = userInfoFeignClient.savePaidRecord(userPaidRecordVo);
            //  判断
            if (200 != userResult.getCode()) {
                throw new GuiguException(211, "新增购买记录异常");
            }
        }




        //  返回订单编号;
        return orderNo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOrder(OrderInfoVo orderInfoVo, Long userId, String orderNo) {
        //  order_derate
        //  order_detail
        //  order_info
        OrderInfo orderInfo = new OrderInfo();
        //  属性拷贝：
        BeanUtils.copyProperties(orderInfoVo, orderInfo);
        //  赋值其他属性字段：
        orderInfo.setUserId(userId);
        orderInfo.setOrderTitle(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
        orderInfo.setOrderNo(orderNo);
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
        orderInfoMapper.insert(orderInfo);
        // 订单明细表;
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (!CollectionUtils.isEmpty(orderDetailVoList)) {
            //  循环遍历保存数据;
            for (OrderDetailVo orderDetailVo : orderDetailVoList) {
                //  创建订单明细对象
                OrderDetail orderDetail = new OrderDetail();
                //  赋值：
                BeanUtils.copyProperties(orderDetailVo, orderDetail);
                orderDetail.setOrderId(orderInfo.getId());
                //  保存数据
                orderDetailMapper.insert(orderDetail);
            }
        }

        //  订单减免表;
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        //  判断：
        if (!CollectionUtils.isEmpty(orderDerateVoList)) {
            for (OrderDerateVo orderDerateVo : orderDerateVoList) {
                OrderDerate orderDerate = new OrderDerate();
                BeanUtils.copyProperties(orderDerateVo, orderDerate);
                orderDerate.setOrderId(orderInfo.getId());
                //  保存数据;
                orderDerateMapper.insert(orderDerate);
            }
        }
    }

    /**
     * 处理交易订单，生成订单信息
     *
     * @param tradeVo 交易参数对象，包含交易类型等信息
     * @param userId  用户 ID，标识当前操作用户
     * @return OrderInfoVo 订单信息视图对象，包含交易流水号、金额、商品详情、优惠信息及签名等
     */
    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        // 初始化订单信息对象
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        BigDecimal originalAmount = new BigDecimal("0.00");
        BigDecimal derateAmount = new BigDecimal("0.00");
        BigDecimal orderAmount = new BigDecimal("0.00");
        //  订单明细集合
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        //  订单减免集合
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        //  计算金额：
        if (tradeVo.getItemType().equals(SystemConstant.ORDER_ITEM_TYPE_ALBUM)) {
            //  购买专辑;
            Result<AlbumInfo> infoResult = albumInfoFeignClient.getAlbumInfo(tradeVo.getItemId());
            Assert.notNull(infoResult, "专辑结果集信息不存在！");
            AlbumInfo albumInfo = infoResult.getData();
            Assert.notNull(albumInfo, "专辑信息不存在！");
            //  专辑原价
            originalAmount = albumInfo.getPrice();
            //  身份验证;
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
            Assert.notNull(userInfoVoResult, "用户信息结果集不存在！");
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            Assert.notNull(userInfoVo, "用户信息不存在！");
            // 判断用户是否为VIP会员 要考虑是否有折扣！
            if (userInfoVo.getIsVip() == 1 && userInfoVo.getVipExpireTime().after(new Date())) {
                //  判断是否有折扣;
                if (albumInfo.getVipDiscount().compareTo(new BigDecimal("-1")) != 0) {
                    //  计算折扣价格; 1000 8 ; 800; 200; 1000*(10-8)/10;
                    derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getVipDiscount())).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
                //  计算折扣;
            } else {
                if (albumInfo.getDiscount().compareTo(new BigDecimal("-1")) != 0) {
                    //  计算折扣价格; 1000 8 ; 800; 200; 1000*(10-8)/10;
                    derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getDiscount())).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
            }
            //  实际价格：
            orderAmount = originalAmount.subtract(derateAmount);
            //  订单结算页的明细集合;
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            //  专辑Id
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVo.setItemPrice(orderAmount);
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVoList.add(orderDetailVo);
            //  如果有减免的话；赋值减免明细;
            if (derateAmount.compareTo(new BigDecimal("0.00")) != 0) {
                //  创建对象
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                //  赋值：
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setRemarks("专辑折扣");
                orderDerateVoList.add(orderDerateVo);
            }

        } else if (tradeVo.getItemType().equals(SystemConstant.ORDER_ITEM_TYPE_TRACK)) {



            //  购买声音;
            //  {"itemType":"1002","itemId":49166,"trackCount":10} trackCount=0;
            if (tradeVo.getTrackCount() < 0) {
                //  抛出异常;
                throw new RuntimeException("购买集数不能小于0！");
            }
            //  计算订单结算页的金额; --声音没有折扣;
            //  单价=专辑.price;album_info 表中; itemId=trackId;
            Result<TrackInfo> trackInfoResult = trackInfoFeignClient.getTrackInfo(tradeVo.getItemId());
            Assert.notNull(trackInfoResult, "声音结果集信息不存在！");
            TrackInfo trackInfo = trackInfoResult.getData();
            Assert.notNull(trackInfo, "声音信息不存在！");
            Result<AlbumInfo> infoResult = albumInfoFeignClient.getAlbumInfo(trackInfo.getAlbumId());
            Assert.notNull(infoResult, "专辑结果集信息不存在！");
            AlbumInfo albumInfo = infoResult.getData();
            Assert.notNull(albumInfo, "专辑信息不存在！");
            //  原始金额;
            originalAmount = tradeVo.getTrackCount() == 0 ? albumInfo.getPrice() :
                    albumInfo.getPrice().multiply(new BigDecimal(tradeVo.getTrackCount()));
            //  不支持折扣，也就是实际金额;
            orderAmount = originalAmount;
            //  根据购买的集数设置订单明细数据;
            Long trackId = tradeVo.getItemId();
            //  购买的集数; 10;针对于trackId来讲的后count 集;
            Integer trackCount = tradeVo.getTrackCount();
            //  远程调用
            Result<List<TrackInfo>> trackInfoListResult = trackInfoFeignClient.findPaidTrackInfoList(trackId, trackCount);
            Assert.notNull(trackInfoListResult, "声音结果集信息不存在！");
            List<TrackInfo> trackInfoList = trackInfoListResult.getData();
            Assert.notNull(trackInfoList, "声音信息不存在！");
            //  stream();
            orderDetailVoList = trackInfoList.stream().map(info -> {
                //  订单结算页的明细集合;
                OrderDetailVo orderDetailVo = new OrderDetailVo();
                orderDetailVo.setItemPrice(albumInfo.getPrice());
                orderDetailVo.setItemName(info.getTrackTitle());
                orderDetailVo.setItemUrl(info.getCoverUrl());
                orderDetailVo.setItemId(info.getId());
                //  返回数据
                return orderDetailVo;
            }).collect(Collectors.toList());





        } else {
            //  vip; {"itemType":"1003","itemId":1}
            Long vipServiceConfigId = tradeVo.getItemId();
            //  远程调用获取数据
            Result<VipServiceConfig> vipServiceConfigResult = vipServiceConfigFeignClient.getVipServiceConfig(vipServiceConfigId);
            Assert.notNull(vipServiceConfigResult, "VIP服务配置结果集不存在！");
            VipServiceConfig vipServiceConfig = vipServiceConfigResult.getData();
            Assert.notNull(vipServiceConfig, "VIP服务配置不存在！");
            //  赋值金额;
            originalAmount = vipServiceConfig.getPrice();
            //  实际价格;
            orderAmount = vipServiceConfig.getDiscountPrice();
            //  减免金额:
            derateAmount = originalAmount.subtract(orderAmount);
            //  订单结算页的明细集合;
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(vipServiceConfig.getName());
            orderDetailVo.setItemPrice(orderAmount);
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVoList.add(orderDetailVo);
            //  如果有减免的话；赋值减免明细;
            if (derateAmount.compareTo(new BigDecimal("0.00")) != 0) {
                //  创建对象
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                //  赋值：
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
                orderDerateVo.setRemarks("VIP折扣");
                orderDerateVoList.add(orderDerateVo);
            }
        }
        // 生成交易流水号
        String tradeNo = UUID.randomUUID().toString().replaceAll("-", "");
        //  定义缓存的key;
        String tradeNoKey = "tradeNo:" + tradeNo;
        //  防止用户重复提交订单;将tradeNo存储到缓存中一份;
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo, 3, TimeUnit.MINUTES);
        orderInfoVo.setTradeNo(tradeNo);
        //  支付方式；1101-微信 1102-支付宝 1103-账户余额
        orderInfoVo.setPayWay("");
        orderInfoVo.setItemType(tradeVo.getItemType());
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);

        // 设置时间戳并生成签名
        orderInfoVo.setTimestamp(SignHelper.getTimestamp());
        //  将订单对象中的数据先排序; 将value以|拼接；在加一个固定的盐值;最后对这个字符串进行md5加密！
        String sign = SignHelper.getSign(JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class));
        orderInfoVo.setSign(sign);
        return orderInfoVo;
    }

    // ... existing code ...

}
