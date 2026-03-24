package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {


    /**
     * 获取分类列表
     * @return
     */
    List<JSONObject> getCategoryList();


    /**
     * 获取属性列表
     * @param category1Id
     * @return
     */
    List<BaseAttribute> findAttribute(Long category1Id);

    /**
     * 获取分类视图
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryView(Long category3Id);

    /**
     * 获取分类列表
     * @param category1Id
     * @return
     */
    JSONObject getBaseCategoryList(Long category1Id);

    /**
     * 获取一级分类下的所有二级分类
     * @param category1Id
     * @return
     */
    List<BaseCategory3> findTopBaseCategory3(Long category1Id);

}
