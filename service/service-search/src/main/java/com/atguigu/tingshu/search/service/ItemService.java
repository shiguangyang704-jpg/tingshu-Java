package com.atguigu.tingshu.search.service;

import java.util.Map;

public interface ItemService {


    /**
     * 根据专辑id查询专辑信息
     * @param albumId
     * @return
     */
    Map<String, Object> getItemInfo(Long albumId);
}
