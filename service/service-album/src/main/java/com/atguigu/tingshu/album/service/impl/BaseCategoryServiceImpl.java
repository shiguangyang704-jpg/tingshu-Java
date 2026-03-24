package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.cache.TsCache;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

	@Autowired
	private BaseCategory1Mapper baseCategory1Mapper;

	@Autowired
	private BaseCategory2Mapper baseCategory2Mapper;

	@Autowired
	private BaseCategory3Mapper baseCategory3Mapper;


	@Autowired
	private BaseCategoryViewMapper baseCategoryViewMapper;

	@Autowired
	private BaseAttributeMapper baseAttributeMapper;


	@Override
	public List<BaseCategory3> findTopBaseCategory3(Long category1Id) {
		//	实现方式有两种：一种直接写sql语句; 第二个中调用mapper;
		//	101,102,103;
		//  select id from base_category2 where category1_id = 1;
		List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(new LambdaQueryWrapper<BaseCategory2>().eq(BaseCategory2::getCategory1Id, category1Id)
				.select(BaseCategory2::getId));
		//	获取上述结合中的二级分类Id;
		List<Long> category2IdList = baseCategory2List.stream().map(BaseCategory2::getId).collect(Collectors.toList());
		//	select * from base_category3 where category2_id in (101,102,103) and is_top = 1 limit 7;
		//	创建查询条件对象
		LambdaQueryWrapper<BaseCategory3> wrapper = new LambdaQueryWrapper<>();
		wrapper.in(BaseCategory3::getCategory2Id,category2IdList)
				.eq(BaseCategory3::getIsTop,1)
				.last(" limit 7 ");
		//	返回三级分类数据；
		return baseCategory3Mapper.selectList(wrapper);
	}

	@Override
	public JSONObject getBaseCategoryList(Long category1Id) {
		//	创建一个JSONObject对象
		JSONObject category1 = new JSONObject();
		//	已知条件：只有一级分类Id;
		//	select * from base_category_view where category1Id = ?;
		List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(new LambdaQueryWrapper<BaseCategoryView>().eq(BaseCategoryView::getCategory1Id, category1Id));
		//	赋值:
		category1.put("categoryId",category1Id);
		category1.put("categoryName",baseCategoryViewList.get(0).getCategory1Name());
		//	存储的二级分类集合列表;
		Map<Long, List<BaseCategoryView>> listMap = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
		Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator = listMap.entrySet().iterator();
		//	创建一个集合列表;
		ArrayList<JSONObject> categoryChild2List = new ArrayList<>();
		while (iterator.hasNext()){
			Map.Entry<Long, List<BaseCategoryView>> entry = iterator.next();
			Long category2Id = entry.getKey();
			List<BaseCategoryView> baseCategoryViewList1 = entry.getValue();
			//	创建一个二级分类对象;
			JSONObject category2 = new JSONObject();
			category2.put("categoryId",category2Id);
			category2.put("categoryName",baseCategoryViewList1.get(0).getCategory2Name());
			//	获取三级分类数据：
			List<JSONObject> categoryChild3List = baseCategoryViewList1.stream().map(baseCategoryView -> {
				JSONObject category3 = new JSONObject();
				category3.put("categoryId", baseCategoryView.getCategory3Id());
				category3.put("categoryName", baseCategoryView.getCategory3Name());
				return category3;
			}).collect(Collectors.toList());
			//	添加三级分类数据到二级分类中
			category2.put("categoryChild",categoryChild3List);
			categoryChild2List.add(category2);
		}
		//	将二级分类数据添加到一级分类中
		category1.put("categoryChild",categoryChild2List);
		return category1;
	}

	@Override
	@TsCache(prefix = "album:category:")
	public BaseCategoryView getCategoryView(Long category3Id) {
		return baseCategoryViewMapper.selectById(category3Id);
	}

	@Override
	public List<BaseAttribute> findAttribute(Long category1Id) {
		//	调用mapper层方法;
		return baseAttributeMapper.selectAttribute(category1Id);
	}

	@Override
	public List<JSONObject> getCategoryList() {
		//	创建一个集合
		List<JSONObject> baseCategory1List = new ArrayList<>();
		//	组装数据：数据从视图中获取！
		List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
		//	分组：key 为 一级分类id value List<BaseCategoryView>
		Map<Long, List<BaseCategoryView>> listMap = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
		//	循环遍历;
		Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator = listMap.entrySet().iterator();
		//	循环遍历所有的一级分类数据;
		while (iterator.hasNext()){
			//	创建一个对象：来保存一级分类数据
			JSONObject category1 = new JSONObject();
			//	判断是否有下一个数据; 如何有这个数据则获取到这个数据.
			Map.Entry<Long, List<BaseCategoryView>> entry = iterator.next();
			//	根据key来获取category1Id；
			Long category1Id = entry.getKey();
			List<BaseCategoryView> categoryViewList = entry.getValue();
			category1.put("categoryId",category1Id);
			category1.put("categoryName",categoryViewList.get(0).getCategory1Name());
			//	实际还有一个一级分类Id下对应的二级分类集合数据：
			Map<Long, List<BaseCategoryView>> listMap1 = categoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
			//	循环遍历获取数据;
			Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator1 = listMap1.entrySet().iterator();
			//	声明一个集合来存储二级分类对象;
			ArrayList<JSONObject> category2ChildList = new ArrayList<>();
			//	1=音乐下应该有101，102,103 三组数据;
			while (iterator1.hasNext()){
				//	创建一个对象：来保存二级分类数据
				JSONObject category2 = new JSONObject();
				Map.Entry<Long, List<BaseCategoryView>> entry1 = iterator1.next();
				//	获取到的是二级分类Id;
				Long category2Id = entry1.getKey();
				//	获取的是二级分类集合数据
				List<BaseCategoryView> categoryViewList1 = entry1.getValue();
				category2.put("categoryId",category2Id);
				category2.put("categoryName",categoryViewList1.get(0).getCategory2Name());
				//	循环遍历获取数据;
				List<JSONObject> categoryChild3List = categoryViewList1.stream().map(baseCategoryView -> {
					//	创建一个对象
					JSONObject category3 = new JSONObject();
					category3.put("categoryId", baseCategoryView.getCategory3Id());
					category3.put("categoryName", baseCategoryView.getCategory3Name());
					return category3;
				}).collect(Collectors.toList());
				//	存储三级分类数据;
				category2.put("categoryChild",categoryChild3List);
				//	存储二级分类 数据;
				category2ChildList.add(category2);
			}
			//	存储一级分类下的二级分类数据集合；
			category1.put("categoryChild",category2ChildList);
			//	将一级分类对象保存到集合
			baseCategory1List.add(category1);
		}
		//	返回数据
		return baseCategory1List;
	}
}
