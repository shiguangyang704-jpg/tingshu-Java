package com.atguigu.tingshu.user.strategy;

import com.atguigu.tingshu.vo.user.UserPaidRecordVo;

/**
 * @author atguigu-mqx
 * @ClassName ItemTypeStrategy
 * @description: TODO
 * @date 2026年03月09日
 * @version: 1.0
 */
public interface ItemTypeStrategy {

    //  定义接口策略;

    /**
     *  1001-专辑；
     * 	1002-声音;
     * 	1003-vip;
     * 	spring 底层创建了一个map集合来维护对象；key=1001 value=AlbumStrategy key=1002 value=TrackStrategy key=1003 value=VipStrategy
     * @param userPaidRecordVo
     * @return
     */
    Boolean savePaidRecord(UserPaidRecordVo userPaidRecordVo);
}
