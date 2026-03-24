package com.atguigu.tingshu.album.controller;

import com.atguigu.tingshu.album.service.BaseCategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * 
 *
 */
@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/admin/album/category")
@SuppressWarnings({"all"})
public class BaseCategoryController {
	
	@Autowired
	private BaseCategoryService baseCategoryService;

}

