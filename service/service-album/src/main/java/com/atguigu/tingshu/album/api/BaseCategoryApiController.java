package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album/category")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {
	
	@Autowired
	private BaseCategoryService baseCategoryService;

	/**
	 * 获取全部一级分类
	 * @return
	 */
	@Operation(summary = "获取所有一级分类数据")
	@GetMapping("/findAllCategory1")
	public Result<List<BaseCategory1>> findAllCategory1(){
		//	调用服务层方法
		List<BaseCategory1> list = baseCategoryService.list();
		//	返回数据：
		return Result.ok(list);
	}


	/**
	 * 根据一级分类Id获取全部数据
	 * @param category1Id
	 * @return
	 */
	@Operation(summary = "根据一级分类Id获取全部数据")
	@GetMapping("/getBaseCategoryList/{category1Id}")
	public Result getBaseCategoryList(@PathVariable Long category1Id){
		//	获取一级分类数据集合
		JSONObject category1 = baseCategoryService.getBaseCategoryList(category1Id);
		//	返回数据;
		return Result.ok(category1);
	}

	/**
	 * 根据一级分类Id获取到三级分类数据
	 * @param category1Id
	 * @return
	 */
	@Operation(summary = "根据一级分类Id获取到三级分类数据")
	@GetMapping("/findTopBaseCategory3/{category1Id}")
	public Result findTopBaseCategory3(@PathVariable Long category1Id){
		//	调用服务层方法; 根据页面渲染数据来讲：需要一级，二级，三级分类数据集合：
		List<BaseCategory3> baseCategory3List = baseCategoryService.findTopBaseCategory3(category1Id);
		//	返回集合
		return Result.ok(baseCategory3List);
	}

	/**
	 * 根据三级分类Id 获取到分类数据
	 * @param category3Id
	 * @return
	 */
	@Operation(summary = "根据三级分类Id 获取到分类数据")
	@GetMapping("/getCategoryView/{category3Id}")
	public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id){
		//	调用服务层方法;
		BaseCategoryView baseCategoryView = baseCategoryService.getCategoryView(category3Id);
		//	返回
		return Result.ok(baseCategoryView);
	}

	/**
	 * 根据一级分类Id查询属性数据
	 * @param category1Id
	 * @return
	 */
	@Operation(summary = "根据一级分类Id查询属性数据")
	@GetMapping("/findAttribute/{category1Id}")
	public Result findAttribute(@PathVariable Long category1Id) {
		//	调用服务层方法
		List<BaseAttribute> baseAttributeList = baseCategoryService.findAttribute(category1Id);
		//	返回数据
		return Result.ok(baseAttributeList);
	}
	/**
	 * 获取分类数据集合
	 * @return
	 */
	@Operation(summary = "获取分类数据集合")
	@GetMapping("/getBaseCategoryList")
	public Result getCategoryList() {
		//	调用服务层方法;
		// T：必须包含范湖Id数据集合; 来源--mysql; 其他数据：es，redis，MongoDB; 需要远程调用其他接口！T:
		// T=JSONObject put()方法;底层调用的map集合; map.put(key,value);
		// Class=Map功效！
		/*
			class Param{
				private Long categoryId;
				private String categoryName;
				private List<T> categoryChild;
			}
			Param p = new Param();
			p.setCategoryId(1L);
			p.getCategoryId();
			-----------------------------
			HashMap map = new HashMap();
			map.put("categoryId",1L) = p.setCategoryId(1L);
			map.get("category1Id");
		 */
		List<JSONObject> list = baseCategoryService.getCategoryList();
		//	返回数据
		return Result.ok(list);
	}
}

