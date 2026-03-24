package com.atguigu.tingshu.vo.user;

import lombok.Data;

import java.util.List;

@Data
public class UserPaidRecordVo {

    //  订单编号
    private String orderNo;
    //  用户Id
    private Long userId;
    //  购买类型
    private String itemType;
    //  购买项目Id{albumId,trackId,vipServiceConfigId}
    private List<Long> itemIdList;
}
